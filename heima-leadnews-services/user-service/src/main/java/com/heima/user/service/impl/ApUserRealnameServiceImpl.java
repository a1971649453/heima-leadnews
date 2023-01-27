package com.heima.user.service.impl;
import java.util.Date;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.heima.common.exception.CustException;
import com.heima.feigns.ArticleFeign;
import com.heima.feigns.WemediaFeign;
import com.heima.model.article.pojos.ApAuthor;
import com.heima.model.common.constants.admin.AdminConstants;
import com.heima.model.common.dtos.PageResponseResult;
import com.heima.model.common.dtos.ResponseResult;
import com.heima.model.common.enums.AppHttpCodeEnum;
import com.heima.model.user.dtos.AuthDTO;
import com.heima.model.user.pojos.ApUser;
import com.heima.model.user.pojos.ApUserRealname;
import com.heima.model.wemedia.pojos.WmUser;
import com.heima.user.mapper.ApUserMapper;
import com.heima.user.mapper.ApUserRealnameMapper;
import com.heima.user.service.ApUserRealnameService;
import io.seata.spring.annotation.GlobalTransactional;
import jdk.nashorn.internal.ir.annotations.Reference;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;

/**
 * @author 金宗文
 * @version 1.0
 */
@Service
public class ApUserRealnameServiceImpl extends ServiceImpl<ApUserRealnameMapper, ApUserRealname> implements ApUserRealnameService {

    @Override
    public ResponseResult loadListByStatus(AuthDTO DTO) {
        //1. 校验参数
        if (DTO == null){
            CustException.cust(AppHttpCodeEnum.PARAM_INVALID,"参数不能为空");
        }
        DTO.checkParam();
        //2. 封装分页查询条件
        Page<ApUserRealname> pageReq = new Page<>(DTO.getPage(), DTO.getSize());

        LambdaQueryWrapper<ApUserRealname> queryWrapper = Wrappers.<ApUserRealname>lambdaQuery();

        queryWrapper.eq(DTO.getStatus() != null, ApUserRealname::getStatus, DTO.getStatus());

        IPage<ApUserRealname> pageResult = this.page(pageReq, queryWrapper);


        //3.封装返回结果
        return new PageResponseResult(DTO.getPage(), DTO.getSize(), pageResult.getTotal(), pageResult.getRecords());
    }

    @Resource
    private ApUserMapper apUserMapper;

    @GlobalTransactional(rollbackFor = Exception.class,timeoutMills = 300000)
//    @Transactional(rollbackFor = Exception.class)
    @Override
    public ResponseResult updateStatusById(AuthDTO dto, Short status) {
        // 1.校验dto中id 参数 不能为空
        if (dto.getId() == null){
            CustException.cust(AppHttpCodeEnum.PARAM_INVALID,"实名认证id不能为空");
        }
        // 2. 根据id查询ap_user_realname表中的数据
        ApUserRealname userRealname = this.getById(dto.getId());

        // 3.判断实名认证状态是否为1待审核
        if (!AdminConstants.WAIT_AUTH.equals(userRealname.getStatus())){
            CustException.cust(AppHttpCodeEnum.PARAM_INVALID,"实名认证状态不正确");
        }
        // 根据实名认证信息关联的apUserId 查询apUser信息
        ApUser apUser = apUserMapper.selectById(userRealname.getUserId());
        if (apUser == null){
            CustException.cust(AppHttpCodeEnum.DATA_NOT_EXIST,"实名认证关联的用户不存在");
        }

        // 4.修改实名认证状态
        userRealname.setStatus(status);
        if (StringUtils.isNotBlank(dto.getMsg())){
            // 驳回原因
            userRealname.setReason(dto.getMsg());
        }
        // 执行修改
        updateById(userRealname);
        // 5.判断状态是否为9 审核成功 如果是2则直接返回成功
        if (status.equals(AdminConstants.FAIL_AUTH)){
            return ResponseResult.okResult();
        }
        // 6.如果为9 审核成功

        // 7. 开通自媒体账户 wm_user
        WmUser wmUser = createWmUser(apUser);
        // 8. 创建作者信息(查询是否已经创建) qp_author
        createApAuthor(apUser,wmUser);


        //演示分布式事务问题
        if (dto.getId() == 5){
            CustException.cust(AppHttpCodeEnum.PARAM_INVALID,"演示异常");
        }
        return ResponseResult.okResult();
    }

    @Resource
    ArticleFeign articleFeign;
    /**
     * 远程创建作者信息
     * @param apUser
     * @param wmUser
     */
    private void createApAuthor(ApUser apUser, WmUser wmUser) {
        //1. 根据用户id查询作者信息是否存在
        ResponseResult<ApAuthor> result = articleFeign.findByUserId(apUser.getId());
        if (!result.checkCode()){
            // 远程调用失败
            CustException.cust(AppHttpCodeEnum.REMOTE_SERVER_ERROR,result.getErrorMessage());
        }
        ApAuthor apAuthor = result.getData();


        //2.存在 抛出异常 提示已经存在
        if (apAuthor != null){
            CustException.cust(AppHttpCodeEnum.DATA_EXIST,"作者信息已经存在");
        }

        //3.不存在 远程调用创建作者信息
        apAuthor = new ApAuthor();
        apAuthor.setName(apUser.getName());
        apAuthor.setType(2);
        apAuthor.setUserId(apUser.getId());
        apAuthor.setCreatedTime(new Date());
        apAuthor.setWmUserId(wmUser.getId());
        ResponseResult saveResult = articleFeign.save(apAuthor);
        if (!saveResult.checkCode()){
            // 远程调用失败
            CustException.cust(AppHttpCodeEnum.REMOTE_SERVER_ERROR,result.getErrorMessage());
        }

    }

    @Resource
    private WemediaFeign wemediaFeign;
    /**
     * 开通自媒体账户
     * @param apUser
     * @return
     */
    private WmUser createWmUser(ApUser apUser) {
        //1.远程根据用户名查询自媒体用户信息
        ResponseResult<WmUser> result = wemediaFeign.findByName(apUser.getName());
//        result.checkCode() == true 远程调用成功 false 远程调用失败
        if (!result.checkCode()){
            // 远程调用失败
            CustException.cust(AppHttpCodeEnum.REMOTE_SERVER_ERROR,result.getErrorMessage());
        }
        //2.判断自媒体用户信息是否存在
        WmUser wmUser = result.getData();


        if (wmUser  != null){
            // 自媒体用户信息已经存在
            CustException.cust(AppHttpCodeEnum.DATA_EXIST,"自媒体用户信息已经存在");
        }

        //3.如果不存在，创建自媒体用户信息
        wmUser = new WmUser();
        wmUser.setApUserId(apUser.getId());
        wmUser.setName(apUser.getName());
        wmUser.setPassword(apUser.getPassword());
        wmUser.setSalt(apUser.getSalt());
        wmUser.setImage(apUser.getImage());
        wmUser.setPhone(apUser.getPhone());
        wmUser.setStatus(9);
        wmUser.setType(0);
        wmUser.setCreatedTime(new Date());
        ResponseResult<WmUser> saveResult = wemediaFeign.save(wmUser);

        // 查看远程调用是否成功
        if (!saveResult.checkCode()){
            CustException.cust(AppHttpCodeEnum.REMOTE_SERVER_ERROR,saveResult.getErrorMessage());
        }
        //4.返回自媒体用户信息
        return saveResult.getData();

    }
}
