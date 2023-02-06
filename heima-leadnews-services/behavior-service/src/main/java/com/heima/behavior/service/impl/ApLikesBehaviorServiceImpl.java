package com.heima.behavior.service.impl;
import java.util.Date;

import com.heima.behavior.service.ApBehaviorEntryService;
import com.heima.behavior.service.ApLikesBehaviorService;
import com.heima.common.exception.CustException;
import com.heima.model.behavior.dtos.LikesBehaviorDTO;
import com.heima.model.behavior.pojos.ApBehaviorEntry;
import com.heima.model.behavior.pojos.ApLikesBehavior;
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
public class ApLikesBehaviorServiceImpl implements ApLikesBehaviorService {
    @Resource
    private MongoTemplate mongoTemplate;
    @Resource
    private ApBehaviorEntryService apBehaviorEntryService;
    @Override
    public ResponseResult like(LikesBehaviorDTO dto) {
        //参数检验
        if (dto == null){
            CustException.cust(AppHttpCodeEnum.PARAM_REQUIRE,"参数不能为null");
        }
        //获取当前用户 如果不为null 根据当前用户ID获得APBehaviorEntry
        ApUser user = AppThreadLocalUtils.getUser();
        if (user == null){
            //如果用户ID未登录 直接返回 不能进行点赞
            CustException.cust(AppHttpCodeEnum.NEED_LOGIN);
        }
        ApBehaviorEntry apBehaviorEntry = apBehaviorEntryService.findByUserIdOrEquipmentId(user.getId(), dto.getEquipmentId());
        if (apBehaviorEntry == null){
            CustException.cust(AppHttpCodeEnum.DATA_NOT_EXIST,"用户ID和设备ID为空,不能创建行为实体");
        }
        String apBehaviorEntryId = apBehaviorEntry.getId();
        ApLikesBehavior likesBehavior = mongoTemplate.findOne(Query.query(Criteria.where("entryId").is(apBehaviorEntryId).and("articleId").is(dto.getArticleId())), ApLikesBehavior.class);
        //是否是点赞操作 0点赞 1取消点赞
        Short operation = dto.getOperation();
        //如果 是0点赞 查看是否已经点过赞
        if (operation.intValue() == 0){
           if (likesBehavior != null ){
               return ResponseResult.errorResult(AppHttpCodeEnum.DATA_NOT_ALLOW,"您已经点过赞了");
           }
           // 没点过赞 新建点赞行为对象
            ApLikesBehavior apLikesBehavior = new ApLikesBehavior();
            apLikesBehavior.setEntryId(apBehaviorEntryId);
            apLikesBehavior.setArticleId(dto.getArticleId());
            apLikesBehavior.setType(dto.getType());
            apLikesBehavior.setCreatedTime(new Date());
            //保存
            mongoTemplate.save(apLikesBehavior);
            return ResponseResult.okResult();
        }else {
            //取消点赞 删除点赞行为对象
            if (likesBehavior == null){
                return ResponseResult.errorResult(AppHttpCodeEnum.DATA_NOT_EXIST,"您还未进行点赞!");
            }
            mongoTemplate.remove(Query.query(Criteria.where("entryId").is(apBehaviorEntryId)), ApLikesBehavior.class);
        }
        return ResponseResult.okResult();
    }
}
