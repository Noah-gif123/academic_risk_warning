package dhui.hellomybatis.entity;

public class Comment {
    private Integer cid;
    private String content;
    private String author;
    private Integer aid;



    public Integer getCid() {
        return cid;
    }

    public void setCid(Integer cid) {
        this.cid = cid;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public Integer getAid() {
        return aid;
    }
    public void setAid(Integer aid) {
        this.aid = aid;
    }

    @Override
    public String toString() {
        return "Comment{" +
                "cid=" + cid +
                ", content='" + content + '\'' +
                ", author='" + author + '\'' +
                ", aid=" + aid +
                '}';
    }
}
