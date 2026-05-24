package com.mirea.kt.ribo.kazintsev;

import android.util.Log;

import java.io.StringReader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserFactory;

public class NetworkUtils {

    private static final String TAG = "NetworkUtils";

    public static final String CATEGORY_POLITICS = "Политика";
    public static final String CATEGORY_SOCIETY  = "Общество";
    public static final String CATEGORY_SPORT    = "Спорт";

    private static final String URL_NEWS  = "https://aif.ru/rss/news.php";
    private static final String URL_SPORT = "https://aif.ru/rss/google_sport";
    private static final String URL_ALL   = "https://aif.ru/rss/googlearticles";

    private static final String USER_AGENT =
            "Mozilla/5.0 (Linux; Android 12; Pixel 6) "
                    + "AppleWebKit/537.36 (KHTML, like Gecko) "
                    + "Chrome/120.0.0.0 Mobile Safari/537.36";

    private static final OkHttpClient httpClient = new OkHttpClient.Builder()
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .followRedirects(true)
            .followSslRedirects(true)
            .build();

    public static List<NewsItem> fetchNews(String category) {
        String url = resolveUrlByCategory(category);
        Log.d(TAG, "fetchNews category='" + category + "' url=" + url);

        List<NewsItem> items = fetchRssFromUrl(url);

        if (items.isEmpty() && !url.equals(URL_ALL)) {
            Log.w(TAG, "Категорийный URL пуст, пробуем общий фид");
            items = fetchRssFromUrl(URL_ALL);
        }

        Log.d(TAG, "fetchNews итого: " + items.size());
        return items;
    }

    private static String resolveUrlByCategory(String category) {
        if (category == null) return URL_ALL;
        if (CATEGORY_POLITICS.equals(category)) return URL_NEWS;
        if (CATEGORY_SPORT.equals(category))    return URL_SPORT;
        if (CATEGORY_SOCIETY.equals(category))  return URL_ALL;
        return URL_ALL;
    }

    public static String fetchArticleContent(String articleUrl) {
        if (articleUrl == null || articleUrl.isEmpty()) return "";
        try {
            Request request = new Request.Builder()
                    .url(articleUrl)
                    .header("User-Agent", USER_AGENT)
                    .header("Accept", "text/html,*/*")
                    .header("Accept-Language", "ru-RU,ru;q=0.9")
                    .build();

            Response response = httpClient.newCall(request).execute();
            if (!response.isSuccessful()) {
                Log.e(TAG, "fetchArticleContent HTTP " + response.code());
                return "";
            }
            String html = readBodyAsString(response);
            return extractArticleText(html);
        } catch (Exception e) {
            Log.e(TAG, "fetchArticleContent: " + e.getMessage(), e);
            return "";
        }
    }

    private static List<NewsItem> fetchRssFromUrl(String url) {
        List<NewsItem> result = new ArrayList<>();
        try {
            Request request = new Request.Builder()
                    .url(url)
                    .header("User-Agent", USER_AGENT)
                    .header("Accept", "application/rss+xml,application/xml,text/xml,*/*")
                    .header("Accept-Language", "ru-RU,ru;q=0.9")
                    .build();

            Response response = httpClient.newCall(request).execute();
            Log.d(TAG, "HTTP " + response.code() + " <- " + url);

            if (!response.isSuccessful()) {
                Log.e(TAG, "Неуспешный ответ: " + response.code());
                return result;
            }

            String xml = readBodyAsString(response);
            Log.d(TAG, "Получено " + xml.length() + " символов");
            if (xml.length() < 50) return result;

            Log.d(TAG, "Начало XML: " + xml.substring(0, Math.min(300, xml.length())));
            result = parseRssXml(xml);
            Log.d(TAG, "Распарсено: " + result.size() + " новостей");

        } catch (Exception e) {
            Log.e(TAG, "fetchRssFromUrl error: " + e.getMessage(), e);
        }
        return result;
    }

