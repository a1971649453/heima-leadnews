package com.heima.article.service;

import com.heima.model.mess.app.AggBehaviorDTO;

/**
 * <p>
 * 热文章表 服务类
 * </p>
 *
 * @author itheima
 */
public interface HotArticleService{
    /**
     * 计算热文章
     */
    public void computeHotArticle();

    /**
     * 更新文章热度
     * @param aggBehavior
     */
    public void updateApArticle(AggBehaviorDTO aggBehavior);
}