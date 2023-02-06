package com.heima.behavior.service.impl;
import java.util.Date;

import com.heima.behavior.service.ApBehaviorEntryService;
import com.heima.behavior.service.ApUnlikeBehaviorService;
import com.heima.common.exception.CustException;
import com.heima.model.behavior.dtos.UnLikesBehaviorDTO;
import com.heima.model.behavior.pojos.ApBehaviorEntry;
import com.heima.model.behavior.pojos.ApUnlikesBehavior;
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
public class ApUnlikeBehaviorServiceImpl implements ApUnlikeBehaviorService {
    @Resource
    private MongoTemplate mongoTemplate;
    @Resource
    ApBehaviorEntryService apBehaviorEntryService;
    @Override
    public ResponseResult unlikeBehavior(UnLikesBehaviorDTO dto) {
        //1.参数检验 注解检验

        //2.用户是否登录 未登录不能进行操作
        ApUser user = AppThreadLocalUtils.getUser();
        if (user == null){
            return ResponseResult.errorResult(AppHttpCodeEnum.NEED_LOGIN);
        }
        //3.当前操作是不喜欢0  取消不喜欢1
        Short type = dto.getType();
        //查询行为对象
        ApBehaviorEntry behaviorEntry = apBehaviorEntryService.findByUserIdOrEquipmentId(user.getId(), null);
        String entryId =  behaviorEntry.getId();
        //查询不喜欢行为对象
        ApUnlikesBehavior apUnlikesBehavior = mongoTemplate.findOne(
                Query.query(Criteria.where("entryId").is(behaviorEntry.getId()).and("articleId").is(dto.getArticleId())), ApUnlikesBehavior.class);
        if (apUnlikesBehavior == null){
            //不喜欢
            if (type.equals(ApUnlikesBehavior.Type.UNLIKE.getCode())){
                ApUnlikesBehavior apUnlikesBehavior1 = new ApUnlikesBehavior();
                apUnlikesBehavior1.setEntryId(entryId);
                apUnlikesBehavior1.setArticleId(dto.getArticleId());
                apUnlikesBehavior1.setType(dto.getType());
                apUnlikesBehavior1.setCreatedTime(new Date());
                mongoTemplate.save(apUnlikesBehavior1);
            }else {
                return ResponseResult.errorResult(AppHttpCodeEnum.DATA_NOT_ALLOW,"您并未添加此作品至不喜欢");
            }
        }else {
            if (type.equals(ApUnlikesBehavior.Type.CANCEL.getCode())){
                mongoTemplate.remove(Query.query(Criteria.where("entryId").is(behaviorEntry.getId()).and("articleId").is(entryId)),ApUnlikesBehavior.class);
            }else {
                return ResponseResult.errorResult(AppHttpCodeEnum.DATA_NOT_ALLOW,"您已经添加此作品至不喜欢");
            }
        }
        return ResponseResult.okResult();
    }
}
