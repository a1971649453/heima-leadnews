package com.heima.behavior.service.impl;
import java.util.Date;

import com.alibaba.fastjson.JSON;
import com.heima.behavior.service.ApBehaviorEntryService;
import com.heima.behavior.service.ApCollectionBehaviorService;
import com.heima.common.exception.CustException;
import com.heima.model.behavior.dtos.CollectionBehaviorDTO;
import com.heima.model.behavior.pojos.ApBehaviorEntry;
import com.heima.model.behavior.pojos.ApCollection;
import com.heima.model.common.constants.article.HotArticleConstants;
import com.heima.model.common.dtos.ResponseResult;
import com.heima.model.common.enums.AppHttpCodeEnum;
import com.heima.model.mess.app.NewBehaviorDTO;
import com.heima.model.threadlocal.AppThreadLocalUtils;
import com.heima.model.user.pojos.ApUser;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

/**
 * @author 金宗文
 * @version 1.0
 */
@Service
@Slf4j
public class ApCollectionBehaviorServiceImpl implements ApCollectionBehaviorService {
    @Resource
    private MongoTemplate mongoTemplate;
    @Resource
    private ApBehaviorEntryService apBehaviorEntryService;
    @Resource
    private RabbitTemplate rabbitTemplate;
    @Override
    public ResponseResult collectBehavior(CollectionBehaviorDTO dto) {
        //参数检验 文章ID不能为null 注解检验

        // 用户需要登录
        ApUser user = AppThreadLocalUtils.getUser();
        if (user == null){
            CustException.cust(AppHttpCodeEnum.NEED_LOGIN);
        }
        ApBehaviorEntry apBehaviorEntry = apBehaviorEntryService.findByUserIdOrEquipmentId(user.getId(), null);
        String entryId = apBehaviorEntry.getId();
        // 0 收藏 1取消收藏
        Short operation = dto.getOperation();
        ApCollection apCollection = mongoTemplate.findOne(Query.query(Criteria.where("entryId").is(entryId).and("articleId").is(dto.getArticleId())), ApCollection.class);
        if (apCollection == null){
            if (operation.equals(ApCollection.Type.ARTICLE.getCode())){
                //收藏
                ApCollection apCollection1 = new ApCollection();
                apCollection1.setEntryId(entryId);
                apCollection1.setArticleId(dto.getArticleId());
                apCollection1.setType(dto.getType());
                apCollection1.setCollectionTime(new Date());
                mongoTemplate.save(apCollection1);

                // 发送 新行为消息 直接使用简单模式
                NewBehaviorDTO newBehaviorDTO = new NewBehaviorDTO();
                newBehaviorDTO.setType(NewBehaviorDTO.BehaviorType.VIEWS);
                newBehaviorDTO.setAdd(dto.getOperation().intValue() == 1 ? 1 : -1);
                newBehaviorDTO.setArticleId(dto.getArticleId());
                rabbitTemplate.convertAndSend(HotArticleConstants.HOT_ARTICLE_SCORE_BEHAVIOR_QUEUE, JSON.toJSONString(newBehaviorDTO));
                log.info("成功发送 文章收藏行为信息, 消息内容:{}",newBehaviorDTO);

                return ResponseResult.okResult();
            }else {
                return ResponseResult.errorResult(AppHttpCodeEnum.DATA_NOT_ALLOW,"您还未收藏此作品");
            }
        }else {
            if (operation.equals(ApCollection.Type.DYNAMIC.getCode())){
                mongoTemplate.remove(Query.query(Criteria.where("entryId").is(entryId).and("articleId").is(dto.getArticleId())), ApCollection.class);
            }else {
                return ResponseResult.errorResult(AppHttpCodeEnum.DATA_NOT_ALLOW,"请勿重复收藏");
            }

        }



        return ResponseResult.okResult();
    }
}
