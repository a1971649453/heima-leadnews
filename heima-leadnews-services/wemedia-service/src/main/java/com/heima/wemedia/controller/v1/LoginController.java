package com.heima.wemedia.controller.v1;

import com.heima.model.common.dtos.ResponseResult;
import com.heima.model.wemedia.dtos.WmUserDTO;
import com.heima.wemedia.service.WmUserService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

/**
 * @author 金宗文
 * @version 1.0
 */
@RestController
@RequestMapping("/login")
@Api("自媒体登录")
public class LoginController {
    @Resource
    private WmUserService wmUserService;

    @ApiOperation("自媒体登录方法")
    @PostMapping("/in")
    public ResponseResult login(@RequestBody WmUserDTO dto){
        return wmUserService.login(dto);
    }
}
