package com.mirea.kt.ribo.kazintsev;

import android.util.Log;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserFactory;

public class NetworkUtils {

    private static final String TAG = "NetworkUtils";

    private static final String URL_POLITICS = "https://lenta.ru/rss/news/politics/";
    private static final String URL_SOCIETY  = "https://lenta.ru/rss/news/russia/";
    private static final String URL_SPORT    = "https://lenta.ru/rss/news/sport/";
    private static final String URL_ALL      = "https://lenta.ru/rss/";

    private static final OkHttpClient client = new OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .followRedirects(true)
            .followSslRedirects(true)
            .build();

    // -----------------------------------------------------------------------
    // Загрузка списка новостей по категории
    // -----------------------------------------------------------------------
    public static List<NewsItem> fetchNews(String category) {
        String url;
        if (category == null || category.equalsIgnoreCase("all")
                || category.equalsIgnoreCase("Все")) {
            url = URL_ALL;
        } else if (category.equalsIgnoreCase("Политика")) {
            url = URL_POLITICS;
        } else if (category.equalsIgnoreCase("Общество")) {
            url = URL_SOCIETY;
        } else if (category.equalsIgnoreCase("Спорт")) {
            url = URL_SPORT;
        } else {
            url = URL_ALL;
        }

        Log.d(TAG, "Загружаем категорию '" + category + "' с: " + url);
        List<NewsItem> items = fetchRssUrl(url);

        if (items.isEmpty()) {
            Log.w(TAG, "Категорийный URL пуст, пробуем общий фид...");
            items = fetchRssUrl(URL_ALL);
        }

        Log.d(TAG, "Итого новостей: " + items.size());
        return items;
    }

    // -----------------------------------------------------------------------
    // Загрузка полного текста статьи по URL
    // -----------------------------------------------------------------------
    public static String fetchArticleContent(String articleUrl) {
        if (articleUrl == null || articleUrl.isEmpty()) return "";
        try {
            Request request = new Request.Builder()
                    .url(articleUrl)
                    .header("User-Agent",
                            "Mozilla/5.0 (Linux; Android 12; Pixel 6) "
                                    + "AppleWebKit/537.36 (KHTML, like Gecko) "
                                    + "Chrome/120.0.0.0 Mobile Safari/537.36")
                    .header("Accept", "text/html,*/*")
                    .header("Accept-Language", "ru-RU,ru;q=0.9")
                    .build();

            Response response = client.newCall(request).execute();
            if (!response.isSuccessful()) {
                Log.e(TAG, "fetchArticleContent HTTP: " + response.code());
                return "";
            }

            String html = response.body().string();
            return extractArticleText(html);

        } catch (Exception e) {
            Log.e(TAG, "fetchArticleContent error: " + e.getMessage(), e);
            return "";
        }
    }

    private static String extractArticleText(String html) {
        if (html == null || html.isEmpty()) return "";

        // ШАГ 1: Удаляем мусорные блоки до поиска контента
        html = removeJunkBlocks(html);

        // ШАГ 2: Ищем основной блок по CSS-классам lenta.ru
        String[] contentMarkers = {
                "class=\"topic-body__content\"",
                "class=\"topic-body\"",
                "class=\"article__text\"",
                "class=\"article-body\"",
                "class=\"article__content\"",
                "class=\"news-text\"",
                "class=\"b-text\"",
                "class=\"entry-content\"",
                "itemprop=\"articleBody\""
        };

        for (String marker : contentMarkers) {
            String extracted = extractBetweenTags(html, marker);
            if (extracted.length() > 150) {
                String result = stripHtmlFully(extracted);
                if (result.length() > 100) {
                    Log.d(TAG, "Найден контент по маркеру: " + marker);
                    return cleanupText(result);
                }
            }
        }

        // ШАГ 3: Фоллбэк — берём body целиком
        Log.w(TAG, "Маркеры не найдены, используем фоллбэк");
        int bodyStart = html.indexOf("<body");
        if (bodyStart == -1) bodyStart = 0;
        String body = html.substring(bodyStart);
        String plain = stripHtmlFully(body);
        return cleanupText(plain);
    }

