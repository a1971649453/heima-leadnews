package com.heima.article.listen;

import com.heima.model.common.constants.article.HotArticleConstants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.data.redis.core.ListOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

/**
 * @author 金宗文
 * @version 1.0
 */
@Component
@Slf4j
public class HotArticleScoreListener {
    @Resource
    private StringRedisTemplate stringRedisTemplate;
    @RabbitListener(queuesToDeclare = @Queue(HotArticleConstants.HOT_ARTICLE_SCORE_BEHAVIOR_QUEUE))
    public void listenNewBehaviorHandler(String newBehavior){
        log.info("接收到 新产生的文章行为数据:{}",newBehavior);

        try {
            // 将新产生的文章行为数据存入redis的list中
            ListOperations<String, String> opsForList = stringRedisTemplate.opsForList();
            opsForList.rightPush(HotArticleConstants.HOT_ARTICLE_SCORE_BEHAVIOR_LIST,newBehavior);
        } catch (Exception e) {
            e.printStackTrace();
            log.error("将新行为信息 传入redis 失败 :{}",e.getMessage());
        }


    }

}
