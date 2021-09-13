package com.heima.user.service.impl;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.heima.common.constants.admin.AdminConstants;
import com.heima.common.exception.CustException;
import com.heima.common.exception.CustomException;
import com.heima.feigns.ArticleFeign;
import com.heima.feigns.WemediaFeign;
import com.heima.model.article.pojos.ApAuthor;
import com.heima.model.user.pojos.ApUser;
import com.heima.model.common.dtos.PageResponseResult;
import com.heima.model.common.dtos.ResponseResult;
import com.heima.model.common.enums.AppHttpCodeEnum;
import com.heima.model.user.dtos.AuthDTO;
import com.heima.model.user.pojos.ApUserRealname;
import com.heima.model.wemedia.pojos.WmUser;
import com.heima.user.mapper.ApUserMapper;
import com.heima.user.mapper.ApUserRealnameMapper;
import com.heima.user.service.ApUserRealnameService;
import io.seata.spring.annotation.GlobalTransactional;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;

@Service
@Slf4j
public class ApUserRealnameServiceImpl extends ServiceImpl<ApUserRealnameMapper, ApUserRealname> implements ApUserRealnameService {
    /**
     * 查询列表
     * @param dto
     * @return
     */
    @Override
    public ResponseResult loadListByStatus(AuthDTO dto) {
        // 1 参数检查
        if (dto == null) {
            throw new CustomException(AppHttpCodeEnum.PARAM_INVALID);
        }
        dto.checkParam();

        // 2 条件查询
        Page<ApUserRealname> page = new Page<>(dto.getPage(), dto.getSize());
        LambdaQueryWrapper<ApUserRealname> lambdaQueryWrapper = new LambdaQueryWrapper();

        if (dto.getStatus() != null) {
            lambdaQueryWrapper.eq(ApUserRealname::getStatus, dto.getStatus());
        }

        IPage<ApUserRealname> resultPage = page(page, lambdaQueryWrapper);

        // 3 返回结果
        return new PageResponseResult(dto.getPage(), dto.getSize(),
                resultPage.getTotal(), resultPage.getRecords());
    }

    @Autowired
    ApUserMapper apUserMapper;
    @Autowired
    WemediaFeign wemediaFeign;
    @Autowired
    ArticleFeign articleFeign;

    /**
     * 全局事务
     * @param dto
     * @param status  2 审核失败   9 审核成功
     * @return
     */
    @GlobalTransactional(rollbackFor = Exception.class)
//    @Transactional(rollbackFor = Exception.class)
    @Override
    public ResponseResult updateStatusById(AuthDTO dto, Short status) {
        //1 参数检查
        if (dto == null || dto.getId()==null) {
            throw new CustomException(AppHttpCodeEnum.PARAM_INVALID);
        }
        //2 查询当前认证用户是否在 APP端有当前用户
        ApUserRealname apUserRealname = getOne(Wrappers.<ApUserRealname>lambdaQuery()
                .eq(ApUserRealname::getId,dto.getId())
                .eq(ApUserRealname::getStatus,1)  // 审核未通过的用户
        );
        if (apUserRealname == null) {
            log.error("apUserRealname is Null userRealnameId:{}", dto.getId());
            throw new CustomException(AppHttpCodeEnum.DATA_NOT_EXIST);
        }
        ApUser apUser = apUserMapper.selectOne(Wrappers.<ApUser>lambdaQuery()
                .eq(ApUser::getId, apUserRealname.getUserId()));
        if(apUser == null){
            log.error("apUserRealname apUser is Null userRealnameId:{}, userId:{} ", dto.getId(), apUserRealname.getUserId());
            throw new CustomException(AppHttpCodeEnum.DATA_NOT_EXIST);
        }
        //3 更新认证用户信息
        apUserRealname.setId(dto.getId());
        apUserRealname.setStatus(status);
        apUserRealname.setUpdatedTime(new Date());
        if(StringUtils.isNotBlank(dto.getMsg())){
            apUserRealname.setReason(dto.getMsg());
        }
        updateById(apUserRealname);
        //4 认证状态如果为 通过
        if (AdminConstants.PASS_AUTH.equals(status)) {
            //4.1 创建自媒体账户
            WmUser wmUser = createWmUser(dto,apUser);
            //4.2 创建作者信息
            createApAuthor(wmUser);
        }
        //5 更新APP用户状态  是否认证通过
        apUser.setFlag((short) 1);
        apUserMapper.updateById(apUser);

        if (dto.getId().intValue() == 5){
            CustException.cust(AppHttpCodeEnum.DATA_NOT_ALLOW,"演示异常，id==5时报错");
        }
        //4 返回结果
        return ResponseResult.okResult(AppHttpCodeEnum.SUCCESS);
    }

    /**
     * 4.2 创建作者信息
     * @param wmUser
     * @return
     */
    private void createApAuthor(WmUser wmUser) {
        //1 检查是否成功调用
        ResponseResult<ApAuthor> apAuthorResult = articleFeign.findByUserId(wmUser.getApUserId());
        if(apAuthorResult.getCode().intValue() != 0){
            CustException.cust(AppHttpCodeEnum.SERVER_ERROR,apAuthorResult.getErrorMessage());
        }
        //2. 检查作者信息是否已经存在
        ApAuthor apAuthor = apAuthorResult.getData();
        if (apAuthor != null) {
            CustException.cust(AppHttpCodeEnum.DATA_EXIST,"作者信息已存在");
        }
        //3. 添加作者信息
        apAuthor = new ApAuthor();
        apAuthor.setCreatedTime(new Date());
        apAuthor.setName(wmUser.getName());
        apAuthor.setType(2); // 自媒体人类型
        apAuthor.setUserId(wmUser.getApUserId()); // APP 用户ID
        apAuthor.setWmUserId(wmUser.getId()); // 自媒体用户ID
        ResponseResult result = articleFeign.save(apAuthor);
        //4. 结果失败，抛出异常
        if (result.getCode() != 0) {
            CustException.cust(AppHttpCodeEnum.SERVER_ERROR,result.getErrorMessage());
        }
    }
    /**
     * 4.1 创建自媒体账户
     * @param dto
     * @param apUser  APP端用户
     * @return
     */
    private WmUser createWmUser(AuthDTO dto, ApUser apUser) {
        //1 查询自媒体账号是否存在（APP端用户密码和自媒体密码一致）
        ResponseResult<WmUser> wmUserResult = wemediaFeign.findByName(apUser.getName());
        if(wmUserResult.getCode().intValue() != 0){
            CustException.cust(AppHttpCodeEnum.SERVER_ERROR,wmUserResult.getErrorMessage());
        }
        WmUser wmUser =wmUserResult.getData();
        if (wmUser != null) {
            CustException.cust(AppHttpCodeEnum.DATA_EXIST,"自媒体用户信息已存在");
        }
        wmUser = new WmUser();
        wmUser.setName(apUser.getName());
        wmUser.setSalt(apUser.getSalt());  // 盐
        wmUser.setPassword(apUser.getPassword()); // 密码
        wmUser.setPhone(apUser.getPhone());
        wmUser.setCreatedTime(new Date());
        wmUser.setType(0); // 个人
        wmUser.setApUserId(apUser.getId());  // app端用户id
        wmUser.setStatus(AdminConstants.PASS_AUTH.intValue());

        ResponseResult<WmUser> saveResult = wemediaFeign.save(wmUser);
        if(saveResult.getCode().intValue()!=0){
            CustException.cust(AppHttpCodeEnum.SERVER_ERROR,saveResult.getErrorMessage());
        }
        return saveResult.getData();
    }
}