    private static String readBodyAsString(Response response) throws Exception {
        ResponseBody body = response.body();
        if (body == null) return "";

        byte[] bytes = body.bytes();
        String contentType = response.header("Content-Type", "");
        Charset charset = detectCharset(contentType, bytes);

        Log.d(TAG, "Кодировка: " + charset.name());
        return new String(bytes, charset);
    }

    private static Charset detectCharset(String contentType, byte[] bytes) {
        if (contentType != null && contentType.toLowerCase().contains("charset=")) {
            try {
                String cs = contentType.toLowerCase()
                        .replaceAll(".*charset=([^;\\s]+).*", "$1").trim();
                return Charset.forName(cs);
            } catch (Exception ignored) {}
        }

        String preview = new String(bytes, 0, Math.min(bytes.length, 500),
                StandardCharsets.ISO_8859_1);
        Pattern pattern = Pattern.compile("encoding=[\"']([^\"']+)[\"']", Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(preview);
        if (matcher.find()) {
            try {
                return Charset.forName(matcher.group(1));
            } catch (Exception ignored) {}
        }

        return StandardCharsets.UTF_8;
    }

    private static List<NewsItem> parseRssXml(String xml) {
        List<NewsItem> items = new ArrayList<>();
        try {
            XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
            factory.setNamespaceAware(false);
            XmlPullParser parser = factory.newPullParser();
            parser.setInput(new StringReader(xml));

            boolean inItem = false;
            String currentTag = null;
            StringBuilder buffer = new StringBuilder();
            String title = null, description = null, link = null,
                    pubDate = null, category = null;

            int event = parser.getEventType();
            while (event != XmlPullParser.END_DOCUMENT) {
                switch (event) {
                    case XmlPullParser.START_TAG:
                        currentTag = parser.getName();
                        buffer.setLength(0);
                        if ("item".equalsIgnoreCase(currentTag)) {
                            inItem = true;
                            title = description = link = pubDate = category = null;
                        }
                        break;

                    case XmlPullParser.TEXT:
                    case XmlPullParser.CDSECT:
                        if (inItem) buffer.append(parser.getText());
                        break;

                    case XmlPullParser.END_TAG:
                        if (inItem && currentTag != null) {
                            String text = buffer.toString().trim();
                            switch (currentTag.toLowerCase()) {
                                case "title":
                                    if (title == null && !text.isEmpty()) title = text;
                                    break;
                                case "description":
                                    if (description == null && !text.isEmpty())
                                        description = stripHtmlTags(text);
                                    break;
                                case "link":
                                    if (link == null && !text.isEmpty()) link = text;
                                    break;
                                case "pubdate":
                                    if (pubDate == null && !text.isEmpty()) pubDate = text;
                                    break;
                                case "category":
                                    if (category == null && !text.isEmpty()) category = text;
                                    break;
                            }
                        }
                        if ("item".equalsIgnoreCase(parser.getName()) && inItem) {
                            if (title != null && !title.isEmpty()) {
                                NewsItem item = new NewsItem(
                                        title,
                                        description != null ? description : "",
                                        link        != null ? link        : "",
                                        pubDate     != null ? pubDate     : "");
                                item.setCategory(category != null ? category : "");
                                items.add(item);
                            }
                            inItem = false;
                        }
                        currentTag = null;
                        buffer.setLength(0);
                        break;
                }
                event = parser.next();
            }
        } catch (Exception e) {
            Log.e(TAG, "parseRssXml error: " + e.getMessage(), e);
        }
        return items;
    }

    private static String extractArticleText(String html) {
        if (html == null || html.isEmpty()) return "";
        html = removeJunkHtmlBlocks(html);
        String[] contentMarkers = {
                "class=\"article__body\"",
                "class=\"article__text\"",
                "class=\"article-text\"",
                "class=\"article__content\"",
                "class=\"topic-body__content\"",
                "class=\"news-text\"",
                "itemprop=\"articleBody\""
        };
        for (String marker : contentMarkers) {
            String block = extractHtmlBlock(html, marker);
            if (block.length() > 150) {
                String text = stripHtmlTags(block);
                if (text.length() > 100) return cleanArticleText(text);
            }
        }
        int bodyStart = html.indexOf("<body");
        if (bodyStart == -1) bodyStart = 0;
        return cleanArticleText(stripHtmlTags(html.substring(bodyStart)));
    }

    private static String extractHtmlBlock(String html, String marker) {
        try {
            int markerPos = html.indexOf(marker);
            if (markerPos == -1) return "";
            int tagStart = html.lastIndexOf("<", markerPos);
            if (tagStart == -1) return "";
            int nameEnd = html.indexOf(" ", tagStart + 1);
            int nameEnd2 = html.indexOf(">", tagStart + 1);
            if (nameEnd == -1 || nameEnd2 < nameEnd) nameEnd = nameEnd2;
            if (nameEnd == -1) return "";
            String tagName = html.substring(tagStart + 1, nameEnd).trim();
            int openEnd = html.indexOf(">", markerPos);
            if (openEnd == -1) return "";
            int depth = 1, pos = openEnd + 1;
            String closeTag = "</" + tagName.toLowerCase();
            while (depth > 0 && pos < html.length()) {
                int nextOpen  = html.toLowerCase().indexOf("<" + tagName.toLowerCase(), pos);
                int nextClose = html.toLowerCase().indexOf(closeTag, pos);
                if (nextClose == -1) break;
                if (nextOpen != -1 && nextOpen < nextClose) {
                    depth++;
                    pos = nextOpen + 1;
                } else {
                    depth--;
                    if (depth == 0) return html.substring(openEnd + 1, nextClose);
                    pos = nextClose + 1;
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "extractHtmlBlock: " + e.getMessage());
        }
        return "";
    }

    private static String removeJunkHtmlBlocks(String html) {
        return html
                .replaceAll("(?is)<nav[^>]*>.*?</nav>", "")
                .replaceAll("(?is)<header[^>]*>.*?</header>", "")
                .replaceAll("(?is)<footer[^>]*>.*?</footer>", "")
                .replaceAll("(?is)<aside[^>]*>.*?</aside>", "")
                .replaceAll("(?is)<script[^>]*>.*?</script>", "")
                .replaceAll("(?is)<style[^>]*>.*?</style>", "")
                .replaceAll("(?is)<noscript[^>]*>.*?</noscript>", "");
    }

    private static String stripHtmlTags(String html) {
        if (html == null) return "";
        return html
                .replaceAll("(?is)<script[^>]*>.*?</script>", " ")
                .replaceAll("(?is)<style[^>]*>.*?</style>", " ")
                .replaceAll("<br\\s*/?>", "\n")
                .replaceAll("</p>", "\n\n")
                .replaceAll("</li>", "\n")
                .replaceAll("<[^>]+>", "")
                .replaceAll("&nbsp;", " ")
                .replaceAll("&amp;", "&")
                .replaceAll("&lt;", "<")
                .replaceAll("&gt;", ">")
                .replaceAll("&quot;", "\"")
                .replaceAll("&#\\d+;", "")
                .replaceAll("[ \\t]{2,}", " ")
                .replaceAll("\\n{3,}", "\n\n")
                .trim();
    }

    private static String cleanArticleText(String text) {
        if (text == null || text.isEmpty()) return "";
        String[] endMarkers = {
                "Что думаете?", "Читайте также", "ЧИТАЙТЕ ТАКЖЕ",
                "На сайте используются cookie", "Подписывайтесь", "erid:"
        };
        for (String marker : endMarkers) {
            int idx = text.indexOf(marker);
            if (idx > 100) text = text.substring(0, idx).trim();
        }
        if (text.length() > 5000) text = text.substring(0, 5000) + "...";
        return text.trim();
    }
}