    /**
     * Очищает итоговый текст от мусора в начале и конце:
     * - хлебные крошки, erid-коды, метаданные — в начале
     * - "Что думаете?", cookie-уведомления, реклама — в конце
     */
    private static String cleanupText(String text) {
        if (text == null || text.isEmpty()) return "";

        // --- Обрезаем конец: убираем мусор после маркеров ---
        String[] endMarkers = {
                "Что думаете?",
                "На сайте используются cookie",
                "Продолжая использовать сайт",
                "Подписывайтесь на нас",
                "Читайте также",
                "ЧИТАЙТЕ ТАКЖЕ",
                "Смотрите также",
                "erid:",
                "Реклама. "
        };
        for (String marker : endMarkers) {
            int idx = text.indexOf(marker);
            if (idx > 100) { // оставляем хотя бы 100 символов
                text = text.substring(0, idx).trim();
            }
        }

        // --- Обрезаем начало: пропускаем короткие "параграфы" (метаданные) ---
        // Разбиваем на абзацы и ищем первый длинный (>=80 символов)
        String[] paragraphs = text.split("\n\n");
        int startIndex = 0;
        for (int i = 0; i < paragraphs.length; i++) {
            String p = paragraphs[i].trim();
            if (p.length() >= 80) {
                startIndex = i;
                break;
            }
        }

        // Собираем текст начиная с первого длинного абзаца
        if (startIndex > 0) {
            StringBuilder sb = new StringBuilder();
            for (int i = startIndex; i < paragraphs.length; i++) {
                if (sb.length() > 0) sb.append("\n\n");
                sb.append(paragraphs[i].trim());
            }
            text = sb.toString();
        }

        // Ограничиваем длину
        if (text.length() > 5000) text = text.substring(0, 5000) + "...";
        return text.trim();
    }

    /**
     * Удаляет блоки, которые точно не содержат текст статьи.
     */
    private static String removeJunkBlocks(String html) {
        return html
                .replaceAll("(?is)<nav[^>]*>.*?</nav>", "")
                .replaceAll("(?is)<header[^>]*>.*?</header>", "")
                .replaceAll("(?is)<footer[^>]*>.*?</footer>", "")
                .replaceAll("(?is)<aside[^>]*>.*?</aside>", "")
                .replaceAll("(?is)<script[^>]*>.*?</script>", "")
                .replaceAll("(?is)<style[^>]*>.*?</style>", "")
                .replaceAll("(?is)<noscript[^>]*>.*?</noscript>", "")
                .replaceAll("(?is)<[^>]*(class|id)=\"[^\"]*\\bad\\b[^\"]*\"[^>]*>.*?</[a-z]+>", "")
                .replaceAll("(?is)<[^>]*(class|id)=\"[^\"]*banner[^\"]*\"[^>]*>.*?</[a-z]+>", "")
                .replaceAll("(?is)<[^>]*(class|id)=\"[^\"]*social[^\"]*\"[^>]*>.*?</[a-z]+>", "")
                .replaceAll("(?is)<[^>]*(class|id)=\"[^\"]*related[^\"]*\"[^>]*>.*?</[a-z]+>", "")
                .replaceAll("(?is)<[^>]*(class|id)=\"[^\"]*recommend[^\"]*\"[^>]*>.*?</[a-z]+>", "")
                .replaceAll("(?is)<[^>]*(class|id)=\"[^\"]*cookie[^\"]*\"[^>]*>.*?</[a-z]+>", "");
    }

