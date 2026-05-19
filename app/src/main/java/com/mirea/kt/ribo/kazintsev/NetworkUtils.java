package com.mirea.kt.ribo.kazintsev;

import android.util.Log;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserFactory;

public class NetworkUtils {

    private static final String TAG = "NetworkUtils";
    private static final String RSS_URL = "https://aif.ru/rsspage";

    private static final OkHttpClient client = new OkHttpClient.Builder()
            .followRedirects(true)
            .followSslRedirects(true)
            .build();

    public static List<NewsItem> fetchNews(String category) {
        List<NewsItem> result = new ArrayList<>();
        try {
            Log.d(TAG, "Запрос к: " + RSS_URL + " | категория: " + category);

            Request request = new Request.Builder()
                    .url(RSS_URL)
                    .header("User-Agent", "Mozilla/5.0 (Linux; Android 10) AppleWebKit/537.36")
                    .header("Accept", "application/rss+xml, application/xml, text/xml, */*")
                    .build();

            Response response = client.newCall(request).execute();

            if (!response.isSuccessful()) {
                Log.e(TAG, "HTTP ошибка: " + response.code());
                return result;
            }

            String body = response.body().string();
            Log.d(TAG, "Ответ получен, длина: " + body.length() + " символов");

            if (body.length() < 100) {
                Log.e(TAG, "Ответ слишком короткий: " + body);
                return result;
            }

            List<NewsItem> allItems = parseRss(body);
            Log.d(TAG, "Всего распарсено: " + allItems.size() + " новостей");

            if (allItems.isEmpty()) {
                Log.w(TAG, "Первые 500 символов ответа: "
                        + body.substring(0, Math.min(500, body.length())));
                return result;
            }

            logCategories(allItems);

            if (category == null || category.equals("all") || category.equals("Все")) {
                result.addAll(allItems);
            } else {
                for (NewsItem item : allItems) {
                    if (matchesCategory(item.getCategory(), category)) {
                        result.add(item);
                    }
                }
                Log.d(TAG, "После фильтра '" + category + "': " + result.size() + " новостей");

                if (result.isEmpty()) {
                    Log.w(TAG, "Фильтр '" + category + "' ничего не нашёл — возвращаем все");
                    result.addAll(allItems);
                }
            }

        } catch (Exception e) {
            Log.e(TAG, "Исключение в fetchNews: " + e.getMessage(), e);
        }
        return result;
    }

    private static void logCategories(List<NewsItem> items) {
        Set<String> cats = new HashSet<>();
        for (NewsItem item : items) {
            cats.add(item.getCategory());
        }
        Log.d(TAG, "Категории в RSS: " + cats.toString());
    }

    private static boolean matchesCategory(String itemCategory, String filterCategory) {
        if (itemCategory == null || itemCategory.isEmpty()) return false;
        if (filterCategory == null || filterCategory.isEmpty()) return true;
        String ic = itemCategory.toLowerCase().trim();
        String fc = filterCategory.toLowerCase().trim();
        return ic.contains(fc) || fc.contains(ic);
    }

    private static List<NewsItem> parseRss(String xml) {
        List<NewsItem> items = new ArrayList<>();
        try {
            XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
            factory.setNamespaceAware(false);
            XmlPullParser parser = factory.newPullParser();
            parser.setInput(new StringReader(xml));

            boolean inItem = false;
            String currentTag = null;
            String title = null, description = null, link = null, pubDate = null, category = null;

            int eventType = parser.getEventType();
            while (eventType != XmlPullParser.END_DOCUMENT) {
                switch (eventType) {
                    case XmlPullParser.START_TAG:
                        currentTag = parser.getName();
                        if ("item".equals(currentTag)) {
                            inItem = true;
                            title = null;
                            description = null;
                            link = null;
                            pubDate = null;
                            category = null;
                        }
                        break;

                    case XmlPullParser.TEXT:
                        if (inItem && currentTag != null) {
                            String text = parser.getText();
                            if (text != null) {
                                text = cleanCdata(text.trim());
                                switch (currentTag) {
                                    case "title":
                                        if (title == null) title = text;
                                        break;
                                    case "description":
                                        if (description == null) description = stripHtml(text);
                                        break;
                                    case "link":
                                        if (link == null) link = text;
                                        break;
                                    case "pubDate":
                                        if (pubDate == null) pubDate = text;
                                        break;
                                    case "category":
                                        if (category == null) category = text;
                                        break;
                                }
                            }
                        }
                        break;

                    case XmlPullParser.END_TAG:
                        if ("item".equals(parser.getName()) && inItem) {
                            if (title != null && !title.isEmpty()) {
                                NewsItem item = new NewsItem(
                                        title,
                                        description != null ? description : "",
                                        link != null ? link : "",
                                        pubDate != null ? pubDate : ""
                                );
                                item.setCategory(category != null ? category : "");
                                items.add(item);
                            }
                            inItem = false;
                        }
                        currentTag = null;
                        break;
                }
                eventType = parser.next();
            }
        } catch (Exception e) {
            Log.e(TAG, "Ошибка XmlPullParser: " + e.getMessage(), e);
            return fallbackParse(xml);
        }
        return items;
    }

    private static String cleanCdata(String text) {
        if (text == null) return "";
        return text.replace("<![CDATA[", "").replace("]]>", "").trim();
    }

    private static String stripHtml(String html) {
        if (html == null) return "";
        return html.replaceAll("<[^>]+>", "")
                .replaceAll("&nbsp;", " ")
                .replaceAll("&amp;", "&")
                .replaceAll("&lt;", "<")
                .replaceAll("&gt;", ">")
                .replaceAll("&quot;", "\"")
                .trim();
    }

    private static List<NewsItem> fallbackParse(String xml) {
        List<NewsItem> items = new ArrayList<>();
        Log.w(TAG, "Запасной строковый парсер");
        try {
            String[] rawItems = xml.split("<item>");
            for (int i = 1; i < rawItems.length; i++) {
                String block = rawItems[i];
                String t = extractTag(block, "title");
                String d = stripHtml(extractTag(block, "description"));
                String l = extractTag(block, "link");
                String p = extractTag(block, "pubDate");
                String c = extractTag(block, "category");
                if (!t.isEmpty()) {
                    NewsItem item = new NewsItem(t, d, l, p);
                    item.setCategory(c);
                    items.add(item);
                }
            }
            Log.d(TAG, "Запасной парсер нашёл: " + items.size() + " новостей");
        } catch (Exception e) {
            Log.e(TAG, "Ошибка запасного парсера: " + e.getMessage());
        }
        return items;
    }

    private static String extractTag(String source, String tag) {
        try {
            String open = "<" + tag + ">";
            String close = "</" + tag + ">";
            int start = source.indexOf(open);
            if (start == -1) return "";
            start += open.length();
            int end = source.indexOf(close, start);
            if (end == -1) return "";
            return cleanCdata(source.substring(start, end).trim());
        } catch (Exception e) {
            return "";
        }
    }
}