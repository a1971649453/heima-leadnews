package com.heima.model.comment.dtos;

import lombok.Data;
import org.hibernate.validator.constraints.Range;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

@Data
public class CommentRepayLikeDTO {
    /**
     * 回复id
     */
    @NotBlank(message = "回复id不能为空")
    private String commentRepayId;
    /**
     * 0：点赞
     * 1：取消点赞
     */
    @Range(min = 0,max = 1)
    @NotNull(message = "操作类型不能为空")
    private Short operation;
}