package com.lcchu.shushu;

public class NovelInfo {
    private String name;
    private String title;
    private String coverURL;
    private String author;
    private String desc;
    private boolean expanded;
    private boolean rank;

    public NovelInfo(){
        expanded = false;
        rank = false;
    }

    public boolean isExpanded() {
        return expanded;
    }

    public boolean isRank() {
        return rank;
    }

    public void setExpanded(boolean expanded) {
        this.expanded = expanded;
    }

    public void setRank(boolean rank) {
        this.rank = rank;
    }

    public String getName() {
        return name;
    }

    public String getAuthor() {
        return author;
    }

    public String getCoverURL() {
        return coverURL;
    }

    public String getDesc() {
        return desc;
    }

    public String getTitle() {
        return title;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public void setCoverURL(String coverURL) {
        this.coverURL = coverURL;
    }

    public void setDesc(String desc) {
        this.desc = desc;
    }

    public void setTitle(String title) {
        this.title = title;
    }
}
