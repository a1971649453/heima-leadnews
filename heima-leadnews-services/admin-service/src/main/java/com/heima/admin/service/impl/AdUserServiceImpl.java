package com.heima.admin.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.heima.admin.mapper.AdUserMapper;
import com.heima.admin.service.AdUserService;
import com.heima.common.exception.CustException;
import com.heima.model.admin.Dtos.AdUserDTO;
import com.heima.model.admin.pojo.AdUser;
import com.heima.model.admin.vo.AdUserVO;
import com.heima.model.common.dtos.ResponseResult;
import com.heima.model.common.enums.AppHttpCodeEnum;
import com.heima.utils.common.AppJwtUtil;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * @author 金宗文
 * @version 1.0
 */
@Service
public class AdUserServiceImpl extends ServiceImpl<AdUserMapper, AdUser> implements AdUserService {

    /**
     * 登陆功能
     * @param DTO
     * @return
     */
    @Override
    public ResponseResult login(AdUserDTO DTO) {
        // 1.校验参数 保证name password不为空
        String name = DTO.getName();
        String password = DTO.getPassword();
        if (StringUtils.isBlank(name) || StringUtils.isBlank(password)){
            CustException.cust(AppHttpCodeEnum.PARAM_INVALID,"用户名或密码不能为空");
        }
        // 2. 根据name 查询用户信息
        AdUser user = this.getOne(Wrappers.<AdUser>lambdaQuery().eq(AdUser::getName, name));
        if (user == null){
            CustException.cust(AppHttpCodeEnum.DATA_NOT_EXIST,"用户不存在");
        }
        
        // 3.判断输入密码和数据库密码是否一致
        String inputPwd = DigestUtils.md5DigestAsHex((password + user.getSalt()).getBytes());

        if (!inputPwd.equals(user.getPassword())){
            CustException.cust(AppHttpCodeEnum.LOGIN_PASSWORD_ERROR,"密码错误");
        }
        // 4.判断用户状态是否正确 9(正常)
        if (user.getStatus() != 9){
            CustException.cust(AppHttpCodeEnum.LOGIN_STATUS_ERROR);
        }

        // 5.修改最近登陆时间
        user.setLoginTime(new Date());
        this.updateById(user);
        // 6.颁发token
        String token = AppJwtUtil.getToken(Long.valueOf(user.getId()));
        // 7.封装返回结果
        Map result = new HashMap<>();
        result.put("token",token);
        AdUserVO userVO = new AdUserVO();
        BeanUtils.copyProperties(user,userVO);
        result.put("user",userVO);

        return ResponseResult.okResult(result);


    }
}
