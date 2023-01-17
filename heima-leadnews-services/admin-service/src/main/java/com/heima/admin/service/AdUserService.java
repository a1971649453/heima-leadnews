package com.heima.admin.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.heima.model.admin.Dtos.AdUserDTO;
import com.heima.model.common.dtos.ResponseResult;
import com.heima.model.admin.pojo.AdUser;

/**
 * @author 金宗文
 * @version 1.0
 */
public interface AdUserService extends IService<AdUser> {
    /**
     * 登录功能
     * @param DTO
     * @return
     */
    ResponseResult login(AdUserDTO DTO);
}