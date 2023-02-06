package com.heima.behavior.service.impl;
import java.util.Date;

import com.heima.behavior.service.ApBehaviorEntryService;
import com.heima.behavior.service.ApReadBehaviorService;
import com.heima.common.exception.CustException;
import com.heima.model.behavior.dtos.ReadBehaviorDTO;
import com.heima.model.behavior.pojos.ApBehaviorEntry;
import com.heima.model.behavior.pojos.ApReadBehavior;
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
public class ApReadBehaviorServiceImpl implements ApReadBehaviorService {
    @Resource
    private MongoTemplate mongoTemplate;

    @Resource
    private ApBehaviorEntryService apBehaviorEntryService;
    @Override
    public ResponseResult readBehavior(ReadBehaviorDTO dto) {
        //1.检验阅读行为DTO参数 使用注解检测
        //2.获取当前用户
        ApUser user = AppThreadLocalUtils.getUser();
        String entryId = "";
        if (user == null){
            //如果用户没登录根据设备关联的行为实体数据进行保存
            // 1.查询设备相关的行为实体对象
            Integer equipmentId = dto.getEquipmentId();
            if (equipmentId == null){
                ResponseResult.errorResult(AppHttpCodeEnum.DATA_NOT_EXIST,"未获得任何数据,不允许此操作,请登录或者提供设备代码");
            }
            ApBehaviorEntry equipmentEntry = apBehaviorEntryService.findByUserIdOrEquipmentId(null, equipmentId);
            entryId = equipmentEntry.getId();

        }else {
            ApBehaviorEntry userBehaviorEntry = apBehaviorEntryService.findByUserIdOrEquipmentId(user.getId(), null);
            entryId = userBehaviorEntry.getId();
        }
        // 获取相关阅读行为对象
        ApReadBehavior readBehavior = mongoTemplate.findOne(Query.query(Criteria.where("entryId").is(entryId).and("articleId").is(dto.getArticleId())), ApReadBehavior.class);
        if (readBehavior == null){
            // 没有阅读 这是第一次阅读 新建阅读行为实体对象
            ApReadBehavior apReadBehavior = new ApReadBehavior();
            apReadBehavior.setEntryId(entryId);
            apReadBehavior.setArticleId(dto.getArticleId());
            apReadBehavior.setCount((short)1);
            apReadBehavior.setCreatedTime(new Date());
            apReadBehavior.setUpdatedTime(new Date());
            mongoTemplate.save(apReadBehavior);
        }else {
            readBehavior.setCount((short) (readBehavior.getCount() + 1));
        }
        return ResponseResult.okResult();
    }
}
