package com.heima.admin.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.heima.model.admin.Dtos.ChannelDTO;
import com.heima.model.admin.Dtos.SensitiveDTO;
import com.heima.model.admin.pojo.AdChannel;
import com.heima.model.admin.pojo.AdSensitive;
import com.heima.model.common.dtos.ResponseResult;

import java.util.List;

/**
 * @author 金宗文
 * @version 1.0
 */
public interface AdSensitiveService extends IService<AdSensitive> {

    /**
     * 查询敏感词列表
     * @param dto
     * @return
     */
    public ResponseResult list(SensitiveDTO dto);

    /**
     * 新增
     * @param adSensitive
     * @return
     */
    public ResponseResult insert(AdSensitive adSensitive);

    /**
     * 修改
     * @param adSensitive
     * @return
     */
    public ResponseResult update(AdSensitive adSensitive);

    /**
     * 删除
     * @param id
     * @return
     */
    public ResponseResult delete(Integer id);

    /**
     * 查询敏感词内容列表
     * @return
     */
    public ResponseResult<List<String>> selectAllSensitives();
}
