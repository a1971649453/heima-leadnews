package com.heima.behavior.controller.v1;

import com.heima.behavior.service.ApUnlikeBehaviorService;
import com.heima.model.behavior.dtos.UnLikesBehaviorDTO;
import com.heima.model.common.dtos.ResponseResult;
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
@RequestMapping("/api/v1")
public class APUnlikeBehaviorController {
    @Resource
    private ApUnlikeBehaviorService apUnlikeBehaviorService;

    @PostMapping("/un_likes_behavior")
    public ResponseResult unlikeBehavior(@RequestBody @Validated UnLikesBehaviorDTO dto){
        return apUnlikeBehaviorService.unlikeBehavior(dto);
    }
}
