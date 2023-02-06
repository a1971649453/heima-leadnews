package com.heima.behavior.controller.v1;

import com.heima.behavior.service.ApLikesBehaviorService;
import com.heima.model.behavior.dtos.LikesBehaviorDTO;
import com.heima.model.common.dtos.ResponseResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
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
@Slf4j
@RequestMapping("/api/v1")
public class ApLikesBehaviorController {

    @Resource
    private ApLikesBehaviorService apLikesBehaviorService;

    @PostMapping("/likes_behavior")
    public ResponseResult like(@RequestBody @Validated LikesBehaviorDTO dto){
        return apLikesBehaviorService.like(dto);
    }

}
