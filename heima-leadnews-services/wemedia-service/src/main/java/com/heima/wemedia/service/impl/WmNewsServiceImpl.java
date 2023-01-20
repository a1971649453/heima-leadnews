package com.heima.wemedia.service.impl;

import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.heima.common.exception.CustException;
import com.heima.model.common.constants.wemedia.WemediaConstants;
import com.heima.model.common.dtos.PageResponseResult;
import com.heima.model.common.dtos.ResponseResult;
import com.heima.model.common.enums.AppHttpCodeEnum;
import com.heima.model.threadlocal.WmThreadLocalUtils;
import com.heima.model.wemedia.dtos.WmNewsDTO;
import com.heima.model.wemedia.dtos.WmNewsPageReqDTO;
import com.heima.model.wemedia.pojos.WmNews;
import com.heima.model.wemedia.pojos.WmNewsMaterial;
import com.heima.model.wemedia.pojos.WmUser;
import com.heima.wemedia.mapper.WmMaterialMapper;
import com.heima.wemedia.mapper.WmNewsMapper;
import com.heima.wemedia.mapper.WmNewsMaterialMapper;
import com.heima.wemedia.service.WmNewsService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import javax.annotation.Resource;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author 金宗文
 * @version 1.0
 */
@Service
public class WmNewsServiceImpl extends ServiceImpl<WmNewsMapper, WmNews> implements WmNewsService {

    @Value("${file.oss.web-site}")
    String webSite;

    @Override
    public ResponseResult findList(WmNewsPageReqDTO dto) {
        //1.参数检验
        if (dto == null){
            CustException.cust(AppHttpCodeEnum.PARAM_INVALID);
        }
        dto.checkParam();
        //2.如果有文章标题，按照文章标题模糊查询
        LambdaQueryWrapper<WmNews> lambdaQueryWrapper = Wrappers.<WmNews>lambdaQuery();

        lambdaQueryWrapper.like(dto.getKeyword() != null,WmNews::getTitle, dto.getKeyword());

        //3.如果有频道信息，按照频道ID查询
        lambdaQueryWrapper.eq(dto.getChannelId() != null,WmNews::getChannelId, dto.getChannelId());

        //4.如果有文章状态，按照状态信息进行查询
        lambdaQueryWrapper.eq(dto.getStatus() != null,WmNews::getStatus, dto.getStatus());

        //5.如果开始时间，结束时间不为空按照时间区间查询
        lambdaQueryWrapper.ge(dto.getBeginPubDate() != null,WmNews::getPublishTime, dto.getBeginPubDate());
        lambdaQueryWrapper.le(dto.getEndPubDate()!=null,WmNews::getPublishTime, dto.getBeginPubDate());
        //6.按照登录用户ID去查询
        //获取当前登录用户ID
        WmUser user = WmThreadLocalUtils.getUser();
        if (user == null){
            CustException.cust(AppHttpCodeEnum.NEED_LOGIN);
        }
        lambdaQueryWrapper.eq(WmNews::getUserId, user.getId());
        //7.按照创建时间降序
        lambdaQueryWrapper.orderByDesc(WmNews::getCreatedTime);
        //8.分页查询，返回结果设置host  地址 为图片访问前缀
        Page<WmNews> wmNewsPage = new Page<>(dto.getPage(), dto.getSize());
        IPage<WmNews> pageResult = this.page(wmNewsPage, lambdaQueryWrapper);
        //4 返回封装查询结果
        PageResponseResult result = new PageResponseResult(dto.getPage(), dto.getSize(), pageResult.getTotal());
        result.setData(pageResult.getRecords());
        // 处理文章图片 设置host为webSite前缀
        result.setHost(webSite);


        return result;
    }

