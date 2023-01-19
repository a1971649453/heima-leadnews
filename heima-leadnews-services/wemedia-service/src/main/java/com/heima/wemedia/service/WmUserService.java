package com.heima.wemedia.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.heima.model.common.dtos.ResponseResult;
import com.heima.model.wemedia.dtos.WmUserDTO;
import com.heima.model.wemedia.pojos.WmUser;
public interface WmUserService extends IService<WmUser> {

    /**
     * 登录
     * @param dto 微媒体用户登录信息(账号,密码)
     * @return
     */
    public ResponseResult login(WmUserDTO dto);
}