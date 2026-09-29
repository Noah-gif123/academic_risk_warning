package dhui.hellomybatis.service.impl;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import dhui.hellomybatis.entity.Article;
import dhui.hellomybatis.mapper.ArticleMapper;
import dhui.hellomybatis.service.ArticleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ArticleServiceImpl implements ArticleService {
    @Autowired
    private ArticleMapper articleMapper;

    @Override
    public PageInfo<Article> getArticleListByPage(int pageNum, int pageSize) {
        PageHelper.startPage(pageNum, pageSize);
        List<Article> articleList = articleMapper.getArticleList();
        PageInfo<Article> pageInfo = new PageInfo<>(articleList);
        return pageInfo;
    }

    // 补充实现接口中的addArticle方法
    @Override
    public int addArticle(Article article) {
        return articleMapper.addArticle(article);
    }
}