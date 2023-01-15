package com.heima.admin.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.heima.model.admin.Dtos.ChannelDTO;
import com.heima.model.admin.pojo.AdChannel;
import com.heima.model.common.dtos.ResponseResult;

/**
 * @author 金宗文
 * @version 1.0
 */
public interface ChannelService extends IService<AdChannel> {


    /**
     * 根据名称分页查询频道列表
     * @param dto
     * @return
     */
    public ResponseResult findByNameAndPage(ChannelDTO dto);

    /**
     * 新增
     * @param channel
     * @return
     */
    public ResponseResult insert(AdChannel channel);


    /**
     * 修改
     * @param adChannel
     * @return
     */
    public ResponseResult update(AdChannel adChannel);


    /**
     * 删除
     * @param id
     * @return
     */
    public ResponseResult deleteById(Integer id);
}

