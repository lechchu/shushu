package com.lcchu.shushu;

public class BookData {
    private String bookName;
    private String chapterID;
    private String chapterListURL;
    private String bookURL;
    private String coverURL;

    BookData(String name,String chapter_id){
        bookName = name;
        chapterID = chapter_id;
        bookURL="https://tw.mingzw.net/mzwbook/"+ bookName + "/" + chapterID;
        coverURL = "https://tw.mingzw.net/images/mzwid/" + bookName + ".jpg";
        chapterListURL = "https://tw.mingzw.net/mzwchapter/"+ bookName;
    }

    public void updateChapter(String ID){
        chapterID = ID;
        bookURL="https://tw.mingzw.net/mzwread/"+ bookName + "_" + chapterID;
    }

    public String getChapterID(){
        return chapterID;
    }

    public String getBookURL(){
        return bookURL;
    }

    public String getChapterListURL(){
        return chapterListURL;
    }

    public String getCoverURL(){
        return coverURL;
    }
}
