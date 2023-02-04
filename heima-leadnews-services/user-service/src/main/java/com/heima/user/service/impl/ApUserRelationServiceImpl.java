package com.heima.user.service.impl;

import com.heima.common.exception.CustException;
import com.heima.model.common.constants.user.UserRelationConstants;
import com.heima.model.common.dtos.ResponseResult;
import com.heima.model.common.enums.AppHttpCodeEnum;
import com.heima.model.threadlocal.AppThreadLocalUtils;
import com.heima.model.user.dtos.UserRelationDTO;
import com.heima.model.user.pojos.ApUser;
import com.heima.user.service.ApUserRelationService;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

/**
 * @author 金宗文
 * @version 1.0
 */
@Service
public class ApUserRelationServiceImpl implements ApUserRelationService {
    @Resource
    private StringRedisTemplate stringRedisTemplate;
    @Override
    public ResponseResult follow(UserRelationDTO dto) {
        //1.检查参数 (是否登录 operation 0或1 自己不能关注自己 是否已经关注)
        ApUser loginUser = AppThreadLocalUtils.getUser();
        if (loginUser == null){
            CustException.cust(AppHttpCodeEnum.NEED_LOGIN);
        }
        Short operation = dto.getOperation();
        if (operation != 0 && operation != 1){
            CustException.cust(AppHttpCodeEnum.PARAM_INVALID,"关注类型错误, 类型必须为0 或1");
        }

        //2. operation 为 0  关注 关注集合 粉丝集合 添加数据
        ZSetOperations<String, String> zSetOperations = stringRedisTemplate.opsForZSet();
        if (operation == 0){
            //自己不能关注自己
            if (dto.getAuthorApUserId().equals(loginUser.getId())){
                CustException.cust(AppHttpCodeEnum.DATA_NOT_ALLOW,"自己不能关注自己");
            }
            //是否已经关注
            // zscore 集合 key:
            Double score = zSetOperations.score(UserRelationConstants.FOLLOW_LIST + loginUser.getId(), String.valueOf(dto.getAuthorApUserId()));
            if (score != null){
                CustException.cust(AppHttpCodeEnum.DATA_NOT_ALLOW,"请勿重复关注");
            }
            // 当前用户关注集合 apuser:follow:用户ID [时间戳] 作者ID
            zSetOperations.add(UserRelationConstants.FOLLOW_LIST + loginUser.getId().toString(),String.valueOf(dto.getAuthorApUserId()),System.currentTimeMillis());

            // 作者粉丝集合 apuser:fans:作者ID [时间戳] 用户ID
            zSetOperations.add(UserRelationConstants.FANS_LIST + dto.getAuthorApUserId().toString(),String.valueOf(loginUser.getId()),System.currentTimeMillis());

        }
        //3. operation 为1 取消关注 关注集合 粉丝集合 删除数据
        else {

            // 当前用户关注集合 apuser:follow:用户ID [时间戳] 作者ID 删除
            zSetOperations.remove(UserRelationConstants.FOLLOW_LIST + loginUser.getId(),dto.getAuthorApUserId().toString());
            // 作者粉丝集合 apuser:fans:作者ID [时间戳] 用户ID 删除
            zSetOperations.remove(UserRelationConstants.FANS_LIST + dto.getAuthorApUserId(),loginUser.getId().toString());

        }
        return ResponseResult.okResult(AppHttpCodeEnum.SUCCESS);
    }
}
