package com.heima.comment.controller.v1;

import com.heima.comment.service.CommentService;
import com.heima.model.comment.dtos.CommentDTO;
import com.heima.model.comment.dtos.CommentLikeDTO;
import com.heima.model.comment.dtos.CommentSaveDTO;
import com.heima.model.common.dtos.ResponseResult;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
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
@RequestMapping("/api/v1/comment")
@Api(value = "评论管理API",tags = "评论管理API")
public class CommentController {
    @Resource
    private CommentService commentService;

    @PostMapping("/save")
    @ApiOperation(value = "保存评论",notes = "保存评论")
    public ResponseResult saveComment(@RequestBody @Validated CommentSaveDTO dto){
        return commentService.saveComment(dto);
    }

    @PostMapping("/like")
    @ApiOperation(value = "点赞评论",notes = "点赞评论")
    public ResponseResult like(@RequestBody @Validated CommentLikeDTO dto){
        return commentService.like(dto);
    }

    @PostMapping("/load")
    @ApiOperation("加载文章评论列表")
    public ResponseResult findByArticleId(@RequestBody @Validated CommentDTO dto){
        return commentService.findByArticleId(dto);
    }
}
