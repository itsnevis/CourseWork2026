package com.mirea.kt.ribo.kazintsev;

public class NewsItem {
    private String title;
    private String description;
    private String link;
    private String pubDate;

    public NewsItem(String title, String description, String link, String pubDate) {
        this.title = title;
        this.description = description;
        this.link = link;
        this.pubDate = pubDate;
    }

    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public String getLink() { return link; }
    public String getPubDate() { return pubDate; }
}