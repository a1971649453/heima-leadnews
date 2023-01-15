package com.heima.admin.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.heima.admin.mapper.ChannelMapper;
import com.heima.admin.service.ChannelService;
import com.heima.common.exception.CustException;
import com.heima.common.exception.CustomException;
import com.heima.model.admin.Dtos.ChannelDTO;
import com.heima.model.admin.pojo.AdChannel;
import com.heima.model.common.dtos.PageResponseResult;
import com.heima.model.common.dtos.ResponseResult;
import com.heima.model.common.enums.AppHttpCodeEnum;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author 金宗文
 * @version 1.0
 */
@Service
public class ChannelServiceImpl extends ServiceImpl<ChannelMapper, AdChannel> implements ChannelService {

    /**
     * 根据名称分页查询频道列表
     * @param dto
     * @return
     */
    @Override
    public ResponseResult findByNameAndPage(ChannelDTO dto) {

        //1.校验参数 非空判断,分页判断
        if (dto == null){
            return ResponseResult.errorResult(AppHttpCodeEnum.PARAM_INVALID,"参数错误");
        }
        // 分页参数校验
        dto.checkParam();
        //2.条件查询
        // 2.1 分页对象
        Page<AdChannel> pageReq = new Page<>(dto.getPage(),dto.getSize());
        // 2.2 设置条件
        LambdaQueryWrapper<AdChannel> wrapper = Wrappers.lambdaQuery();
        // name 不为空
        if (StringUtils.isNotBlank(dto.getName())){
           wrapper.like(AdChannel::getName,dto.getName());
        }
        // status 不为空
        if (dto.getStatus() != null){
            wrapper.eq(AdChannel::getStatus,dto.getStatus());
        }
        // 排序 按照排序字段ord升序排序
        wrapper.orderByAsc(AdChannel::getOrd);

        // 分页查询
        IPage<AdChannel> pageResult = this.page(pageReq, wrapper);

        PageResponseResult result = new PageResponseResult(dto.getPage(), dto.getSize(), pageResult.getTotal(), pageResult.getRecords());

        //4.封装返回结果
        return result;
    }

    /**
     * 新增
     * @param adchannel
     * @return
     */
    @Override
    public ResponseResult insert(AdChannel adchannel) {
        //1.参数校验 频道名称不能为空,
        if (adchannel == null || StringUtils.isBlank(adchannel.getName())){
            return ResponseResult.errorResult(AppHttpCodeEnum.PARAM_REQUIRE);
        }

        //2.不能大于10个字符 且频道名称不可以重复
        if (adchannel.getName().length() > 10){
            return ResponseResult.errorResult(AppHttpCodeEnum.PARAM_INVALID,"频道名称不能大于10个字符");
        }
        // 查找出此频道名称的数量
        int count = this.count(Wrappers.<AdChannel>lambdaQuery().eq(AdChannel::getName, adchannel.getName()));
        if (count > 0){
            return ResponseResult.errorResult(AppHttpCodeEnum.DATA_EXIST,"频道名称已经存在");
        }
        // 执行新增
        this.save(adchannel);

        // 返回结果
        return ResponseResult.okResult();
    }

    @Override
    public ResponseResult update(AdChannel adChannel) {

        // 校验参数(id)
        if (adChannel == null || adChannel.getId() == null){
            return ResponseResult.errorResult(AppHttpCodeEnum.PARAM_INVALID,"频道ID不能为空");
        }
        // 如果 频道名称不为空,并且新的频道名称不等于旧的频道名称
        AdChannel oldChannel = this.getById(adChannel.getId());
        if (oldChannel == null){
            return ResponseResult.errorResult(AppHttpCodeEnum.DATA_NOT_EXIST,"频道不存在");
        }
        if (StringUtils.isNotBlank(adChannel.getName()) && !adChannel.getName().equals(oldChannel.getName())){
            // 判断新的频道名称是否已经存在
            int count = this.count(Wrappers.<AdChannel>lambdaQuery().eq(AdChannel::getName, adChannel.getName()));
            if (count > 0){
                return ResponseResult.errorResult(AppHttpCodeEnum.DATA_EXIST,"频道名称已经存在");
            }
        }
        // 修改频道
        updateById(adChannel);
        // 返回结果
        return ResponseResult.okResult();

    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public ResponseResult deleteById(Integer id) {
        // 1.校验参数
        if(id == null){
            return ResponseResult.errorResult(AppHttpCodeEnum.DATA_NOT_EXIST,"频道ID不能为空");
        }
        // 如果当前状态为有效则不能删除
        // 1.去查询当前频道
        AdChannel channel = this.getById(id);
        if (channel == null){
            return ResponseResult.errorResult(AppHttpCodeEnum.DATA_NOT_EXIST,"频道不存在");
        }

        if (channel.getStatus()){
            return ResponseResult.errorResult(AppHttpCodeEnum.DATA_NOT_ALLOW,"频道有效不允许删除");
        }
        // 测试统一异常处理
//         int i = 1 / 0;

        //删除
        this.removeById(id);

//        if (id > 45){
//            CustException.cust(AppHttpCodeEnum.PARAM_INVALID);
//        }

        return ResponseResult.okResult();
    }
}