    private static String extractBetweenTags(String html, String marker) {
        try {
            int markerPos = html.indexOf(marker);
            if (markerPos == -1) return "";

            int tagStart = html.lastIndexOf("<", markerPos);
            if (tagStart == -1) return "";

            int nameEnd  = html.indexOf(" ", tagStart + 1);
            int nameEnd2 = html.indexOf(">", tagStart + 1);
            if (nameEnd == -1 || nameEnd2 < nameEnd) nameEnd = nameEnd2;
            if (nameEnd == -1) return "";
            String tagName = html.substring(tagStart + 1, nameEnd).trim();

            int openEnd = html.indexOf(">", markerPos);
            if (openEnd == -1) return "";

            String closeTag = "</" + tagName;
            int depth = 1;
            int pos = openEnd + 1;
            while (depth > 0 && pos < html.length()) {
                int nextOpen  = html.toLowerCase().indexOf("<" + tagName.toLowerCase(), pos);
                int nextClose = html.toLowerCase().indexOf(closeTag.toLowerCase(), pos);
                if (nextClose == -1) break;
                if (nextOpen != -1 && nextOpen < nextClose) {
                    depth++;
                    pos = nextOpen + 1;
                } else {
                    depth--;
                    if (depth == 0) {
                        return html.substring(openEnd + 1, nextClose);
                    }
                    pos = nextClose + 1;
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "extractBetweenTags: " + e.getMessage());
        }
        return "";
    }

    private static String stripHtmlFully(String html) {
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

    // -----------------------------------------------------------------------
    // Загрузка и парсинг RSS
    // -----------------------------------------------------------------------
    private static List<NewsItem> fetchRssUrl(String url) {
        List<NewsItem> result = new ArrayList<>();
        try {
            Request request = new Request.Builder()
                    .url(url)
                    .header("User-Agent",
                            "Mozilla/5.0 (Linux; Android 12; Pixel 6) "
                                    + "AppleWebKit/537.36 (KHTML, like Gecko) "
                                    + "Chrome/120.0.0.0 Mobile Safari/537.36")
                    .header("Accept", "application/rss+xml,application/xml,text/xml,*/*")
                    .header("Accept-Language", "ru-RU,ru;q=0.9")
                    .build();

            Response response = client.newCall(request).execute();
            Log.d(TAG, "HTTP " + response.code() + " от " + url);
            if (!response.isSuccessful()) return result;

            String body = response.body().string();
            Log.d(TAG, "Получено символов: " + body.length());
            if (body.length() < 50) return result;

            result = parseRss(body);
            Log.d(TAG, "Распарсено новостей: " + result.size());

        } catch (Exception e) {
            Log.e(TAG, "fetchRssUrl error: " + e.getMessage(), e);
        }
        return result;
    }

    private static List<NewsItem> parseRss(String xml) {
        List<NewsItem> items = new ArrayList<>();
        try {
            XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
            factory.setNamespaceAware(false);
            XmlPullParser parser = factory.newPullParser();
            parser.setInput(new StringReader(xml));

            boolean inItem   = false;
            String currentTag = null;
            StringBuilder buf = new StringBuilder();

            String title = null, description = null, link = null,
                    pubDate = null, category = null;

            int event = parser.getEventType();
            while (event != XmlPullParser.END_DOCUMENT) {
                switch (event) {
                    case XmlPullParser.START_TAG:
                        currentTag = parser.getName();
                        buf.setLength(0);
                        if ("item".equalsIgnoreCase(currentTag)) {
                            inItem = true;
                            title = description = link = pubDate = category = null;
                        }
                        break;

                    case XmlPullParser.TEXT:
                    case XmlPullParser.CDSECT:
                        if (inItem) buf.append(parser.getText());
                        break;

                    case XmlPullParser.END_TAG:
                        if (inItem && currentTag != null) {
                            String text = buf.toString().trim();
                            switch (currentTag.toLowerCase()) {
                                case "title":
                                    if (title == null && !text.isEmpty()) title = text;
                                    break;
                                case "description":
                                    if (description == null && !text.isEmpty())
                                        description = stripHtmlFully(text);
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
                        buf.setLength(0);
                        break;
                }
                event = parser.next();
            }
        } catch (Exception e) {
            Log.e(TAG, "parseRss error: " + e.getMessage(), e);
        }
        return items;
    }
}