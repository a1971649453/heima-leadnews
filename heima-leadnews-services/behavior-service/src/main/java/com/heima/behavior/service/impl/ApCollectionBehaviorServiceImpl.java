package com.heima.behavior.service.impl;
import java.util.Date;

import com.heima.behavior.service.ApBehaviorEntryService;
import com.heima.behavior.service.ApCollectionBehaviorService;
import com.heima.common.exception.CustException;
import com.heima.model.behavior.dtos.CollectionBehaviorDTO;
import com.heima.model.behavior.pojos.ApBehaviorEntry;
import com.heima.model.behavior.pojos.ApCollection;
import com.heima.model.common.dtos.ResponseResult;
import com.heima.model.common.enums.AppHttpCodeEnum;
import com.heima.model.threadlocal.AppThreadLocalUtils;
import com.heima.model.user.pojos.ApUser;
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
public class ApCollectionBehaviorServiceImpl implements ApCollectionBehaviorService {
    @Resource
    private MongoTemplate mongoTemplate;
    @Resource
    private ApBehaviorEntryService apBehaviorEntryService;
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
