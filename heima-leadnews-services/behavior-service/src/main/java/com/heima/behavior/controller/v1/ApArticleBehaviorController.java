package com.heima.behavior.controller.v1;

import com.heima.behavior.service.ApArticleBehaviorService;
import com.heima.model.behavior.dtos.ArticleBehaviorDTO;
import com.heima.model.behavior.dtos.CollectionBehaviorDTO;
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
@RequestMapping("/api/v1/article")
public class ApArticleBehaviorController {
    @Resource
    private ApArticleBehaviorService apArticleBehaviorService;

    @PostMapping("/load_article_behavior")
    public ResponseResult loadArticleBehavior(@RequestBody @Validated ArticleBehaviorDTO dto){
        return apArticleBehaviorService.loadArticleBehavior(dto);
    }
}
