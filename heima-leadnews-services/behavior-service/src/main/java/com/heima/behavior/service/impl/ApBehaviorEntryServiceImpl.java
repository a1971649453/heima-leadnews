package com.heima.behavior.service.impl;
import java.util.Date;
import java.util.List;

import com.heima.behavior.service.ApBehaviorEntryService;
import com.heima.model.behavior.pojos.ApBehaviorEntry;
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
public class ApBehaviorEntryServiceImpl implements ApBehaviorEntryService {
    @Resource
    MongoTemplate mongoTemplate;
    @Override
    public ApBehaviorEntry findByUserIdOrEquipmentId(Integer userId, Integer equipmentId) {
        //1.判断userID是否为null
        if (userId != null ){
            //1.2不为null 使用mongoTemplate查询
            ApBehaviorEntry apBehaviorEntry1 = mongoTemplate.findOne(Query.query(Criteria.where("refId").is(userId).and("type").is(1)), ApBehaviorEntry.class);
            if (apBehaviorEntry1 == null) {
                ApBehaviorEntry apBehaviorEntry = new ApBehaviorEntry();
                apBehaviorEntry.setType(ApBehaviorEntry.Type.USER.getCode());
                apBehaviorEntry.setRefId(userId);
                apBehaviorEntry.setCreatedTime(new Date());
                mongoTemplate.save(apBehaviorEntry);
                return apBehaviorEntry;
            }
            return apBehaviorEntry1;
        }
        //1.1为null
        //2.判断设备ID是否为null
        if (equipmentId != null){
            ApBehaviorEntry behaviorEntry = mongoTemplate.findOne(Query.query(Criteria.where("refId").is(equipmentId).and("type").is(0)), ApBehaviorEntry.class);
            if (behaviorEntry == null) {
                ApBehaviorEntry apBehaviorEntry = new ApBehaviorEntry();
                apBehaviorEntry.setType(ApBehaviorEntry.Type.EQUIPMENT.getCode());
                apBehaviorEntry.setRefId(equipmentId);
                apBehaviorEntry.setCreatedTime(new Date());
                mongoTemplate.save(apBehaviorEntry);
                return apBehaviorEntry;
            }
            return behaviorEntry;
        }
        //2.1为null  返回
        return null;
    }
}
