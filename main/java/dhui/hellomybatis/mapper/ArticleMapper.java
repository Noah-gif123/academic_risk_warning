package dhui.hellomybatis.mapper;

import dhui.hellomybatis.entity.Article;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface ArticleMapper {
    List<Article> getArticleList();
    public Article selectArticle(int aid);
    public int addArticle(Article article);
    public int addPK(Article article);
    public int insertPKBack(Article article);
    public List<Article> getSql();
    public Article getArticleWithComment(int aid);
    public List<Article> getArticles1(Article article);
    public List<Article> getArticles2(Article article);
    public List<Article> getArticles3(Article article);
}
