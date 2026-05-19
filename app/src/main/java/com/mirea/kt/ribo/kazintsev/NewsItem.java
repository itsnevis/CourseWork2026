package com.mirea.kt.ribo.kazintsev;

public class NewsItem {

    private final String title;
    private final String description;
    private final String link;
    private final String pubDate;
    private String category;

    public NewsItem(String title, String description, String link, String pubDate) {
        this.title = title;
        this.description = description;
        this.link = link;
        this.pubDate = pubDate;
        this.category = "";
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public String getLink() {
        return link;
    }

    public String getPubDate() {
        return pubDate;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }
}