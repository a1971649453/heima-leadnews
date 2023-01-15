package com.heima.model.admin.Dtos;

import com.heima.model.common.dtos.PageRequestDTO;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * @author 金宗文
 * @version 1.0
 */
@Data
public class ChannelDTO extends PageRequestDTO {

    @ApiModelProperty("频道名称")
    private String name;

    @ApiModelProperty("频道状态")
    private Integer status;



}