    @Override
    public ResponseResult submitNews(WmNewsDTO dto) {
        //1.参数校验
        if (StringUtils.isBlank(dto.getContent())) {
            CustException.cust(AppHttpCodeEnum.PARAM_INVALID);
        }
        //2.校验是否登录
        WmUser user = WmThreadLocalUtils.getUser();
        if (user == null){
            CustException.cust(AppHttpCodeEnum.NEED_LOGIN);
        }
        //3.保存
        //文章布局是否为自动
        WmNews wmNews = new WmNews();
        BeanUtils.copyProperties(dto,wmNews);

        if (dto.getType().equals(WemediaConstants.WM_NEWS_TYPE_AUTO)){
            wmNews.setType(null);
        }
        String images = imageListToStr(dto.getImages());
        wmNews.setImages(images);
        wmNews.setUserId(user.getId());
        saveWmNews(wmNews);
        // 如果是草稿,直接反回
        if (dto.getStatus().equals(WemediaConstants.WM_NEWS_DRAFT_STATUS)){
            return ResponseResult.okResult(AppHttpCodeEnum.SUCCESS);
        }
        // TODO 3.1 抽取文章中关联的图片路径
        List<String> materials = parseContentImages(dto.getContent());
        // TODO 3.2 关联文章内容中的图片和素材关系
        if (!CollectionUtils.isEmpty(materials)) {
            saveRelativeInfo(materials, wmNews.getId(),WemediaConstants.WM_CONTENT_REFERENCE);
        }
        // TODO 3.3 关联文章封面中的图片和素材关系  封面可能是选择自动或者是无图
        saveRelativeInfoForCover(dto,materials, wmNews);
        return ResponseResult.okResult();
    }

    @Override
    public ResponseResult findWmNewsById(Integer id) {
        //1 参数检查
        if (id == null) {
            CustException.cust(AppHttpCodeEnum.PARAM_INVALID);
        }
        //2 执行查询
        WmNews wmNews = getById(id);
        if (wmNews == null) {
            CustException.cust(AppHttpCodeEnum.DATA_NOT_EXIST);
        }
        //3 返回结果
        ResponseResult result = ResponseResult.okResult(wmNews);
        result.setHost(webSite);
        return result;
    }

    @Override
    public ResponseResult delNews(Integer id) {
        //1.校验参数
        if (id == null) {
            CustException.cust(AppHttpCodeEnum.PARAM_INVALID);
        }
        //2.判断是否登录
        WmUser user = WmThreadLocalUtils.getUser();
        if (user == null){
            CustException.cust(AppHttpCodeEnum.NEED_LOGIN);
        }
        //3.当文章状态为9(已发布)且已上架则不能删除文章，下架状态可以删除，如果是其他状态可以删除
        WmNews wmNews = this.getById(id);
        if (wmNews == null) {
            CustException.cust(AppHttpCodeEnum.DATA_NOT_EXIST,"文章不存在");
        }
        Short status = wmNews.getStatus();
        if (status.equals(WmNews.Status.PUBLISHED.getCode()) && wmNews.getEnable().equals(WemediaConstants.WM_NEWS_UP)){
            CustException.cust(AppHttpCodeEnum.DATA_EXIST,"文章已上架，不能删除");
        }

        //4.删除文章之前需要先把素材与文章的关系删除掉
        wmNewsMaterialMapper.delete(Wrappers.<WmNewsMaterial>lambdaQuery().eq(WmNewsMaterial::getNewsId,wmNews.getId()));
        //5.删除
        this.removeById(id);
        return ResponseResult.okResult();
    }

    @Override
    public ResponseResult downOrUp(WmNewsDTO dto) {
        //1.检查参数
        if(dto == null || dto.getId() == null){
            CustException.cust(AppHttpCodeEnum.PARAM_INVALID);
        }
        Short enable = dto.getEnable();
        if(enable == null ||
                (!WemediaConstants.WM_NEWS_UP.equals(enable)&&!WemediaConstants.WM_NEWS_DOWN.equals(enable))){
            CustException.cust(AppHttpCodeEnum.PARAM_INVALID,"上下架状态错误");
        }
        //2.查询文章
        WmNews wmNews = getById(dto.getId());
        if(wmNews == null){
            CustException.cust(AppHttpCodeEnum.DATA_NOT_EXIST,"文章不存在");
        }
        //3.判断文章是否发布
        if(!wmNews.getStatus().equals(WmNews.Status.PUBLISHED.getCode())){
            CustException.cust(AppHttpCodeEnum.DATA_NOT_EXIST,"当前文章不是发布状态，不能上下架");
        }
        //4.修改文章状态，同步到app端（后期做）TODO
        update(Wrappers.<WmNews>lambdaUpdate().eq(WmNews::getId,dto.getId())
                .set(WmNews::getEnable,dto.getEnable()));
        return ResponseResult.okResult(AppHttpCodeEnum.SUCCESS);
    }

