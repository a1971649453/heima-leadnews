package com.heima.wemedia.service.impl;
import java.util.Date;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.heima.common.exception.CustException;
import com.heima.file.service.FileStorageService;
import com.heima.model.common.constants.wemedia.WemediaConstants;
import com.heima.model.common.dtos.PageResponseResult;
import com.heima.model.common.dtos.ResponseResult;
import com.heima.model.common.enums.AppHttpCodeEnum;
import com.heima.model.threadlocal.WmThreadLocalUtils;
import com.heima.model.wemedia.dtos.WmMaterialDTO;
import com.heima.model.wemedia.pojos.WmMaterial;
import com.heima.model.wemedia.pojos.WmNewsMaterial;
import com.heima.model.wemedia.pojos.WmUser;
import com.heima.wemedia.mapper.WmMaterialMapper;
import com.heima.wemedia.mapper.WmNewsMaterialMapper;
import com.heima.wemedia.service.WmMaterialService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/**
 * @author 金宗文
 * @version 1.0
 */
@Service
public class WmMaterialServiceImpl extends ServiceImpl<WmMaterialMapper, WmMaterial> implements WmMaterialService {
    @Resource
    FileStorageService fileStorageService;

    @Value("${file.oss.prefix}")
    String prefix;

    @Value("${file.oss.web-site}")
    String webSite;

    @Resource
    WmNewsMaterialMapper wmNewsMaterialMapper;

    @Override
    public ResponseResult uploadPicture(MultipartFile multipartFile) {
        //1.参数校验(文件对象, 判断是否登录, 文件后缀是否支持)
        if (multipartFile == null || multipartFile.isEmpty()) {
            return ResponseResult.errorResult(AppHttpCodeEnum.PARAM_INVALID,"请上传正确文件");
        }
        WmUser user = WmThreadLocalUtils.getUser();
        if (user == null) {
            return ResponseResult.errorResult(AppHttpCodeEnum.NEED_LOGIN);
        }
        // 获得原始文件名称
        String originalFilename = multipartFile.getOriginalFilename();
        if (!checkFileSuffix(originalFilename)){
            CustException.cust(AppHttpCodeEnum.PARAM_INVALID,"文件类型不支持 目前支持jpg,jpeg,png,gif");
        }
        //2.上传到oss 生成新的文件名称,上传到oss
        String  newFileName = null;
        try {
            String filename = UUID.randomUUID().toString().replace("-", "");
            String suffix = originalFilename.substring(originalFilename.lastIndexOf("."));
            newFileName = fileStorageService.store(prefix, filename+suffix, multipartFile.getInputStream());
        } catch (IOException e) {
            e.printStackTrace();
            CustException.cust(AppHttpCodeEnum.SERVER_ERROR,"上传文件到oss失败");

        }
        //3.保存到数据库
        WmMaterial wmMaterial = new WmMaterial();
        wmMaterial.setUserId(user.getId());
        wmMaterial.setUrl(newFileName);
        wmMaterial.setType((short)0);
        wmMaterial.setIsCollection((short)0);
        wmMaterial.setCreatedTime(new Date());

        this.save(wmMaterial);
        //4.封装返回 需要将url路径补全
        wmMaterial.setUrl(webSite + wmMaterial.getUrl());
        return ResponseResult.okResult(wmMaterial);
    }

    @Override
    public PageResponseResult findList(WmMaterialDTO dto) {
        // 1.参数校验
        dto.checkParam();
        WmUser user = WmThreadLocalUtils.getUser();
        if (user == null){
            CustException.cust(AppHttpCodeEnum.NEED_LOGIN);
        }
        // 2.分页查询 分页条件,是否收藏,用户id,发布时间降序排序
        LambdaQueryWrapper<WmMaterial> lambdaQueryWrapper = new LambdaQueryWrapper<>();
        Page<WmMaterial> page = new Page<>(dto.getPage(),dto.getSize());
        if (WemediaConstants.COLLECT_MATERIAL.equals(dto.getIsCollection())){
            lambdaQueryWrapper.eq(WmMaterial::getIsCollection,dto.getIsCollection());
        }
        lambdaQueryWrapper.eq(WmMaterial::getUserId,user.getId());
        lambdaQueryWrapper.orderByDesc(WmMaterial::getCreatedTime);

        //执行查询
        IPage<WmMaterial> result = this.page(page, lambdaQueryWrapper);
        List<WmMaterial> records = result.getRecords();
       // 将url补全
        for (WmMaterial record : records) {
            record.setUrl(webSite + record.getUrl());
            System.out.println( record.getUrl());
        }

        // 3.数据返回
        PageResponseResult pageResponseResult = new PageResponseResult(dto.getPage(), dto.getSize(), result.getTotal());
        pageResponseResult.setData(records);
        return pageResponseResult;
    }

    @Override
    public ResponseResult delPicture(Integer id) {
        // 1.参数校验
        if (id == null){
            CustException.cust(AppHttpCodeEnum.PARAM_REQUIRE,"素材id不能为空");
        }
        WmMaterial wmMaterial = this.getById(id);
        if (wmMaterial == null){
            CustException.cust(AppHttpCodeEnum.DATA_NOT_EXIST,"此素材不存在");
        }
        // 2.在关联wm_news_material查询是否存在此id相关的文章
        LambdaQueryWrapper<WmNewsMaterial> lambdaQueryWrapper = Wrappers.<WmNewsMaterial>lambdaQuery();
        lambdaQueryWrapper.eq(WmNewsMaterial::getMaterialId,id);
        Integer count = wmNewsMaterialMapper.selectCount(lambdaQueryWrapper);
        // 3.如果存在,则不允许删除
        if (count > 0){
            CustException.cust(AppHttpCodeEnum.DATA_EXIST,"此素材已经被文章使用,不能删除");
        }
        // 4.删除oss中的文件

        // 获取素材url
        String url = wmMaterial.getUrl();
        fileStorageService.delete(url);

        // 5.删除数据库中的数据
        this.removeById(id);

        return ResponseResult.okResult(AppHttpCodeEnum.SUCCESS);
    }

    /**
     * 收藏/取消收藏
     * @param id
     * @param type
     * @return
     */
    @Override
    public ResponseResult updateStatus(Integer id, Short type) {
        //1.参数检验
        if (id == null){
            CustException.cust(AppHttpCodeEnum.PARAM_INVALID,"素材id不能为空");
        }
        // 2.查询数据库中是否存在此id的素材
        WmMaterial wmMaterial = this.getById(id);
       if (wmMaterial == null) {
           CustException.cust(AppHttpCodeEnum.DATA_NOT_EXIST, "此素材不存在");
       }
       // 查看此素材是否属于当前用户
        WmUser user = WmThreadLocalUtils.getUser();
       if (user.getId().intValue() != wmMaterial.getUserId().intValue()) {
           CustException.cust(AppHttpCodeEnum.DATA_NOT_ALLOW, "此素材不属于当前用户");
       }




        // 3.修改收藏状态
        wmMaterial.setIsCollection(type);
        this.updateById(wmMaterial);
        return ResponseResult.okResult();
    }

    private boolean checkFileSuffix(String originalFilename) {
        if (StringUtils.isNotBlank(originalFilename)){
            List<String> allowSuffix = Arrays.asList(".jpg", ".png", ".jepg", ".gif");
            for (String suffix : allowSuffix) {
                if (originalFilename.endsWith(suffix)){
                    return true;
                }
            }
        }
        return false;
    }
}
