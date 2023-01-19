package com.heima.wemedia.controller.v1;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.heima.model.common.dtos.ResponseResult;
import com.heima.model.wemedia.dtos.WmMaterialDTO;
import com.heima.model.wemedia.pojos.WmUser;
import com.heima.wemedia.service.WmUserService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;

/**
 * @author 金宗文
 * @version 1.0
 */
@Api(value = "自媒体用户API",tags = "自媒体用户API")
@Slf4j
@RequestMapping("/api/v1/user")
@RestController
public class WmUserController {
    @Resource
    private WmUserService wmUserService;


    @ApiOperation("根据用户名查询用户信息")
    @GetMapping("/findByName/{name}")
    public ResponseResult findByName(@PathVariable("name") String name){
        WmUser wmUser = wmUserService.getOne(Wrappers.<WmUser>lambdaQuery().eq(WmUser::getName, name));

        return ResponseResult.okResult(wmUser);
    }


    @ApiOperation("保存用户信息")
    @PostMapping("/save")
    public ResponseResult save(@RequestBody WmUser wmUser){
        //wmUser 新增用户名没有ID 由mybatisplus自动生成
        wmUserService.save(wmUser);

        return ResponseResult.okResult(wmUser);
    }



}