    /**
     * 【3.3】 关联文章封面中的图片和素材关系
     * @param dto  前端用户选择封面信息数据
     * @param materials  从内容中解析的图片列表
     * @param wmNews     文章ID
     */
    private void saveRelativeInfoForCover(WmNewsDTO dto, List<String> materials, WmNews wmNews) {
        // 前端用户选择的图
        List<String> images = dto.getImages();
        // 自动获取封面 ****
        if (WemediaConstants.WM_NEWS_TYPE_AUTO.equals(dto.getType())) {
            int materialSize = materials.size();
            if (materialSize > 0 && materialSize <= 2) {  // 单图
                images =  materials.stream().limit(1).collect(Collectors.toList());
                wmNews.setType(WemediaConstants.WM_NEWS_SINGLE_IMAGE);
            } else if (materialSize > 2) { // 多图
                images =  materials.stream().limit(3).collect(Collectors.toList());
                wmNews.setType(WemediaConstants.WM_NEWS_MANY_IMAGE);
            } else {  // 无图
                wmNews.setType(WemediaConstants.WM_NEWS_NONE_IMAGE);
            }
            if (images != null && images.size() > 0) {
                // 将图片集合 转为字符串  url1,url2,url3
                wmNews.setImages(imageListToStr(images));
            }
            updateById(wmNews);
        }
        // 保存图片列表和素材的关系
        if (images != null && images.size() > 0) {
            images = images.stream().map(x->x.replace(webSite,"")
                    .replace(" ","")).collect(Collectors.toList());
            saveRelativeInfo(images, wmNews.getId(), WemediaConstants.WM_IMAGE_REFERENCE);
        }
    }
    @Resource
    WmMaterialMapper wmMaterialMapper;
    /**
     * 保存素材和文章关系
     * @param urls  素材列表
     * @param newsId     文章ID
     * @param type       类型 0：内容素材  1：封面素材
     */
    private void saveRelativeInfo(List<String> urls, Integer newsId, Short type) {
        //1 查询文章内容中的图片对应的素材ID
        List<Integer> ids = wmMaterialMapper.selectRelationsIds(urls,
                WmThreadLocalUtils.getUser().getId());
        //2 判断素材是否缺失
        if(CollectionUtils.isEmpty(ids) || ids.size() < urls.size()){
            CustException.cust(AppHttpCodeEnum.DATA_NOT_EXIST,"相关素材缺失,保存文章失败");
        }
        //3 保存素材关系
        wmNewsMaterialMapper.saveRelations(ids,newsId,type);
    }

    /**
     * 图片列表转字符串，并去除图片前缀
     * @param images 图片列表
     */
    private String imageListToStr(List<String> images) {
       return images.stream().map(image -> image.replace(webSite,"")).collect(Collectors.joining(","));
    }

    @Resource
    WmNewsMaterialMapper wmNewsMaterialMapper;
    /**
     * 保存或修改文章
     * @param wmNews 文章对象（前端传递）
     */
    private void saveWmNews(WmNews wmNews) {
        wmNews.setCreatedTime(new Date());
        wmNews.setUserId(WmThreadLocalUtils.getUser().getId());
        wmNews.setSubmitedTime(new Date());
        wmNews.setEnable(WemediaConstants.WM_NEWS_UP); // 上架
        if (wmNews.getId()==null) { // 保存操作
            save(wmNews);
        }else {  // 修改
            // 当前文章 和 素材关系表数据删除
            wmNewsMaterialMapper.delete(Wrappers.<WmNewsMaterial>lambdaQuery()
                    .eq(WmNewsMaterial::getNewsId, wmNews.getId()));
            updateById(wmNews);
        }
    }

    /**
     * 抽取文章内容中 所引用的所有图片
     * @param content 文章内容
     * @return
     */
    private List<String> parseContentImages(String content) {
        List<Map> contents = JSON.parseArray(content, Map.class);
        // 遍历文章内容   将所有 type为image的 value获取出来  去除前缀路径
        return contents.stream()
                // 过滤type=image所有的集合
                .filter( map -> map.get("type").equals(WemediaConstants.WM_NEWS_TYPE_IMAGE))
                // 获取到image下的value  图片url
                .map(x -> (String)x.get("value"))
                // 图片url去除前缀
                .map(url-> url.replace(webSite,"").replace(" ",""))
                // 去除重复的路径
                .distinct()
                // stream 转成list集合
                .collect(Collectors.toList());
    }
}
