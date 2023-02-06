package com.heima.model.behavior.dtos;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;

import javax.validation.constraints.NotNull;

@Data
public class ArticleBehaviorDTO {
    // 设备ID
    Integer equipmentId;
    // 文章ID
    @NotNull(message = "文章ID不能为空")
    @JsonSerialize(using = ToStringSerializer.class)
    Long articleId;
    // 作者ID
    Integer authorId;
    // 作者对应的apuserid
    @NotNull(message = "作者app ID不能为空")
    Integer authorApUserId;
}