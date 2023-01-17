package com.heima.article.controller;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.heima.article.service.AuthorService;
import com.heima.model.article.pojos.ApAuthor;
import com.heima.model.common.dtos.ResponseResult;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;

/**
 * @author 金宗文
 * @version 1.0
 */
@Api("文章作者controller")
@RestController
@RequestMapping("/api/v1/author")
public class AuthorController {
    @Resource
    private AuthorService authorService;

    @ApiOperation("根据用户Id查询作者信息")
    @GetMapping("/findByUserId/{userId}")
    public ResponseResult findByUserId(@PathVariable("userId") Integer userId){
        ApAuthor apAuthor = authorService.getOne(Wrappers.<ApAuthor>lambdaQuery().eq(ApAuthor::getUserId, userId));
        return ResponseResult.okResult(apAuthor);
    }

    @ApiOperation(("保存作者信息"))
    @PostMapping("/save")
    public ResponseResult save(@RequestBody ApAuthor apAuthor){
        authorService.save(apAuthor);
        return ResponseResult.okResult();
    }
}
