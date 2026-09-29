package dhui.hellomybatis.entity;

import java.util.List;

public class Article {
    private Integer aid;
    private String title;
    private String content;
    private String titlepic;
    private String video;
    private List<Comment> commentList;

    public Integer getAid() {
        return aid;
    }

    public void setAid(Integer aid) {
        this.aid = aid;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getTitlepic() {
        return titlepic;
    }

    public void setTitlepic(String titlepic) {
        this.titlepic = titlepic;
    }

    public String getVideo() {
        return video;
    }

    public void setVideo(String video) {
        this.video = video;
    }

    public List<Comment> getCommentList() {
        return commentList;
    }

    public void setCommentList(List<Comment> commentList) {
        this.commentList = commentList;
    }

    @Override
    public String toString() {
        return "Article{" +
                "aid=" + aid +
                ", title='" + title + '\'' +
                ", content='" + content + '\'' +
                ", titlepic='" + titlepic + '\'' +
                ", video='" + video + '\'' +
                ", commentList=" + commentList +
                '}';
    }
}
