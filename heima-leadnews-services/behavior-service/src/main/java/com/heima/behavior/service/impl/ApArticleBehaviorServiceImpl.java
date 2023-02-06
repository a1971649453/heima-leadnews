package com.heima.behavior.service.impl;

import com.heima.behavior.service.ApArticleBehaviorService;
import com.heima.behavior.service.ApBehaviorEntryService;
import com.heima.common.exception.CustException;
import com.heima.model.behavior.dtos.ArticleBehaviorDTO;
import com.heima.model.behavior.pojos.ApBehaviorEntry;
import com.heima.model.behavior.pojos.ApCollection;
import com.heima.model.behavior.pojos.ApLikesBehavior;
import com.heima.model.behavior.pojos.ApUnlikesBehavior;
import com.heima.model.common.constants.user.UserRelationConstants;
import com.heima.model.common.dtos.ResponseResult;
import com.heima.model.common.enums.AppHttpCodeEnum;
import com.heima.model.threadlocal.AppThreadLocalUtils;
import com.heima.model.user.pojos.ApUser;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.Map;

/**
 * @author 金宗文
 * @version 1.0
 */
@Service
public class ApArticleBehaviorServiceImpl implements ApArticleBehaviorService {
    @Resource
    private MongoTemplate mongoTemplate;
    @Resource
    private ApBehaviorEntryService apBehaviorEntryService;
    @Resource
    private StringRedisTemplate stringRedisTemplate;


    @Override
    public ResponseResult loadArticleBehavior(ArticleBehaviorDTO dto) {
        Map<String, Boolean> map = new HashMap<>();
        //1.判断用户是否登录
        ApUser user = AppThreadLocalUtils.getUser();
        if (user == null){
            map.put("islike",false);
            map.put("isfollow",false);
            map.put("isunlike",false);
            map.put("iscollection",false);
            return ResponseResult.okResult(map);
        }
        //2.查询行为实体
        ApBehaviorEntry behaviorEntry = apBehaviorEntryService.findByUserIdOrEquipmentId(user.getId(), null);
        String entryId = behaviorEntry.getId();
        //根据行为实体 文章ID查询 是否点赞
        ApLikesBehavior apLikesBehavior = mongoTemplate.findOne(Query.query(Criteria.where("entryId").is(entryId).and("articleId").is(dto.getArticleId())), ApLikesBehavior.class);
        map.put("islike", apLikesBehavior != null);
        //根据行为实体 文章ID查询 是否不喜欢
        ApUnlikesBehavior apUnlikesBehavior = mongoTemplate.findOne(Query.query(Criteria.where("entryId").is(entryId).and("articleId").is(dto.getArticleId())), ApUnlikesBehavior.class);
        map.put("isunlike", apUnlikesBehavior != null);
        //根据行为实体 文章ID查询 是否收藏
        ApCollection apCollection = mongoTemplate.findOne(Query.query(Criteria.where("entryId").is(entryId).and("articleId").is(dto.getArticleId())), ApCollection.class);
        map.put("iscollection",apCollection != null);
        //根据登录用户id 去redis中查询是否关注该作者
        Integer authorApUserId = dto.getAuthorApUserId();
        ZSetOperations<String, String> zSetOperations = stringRedisTemplate.opsForZSet();
        Double score = zSetOperations.score(UserRelationConstants.FOLLOW_LIST + user.getId().toString(), authorApUserId.toString());
        map.put("isfollow",score != null);
        return ResponseResult.okResult(map);
    }
}
