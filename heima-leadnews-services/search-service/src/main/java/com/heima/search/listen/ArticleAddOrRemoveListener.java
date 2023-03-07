package com.heima.search.listen;

import com.heima.common.exception.CustException;
import com.heima.feigns.ArticleFeign;

import com.heima.model.common.constants.message.NewsUpOrDownConstants;
import com.heima.model.common.dtos.ResponseResult;
import com.heima.model.common.enums.AppHttpCodeEnum;

import com.heima.model.search.vos.SearchArticleVO;
import com.heima.search.service.ArticleSearchService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

/**
 * @author 金宗文
 * @version 1.0
 */
@Component
@Slf4j
public class ArticleAddOrRemoveListener {
    @Resource
    private ArticleSearchService articleSearchService;

    @Resource
    private ArticleFeign articleFeign;


    @RabbitListener(queues = NewsUpOrDownConstants.NEWS_UP_FOR_ES_QUEUE)
    public void listenNewsUpMsg(String articleId){
        log.info(" 接收到文章上架消息,消息内容:{}",articleId);
        // 根据articleId 查询文章信息 将文章信息 添加到ES索引库中
        ResponseResult<SearchArticleVO> result = articleFeign.findArticle(Long.valueOf(articleId));
        if (!result.checkCode()) {
            CustException.cust(AppHttpCodeEnum.DATA_NOT_EXIST,"未查询到对应的文章数据");
            log.error("未查询到对应的文章数据,articleId:{}",articleId);
        }
        // 远程查询Article信息
        articleSearchService.saveArticle(result.getData());
    }

    @RabbitListener(queues = NewsUpOrDownConstants.NEWS_DOWN_FOR_ES_QUEUE)
    public void listenNewsDownMsg(String articleId){
        log.info(" 接收到文章下架消息,消息内容:{}",articleId);
        // 根据articleId 查询文章信息 将文章信息 删除到ES索引库中
        articleSearchService.deleteArticle(articleId);
    }
}
