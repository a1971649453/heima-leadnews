package com.heima.comment.controller.v1;

import com.heima.comment.service.CommentRepayService;
import com.heima.model.comment.dtos.CommentRepayDTO;
import com.heima.model.comment.dtos.CommentRepayLikeDTO;
import com.heima.model.comment.dtos.CommentRepaySaveDTO;
import com.heima.model.common.dtos.ResponseResult;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
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
@RequestMapping("/api/v1/comment_repay")
@Api("评论回复管理API")
public class CommentRepayController {
    @Resource
    private CommentRepayService commentRepayService;

    /**
     * 查看更多回复内容
     * @param dto
     * @return
     */
    @PostMapping("/load")
    @ApiOperation(value = "查看更多回复内容",notes = "查看更多回复内容")
    public ResponseResult loadCommentRepay(@RequestBody @Validated CommentRepayDTO dto){
        return commentRepayService.loadCommentRepay(dto);
    }

    /**
     * 保存回复
     * @return
     */
    @PostMapping("/save")
    @ApiOperation(value = "保存回复",notes = "保存回复")
    public ResponseResult saveCommentRepay(@RequestBody @Validated CommentRepaySaveDTO dto){
        return commentRepayService.saveCommentRepay(dto);
    }
    @PostMapping("/like")
    @ApiOperation(value = "点赞回复评论",notes = "点赞回复评论")
    public ResponseResult saveCommentRepayLike(@RequestBody @Validated CommentRepayLikeDTO dto){
        return commentRepayService.saveCommentRepayLike(dto);
    }


}
