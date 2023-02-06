package com.heima.model.behavior.dtos;
import lombok.Data;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

@Data
public class ReadBehaviorDTO {
    // 设备ID
    Integer equipmentId;
    // 文章、动态、评论等ID
    @NotNull(message = "文章ID不能为空")
    Long articleId;
    /**
     * 阅读次数  
     */
    Short count;
}