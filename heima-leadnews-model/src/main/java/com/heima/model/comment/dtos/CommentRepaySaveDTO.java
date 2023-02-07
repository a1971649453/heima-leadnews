package com.heima.model.comment.dtos;

import lombok.Data;
import org.hibernate.validator.constraints.Length;

import javax.validation.constraints.NotNull;

@Data
public class CommentRepaySaveDTO {
    /**
     * 评论id
     */
    @NotNull(message = "评论id不能为空")
    private String commentId;
    /**
     * 回复内容
     */
    @NotNull(message = "回复评论内容不能为空")
    @Length(max = 140,message = "回复评论内容不能超过140个字符")
    private String content;
}