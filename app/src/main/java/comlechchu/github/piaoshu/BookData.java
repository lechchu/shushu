package comlechchu.github.piaoshu;

public class BookData {
    private String bookName;
    private String chapterID;
    private String chapterListURL;
    private String bookURL;
    private String coverURL;

    BookData(String name,String chapter_id){
        bookName = name;
        chapterID = chapter_id;
        bookURL="https://tw.kjasugn.top/novel/pagea/"+ bookName + "_" + chapterID + ".html";
        coverURL = "https://static.ttkan.co/cover/" + bookName + ".jpg?w=100&h=130&q=100";
        chapterListURL = "https://tw.ttkan.co/api/nq/amp_novel_chapters?language=tw&novel_id=" + bookName;
    }


    public void updateChapter(String ID){
        chapterID = ID;
        bookURL="https://tw.kjasugn.top/novel/pagea/"+ bookName + "_" + chapterID + ".html";
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
