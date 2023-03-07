package com.heima.search.service.impl;

import com.heima.common.exception.CustException;
import com.heima.feigns.BehaviorFeign;
import com.heima.model.behavior.pojos.ApBehaviorEntry;
import com.heima.model.common.dtos.ResponseResult;
import com.heima.model.common.enums.AppHttpCodeEnum;
import com.heima.model.search.dtos.HistorySearchDTO;
import com.heima.model.search.dtos.UserSearchDTO;
import com.heima.model.search.pojos.ApUserSearch;
import com.heima.model.threadlocal.AppThreadLocalUtils;
import com.heima.model.user.pojos.ApUser;
import com.heima.search.service.ApUserSearchService;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Date;
import java.util.List;

/**
 * @author 金宗文
 * @version 1.0
 */

@Service
public class ApUserSearchServiceImpl implements ApUserSearchService {
    @Resource
    private BehaviorFeign behaviorFeign;

    @Resource
    private MongoTemplate mongoTemplate;
    @Override
    @Async("taskExecutor")
    public void insert(UserSearchDTO userSearchDto) {
        // 保存搜索记录
        // 1.根据用户ID  或设备id查询对应行为实体
        String searchWords = userSearchDto.getSearchWords();
        ResponseResult<ApBehaviorEntry> result = behaviorFeign.findByUserIdOrEquipmentId(userSearchDto.getLoginUserId(), userSearchDto.getEquipmentId());
        if (!result.checkCode()){
            CustException.cust(AppHttpCodeEnum.REMOTE_SERVER_ERROR,result.getErrorMessage());
        }
        ApBehaviorEntry behaviorEntry = result.getData();
        // 2. 根据行为实体id 及 关键词 查询是否存在
        Query query = Query.query(Criteria.where("entryId").is(behaviorEntry.getId()).and("keyword").is(searchWords));
        ApUserSearch userSearch = mongoTemplate.findOne(query, ApUserSearch.class);

        // 3.如果存在该历史记录 修改更新时间
        if (userSearch != null){
            userSearch.setCreatedTime(new Date());
            mongoTemplate.save(userSearch);
            return;
        }
        //4. 如果不存在 则新增记录
        userSearch = new ApUserSearch();
        userSearch.setEntryId(behaviorEntry.getId());
        userSearch.setKeyword(searchWords);
        userSearch.setCreatedTime(new Date());
        mongoTemplate.insert(userSearch);


    }

    /**
     * 加载用户搜索记录
     * @param userSearchDto
     * @return
     */
    @Override
    public ResponseResult findUserSearch(UserSearchDTO userSearchDto) {
        // 1.检验参数
        ApUser user = AppThreadLocalUtils.getUser();

        // 2.用户是否登录 如果登陆 根据用户ID  查询10条行为实体
        ResponseResult<ApBehaviorEntry> result = behaviorFeign.findByUserIdOrEquipmentId(user == null ? null : user.getId(), userSearchDto.getEquipmentId());
        if (!result.checkCode()){
            CustException.cust(AppHttpCodeEnum.REMOTE_SERVER_ERROR,result.getErrorMessage());
        }
        ApBehaviorEntry behaviorEntry = result.getData();
        // 3. 根据行为实体ID查询 10条行为实体
        Query query = Query.query(Criteria.where("entryId").is(behaviorEntry.getId()))
                .with(Sort.by(Sort.Direction.DESC,"createdTime"))
                .limit(10);
        List<ApUserSearch> apUserSearches = mongoTemplate.find(query, ApUserSearch.class);

        return ResponseResult.okResult(apUserSearches);
    }

    /**
     * 删除用户搜索记录
     * @param historySearchDto
     * @return
     */
    @Override
    public ResponseResult delUserSearch(HistorySearchDTO historySearchDto) {
        // 1.检验参数
        if (historySearchDto.getId() == null){
            return ResponseResult.errorResult(AppHttpCodeEnum.PARAM_INVALID);
        }
        ApUser user = AppThreadLocalUtils.getUser();
        ResponseResult<ApBehaviorEntry> result = behaviorFeign.findByUserIdOrEquipmentId(user == null ? null : user.getId(), historySearchDto.getEquipmentId());
        if (!result.checkCode()){
            CustException.cust(AppHttpCodeEnum.REMOTE_SERVER_ERROR,result.getErrorMessage());
        }
        ApBehaviorEntry behaviorEntry = result.getData();
        if(behaviorEntry == null){
            return ResponseResult.errorResult(AppHttpCodeEnum.DATA_NOT_EXIST,"行为实体数据不存在");
        }
        // 2.根据行为实体ID  删除搜索记录
        mongoTemplate.remove(Query.query(Criteria.where("entryId").is(behaviorEntry.getId()).and("_id").is(historySearchDto.getId())),ApUserSearch.class);
        return ResponseResult.okResult();
    }
}
