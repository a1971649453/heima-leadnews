package com.heima.admin.controller.v1;

import com.heima.admin.service.ChannelService;
import com.heima.model.admin.Dtos.ChannelDTO;
import com.heima.model.admin.pojo.AdChannel;
import com.heima.model.common.dtos.ResponseResult;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;

/**
 * @author 金宗文
 * @version 1.0
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/channel")
@Api(value = "频道管理controller", tags = "频道管理controller")
public class ChannelController {

    @Resource
    private ChannelService channelService;

    /**
     * 根据名称分页查询频道列表
     * @param dto
     * @return
     */
    @PostMapping("/list")
    @ApiOperation("频道分页列表查询")
    public ResponseResult list(@RequestBody ChannelDTO dto){
        return channelService.findByNameAndPage(dto);
    }

    /**
     * 新增
     * @param channel
     * @return
     */
    @PostMapping("/save")
    @ApiOperation("保存频道信息")
    public ResponseResult insert(@RequestBody AdChannel channel){
        return channelService.insert(channel);
    }

    /**
     * 频道修改
     * @param adChannel 频道
     * @return
     */
    @ApiOperation("频道修改")
    @PostMapping("/update")
    public ResponseResult update(@RequestBody AdChannel adChannel) {
        return channelService.update(adChannel);
    }

    /**
     * 删除
     * @param id
     * @return
     */
    @ApiOperation("根据频道ID删除")
    @GetMapping("/del/{id}")
    public ResponseResult deleteById(@PathVariable("id") Integer id) {
        return channelService.deleteById(id);
    }


}
