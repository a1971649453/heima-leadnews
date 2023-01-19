package com.heima.wemedia.service.impl;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.heima.common.exception.CustException;
import com.heima.model.common.constants.admin.AdminConstants;
import com.heima.model.common.dtos.ResponseResult;
import com.heima.model.common.enums.AppHttpCodeEnum;
import com.heima.model.wemedia.dtos.WmUserDTO;
import com.heima.model.wemedia.pojos.WmUser;
import com.heima.model.wemedia.vo.WmUserVO;
import com.heima.utils.common.AppJwtUtil;
import com.heima.wemedia.mapper.WmUserMapper;
import com.heima.wemedia.service.WmUserService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Service
public class WmUserServiceImpl extends ServiceImpl<WmUserMapper, WmUser> implements WmUserService {

    @Override
    public ResponseResult login(WmUserDTO dto) {
        //1.检查参数
        if(StringUtils.isBlank(dto.getName())||StringUtils.isBlank(dto.getPassword())){
            CustException.cust(AppHttpCodeEnum.PARAM_INVALID);
        }
        //2. 根据用户名查询用户信息
        WmUser wmUser = this.getOne(Wrappers.<WmUser>lambdaQuery().eq(WmUser::getName, dto.getName()));
        //3. 比较密码是否正确
        String pswd = DigestUtils.md5DigestAsHex((dto.getPassword() + wmUser.getSalt()).getBytes());
        if (!wmUser.getPassword().equals(pswd)) {
            CustException.cust(AppHttpCodeEnum.LOGIN_PASSWORD_ERROR);
        }
        //4.判断user状态
        Integer status = wmUser.getStatus();
        if (status.intValue() != 9) {
            CustException.cust(AppHttpCodeEnum.LOGIN_STATUS_ERROR);
        }
        //5.修改登陆时间
        wmUser.setLoginTime(new Date());
        //6. 颁发token
        String token = AppJwtUtil.getToken(Long.valueOf(wmUser.getId()));

        // 7.封装返回结果
        WmUserVO wmUserVO = new WmUserVO();
        BeanUtils.copyProperties(wmUser,wmUserVO);
        Map result = new HashMap<>();
        result.put("token",token);
        result.put("user",wmUserVO);
        return ResponseResult.okResult(result);
    }
}