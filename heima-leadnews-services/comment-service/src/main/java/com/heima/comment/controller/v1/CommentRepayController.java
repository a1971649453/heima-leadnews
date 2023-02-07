package com.heima.comment.controller.v1;

import com.heima.comment.service.CommentRepayService;
import com.heima.model.comment.dtos.CommentRepayDTO;
import com.heima.model.comment.dtos.CommentRepayLikeDTO;
import com.heima.model.comment.dtos.CommentRepaySaveDTO;
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
@RequestMapping("/api/v1/comment_repay")
public class CommentRepayController {
    @Resource
    private CommentRepayService commentRepayService;

    /**
     * 查看更多回复内容
     * @param dto
     * @return
     */
    @PostMapping("/load")
    public ResponseResult loadCommentRepay(@RequestBody @Validated CommentRepayDTO dto){
        return commentRepayService.loadCommentRepay(dto);
    }

    /**
     * 保存回复
     * @return
     */
    @PostMapping("/save")
    public ResponseResult saveCommentRepay(@RequestBody @Validated CommentRepaySaveDTO dto){
        return commentRepayService.saveCommentRepay(dto);
    }
    @PostMapping("/like")
    public ResponseResult saveCommentRepayLike(@RequestBody @Validated CommentRepayLikeDTO dto){
        return commentRepayService.saveCommentRepayLike(dto);
    }


}
