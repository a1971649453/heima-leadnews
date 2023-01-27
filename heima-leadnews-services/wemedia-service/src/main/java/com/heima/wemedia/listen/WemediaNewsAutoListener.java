package com.heima.wemedia.listen;

import com.heima.model.common.constants.message.NewsAutoScanConstants;
import com.heima.wemedia.service.WmNewsAutoScanService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

/**
 * @author 金宗文
 * @version 1.0
 */
@Component
@Slf4j
public class WemediaNewsAutoListener {
    @Resource
    WmNewsAutoScanService wmNewsAutoScanService;

    /**
     * queues 监听队列,不会自动创建
     *
     * queuesToDeclare 不存在会自动创建队列
     *
     * bindings:监听队列 可以创建交换机 队列 以及绑定关系
     */
    @RabbitListener(queuesToDeclare = @Queue(NewsAutoScanConstants.WM_NEWS_AUTO_SCAN_QUEUE))
    public void handleAutoScanMsg(String newsId){
        log.info("接收到自动审核消息:{}",newsId);
        wmNewsAutoScanService.autoScanWmNews(Integer.valueOf(newsId));
        log.info("自动审核完成:{}",newsId);
    }
}
