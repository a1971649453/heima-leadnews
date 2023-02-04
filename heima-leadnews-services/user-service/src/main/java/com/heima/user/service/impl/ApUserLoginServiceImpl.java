package com.heima.user.service.impl;

import com.baomidou.mybatisplus.core.toolkit.ObjectUtils;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.heima.common.exception.CustException;
import com.heima.model.common.dtos.ResponseResult;
import com.heima.model.common.enums.AppHttpCodeEnum;
import com.heima.model.user.dtos.LoginDTO;
import com.heima.model.user.pojos.ApUser;
import com.heima.user.mapper.ApUserMapper;
import com.heima.user.service.ApUserLoginService;
import com.heima.utils.common.AppJwtUtil;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.Map;

/**
 * @author 金宗文
 * @version 1.0
 */
@Service
public class ApUserLoginServiceImpl implements ApUserLoginService {
    @Resource
    ApUserMapper apUserMapper;
    @Override
    public ResponseResult login(LoginDTO dto) {
        //1.判断手机号 密码是否为空
        String phone = dto.getPhone();
        String password = dto.getPassword();
        //1.1不为空 查询手机号对应用户是否存在
        if (StringUtils.isNotBlank(phone) && StringUtils.isNotBlank(password)) {
            //1.2 对比输入密码和数据库密码是否一致
            ApUser apUser = apUserMapper.selectOne(Wrappers.<ApUser>lambdaQuery().eq(ApUser::getPhone, phone));
            if (ObjectUtils.isNull(apUser)){
                CustException.cust(AppHttpCodeEnum.AP_USER_DATA_NOT_EXIST,"用户对应信息不存在");
            }
            String dbPassword = apUser.getPassword();
            String inputPassword = DigestUtils.md5DigestAsHex((password + apUser.getSalt()).getBytes());
            if (!inputPassword.equals(dbPassword)){
                CustException.cust(AppHttpCodeEnum.LOGIN_PASSWORD_ERROR);
            }
            //1.3 颁发token并返回 (user token)
            String token = AppJwtUtil.getToken(Long.valueOf(apUser.getId()));
            Map result = new HashMap<>();
            //将敏感信息设置为空
            apUser.setPassword("");
            apUser.setSalt("");
            result.put("token",token);
            result.put("user",apUser);
            return ResponseResult.okResult(result);
        }


        //2. 如果为空 采用设备ID登陆
        // 2.1 判断设备ID是否存在
        if (dto.getEquipmentId() == null){
            return ResponseResult.errorResult(AppHttpCodeEnum.PARAM_INVALID);
        }
        // 2.2 直接颁发token userID设置为0
        String token = AppJwtUtil.getToken(0L);
        Map result = new HashMap();
        result.put("token",token);
        return ResponseResult.okResult(result);


    }
}
