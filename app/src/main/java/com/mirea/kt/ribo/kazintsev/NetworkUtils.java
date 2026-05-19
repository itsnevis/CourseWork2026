package com.mirea.kt.ribo.kazintsev;

import android.util.Log;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class NetworkUtils {
    public static List<NewsItem> fetchNews(String category) {
        List<NewsItem> items = new ArrayList<>();
        try {
            // Упрощенный URL для проверки
            URL url = new URL("https://aif.ru/rsspage");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestProperty("User-Agent", "Mozilla/5.0");
            conn.setConnectTimeout(10000);

            BufferedReader r = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            StringBuilder total = new StringBuilder();
            String line;
            while ((line = r.readLine()) != null) {
                total.append(line);
            }
            String xml = total.toString();

            String[] rawItems = xml.split("<item>");
            for (int i = 1; i < rawItems.length; i++) {
                String itemContent = rawItems[i];
                String title = extract(itemContent, "<title>", "</title>");
                String desc = extract(itemContent, "<description>", "</description>").replaceAll("<[^>]*>", "");
                String link = extract(itemContent, "<link>", "</link>");
                String date = extract(itemContent, "<pubDate>", "</pubDate>");

                if (!title.isEmpty()) {
                    items.add(new NewsItem(title, desc, link, date));
                }
            }
        } catch (Exception e) {
            Log.e("NW_UTILS", "Error: " + e.getMessage());
        }
        return items;
    }

    private static String extract(String source, String startTag, String endTag) {
        try {
            int start = source.indexOf(startTag) + startTag.length();
            int end = source.indexOf(endTag, start);
            if (start < startTag.length() || end == -1) return "";
            return source.substring(start, end).replace("<![CDATA[", "").replace("]]>", "").trim();
        } catch (Exception e) {
            return "";
        }
    }
}