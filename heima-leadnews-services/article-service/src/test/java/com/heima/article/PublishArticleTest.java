package com.heima.article;

import com.heima.article.service.ApArticleService;
import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import javax.annotation.Resource;

/**
 * @author 金宗文
 * @version 1.0
 */
@SpringBootTest
//@RunWith(SpringRunner.class)
public class PublishArticleTest {
    @Resource
    ApArticleService apArticleService;
    @Test
    public void testPublishArticle(){
        apArticleService.publishArticle(6276);
    }
}
