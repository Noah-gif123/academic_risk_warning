package dhui.hellomybatis.service;

import com.github.pagehelper.PageInfo;
import dhui.hellomybatis.entity.Article;

public interface ArticleService {
    PageInfo<Article> getArticleListByPage(int pageNum, int pageSize);

    int addArticle(Article article);
}
