package com.heima.article.service.impl;

import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.heima.article.mapper.ApArticleConfigMapper;
import com.heima.article.mapper.ApArticleContentMapper;
import com.heima.article.mapper.ApArticleMapper;
import com.heima.article.mapper.AuthorMapper;
import com.heima.article.service.ApArticleService;
import com.heima.article.service.GeneratePageService;
import com.heima.common.exception.CustException;
import com.heima.feigns.AdminFeign;
import com.heima.feigns.WemediaFeign;
import com.heima.model.admin.pojo.AdChannel;
import com.heima.model.article.dtos.ArticleHomeDTO;
import com.heima.model.article.pojos.ApArticle;
import com.heima.model.article.pojos.ApArticleConfig;
import com.heima.model.article.pojos.ApArticleContent;
import com.heima.model.article.pojos.ApAuthor;
import com.heima.model.common.constants.article.ArticleConstants;
import com.heima.model.common.constants.message.NewsUpOrDownConstants;
import com.heima.model.common.dtos.ResponseResult;
import com.heima.model.common.enums.AppHttpCodeEnum;
import com.heima.model.wemedia.pojos.WmNews;
import io.seata.spring.annotation.GlobalTransactional;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;


@Service
@Slf4j
public class ApArticleServiceImpl extends ServiceImpl<ApArticleMapper, ApArticle> implements ApArticleService {
    @Resource
    WemediaFeign wemediaFeign;

    @Resource
    AdminFeign adminFeign;

    @Resource
    AuthorMapper authorMapper;

    @Resource
    ApArticleConfigMapper apArticleConfigMapper;

    @Resource
    ApArticleContentMapper apArticleContentMapper;

    @Resource
    ApArticleMapper apArticleMapper;

    @Value("${file.oss.web-site}")
    String WebSite;

    @Value("${file.minio.readPath}")
    private String readPath;


    @Resource
    GeneratePageService generatePageService;

    @Resource
    RabbitTemplate rabbitTemplate;

    @Override
    @GlobalTransactional(rollbackFor = Exception.class,timeoutMills = 300000)
    public void publishArticle(Integer newsId) {
        // 1.根据id查询并校验自媒体文章
        WmNews wmNews = getWmNews(newsId);
        //拷贝属性
        // 2.根据WmNews封装ApArticle对象
        ApArticle apArticle = getApArticle(wmNews);
        // 3.保存或修改ApArticle
        saveOrUpdateArticle(apArticle);
        // 4.保存关联的配置信息 和 内容信息
        saveConfigAndContent(wmNews,apArticle);
        // 5.基于新的文章内容生成 html静态页
        generatePageService.generateArticlePage(wmNews.getContent(), apArticle);
        // 6.修改更新WmNews状态 9
        updateWmNews(wmNews,apArticle);
        // 7.TODO 通知ES更新索引库
        log.info("文章发布成功 并通知ES search微服务 更新索引库 ========{}",apArticle.getId());
        rabbitTemplate.convertAndSend(NewsUpOrDownConstants.NEWS_UP_FOR_ES_QUEUE,apArticle.getId());

    }

    @Override
    public ResponseResult load(Short loadtype, ArticleHomeDTO dto) {
        // 1.检查参数(分页 时间 类型 频道)
        Integer size = dto.getSize();
        if (size == null || size <= 0){
            dto.setSize(10);
        }
        if (dto.getMinBehotTime() == null){
            dto.setMinBehotTime(new Date());
        }
        if (dto.getMaxBehotTime() == null){
            dto.setMaxBehotTime(new Date());
        }
        if (StringUtils.isBlank(dto.getTag())){
            dto.setTag(ArticleConstants.DEFAULT_TAG);
        }
        if (!loadtype.equals(ArticleConstants.LOADTYPE_LOAD_MORE) && !loadtype.equals(ArticleConstants.LOADTYPE_LOAD_NEW)){
            loadtype = ArticleConstants.LOADTYPE_LOAD_MORE;
        }
//        2.调用mapper查询
        List<ApArticle> articleList = apArticleMapper.loadArticleList(dto, loadtype);
        // 3.返还结果 (封面需要拼接访问前缀)
        for (ApArticle apArticle : articleList) {
            parseArticle(apArticle);
        }
        return ResponseResult.okResult(articleList);
    }

    @Resource
    StringRedisTemplate stringRedisTemplate;
    @Override
    public ResponseResult load2(Short loadType, ArticleHomeDTO dto, boolean firstPage) {
        if (firstPage){
            // 从Redis缓存中 查询热点数据
            String articleListJson = stringRedisTemplate.opsForValue().get(ArticleConstants.HOT_ARTICLE_FIRST_PAGE + dto.getTag());
            if (StringUtils.isNotBlank(articleListJson)) {
                //转为集合
                List<ApArticle> articleList = JSON.parseArray(articleListJson, ApArticle.class);
                for (ApArticle article : articleList) {
                    parseArticle(article);
                }
                return ResponseResult.okResult(articleList);
            }
        }

        return load(loadType,dto);
    }


    public void parseArticle(ApArticle apArticle){
        String images = apArticle.getImages();
        if (StringUtils.isNotBlank(images)){
            images = Arrays.stream(images.split(",")).map(image -> WebSite + image).collect(Collectors.joining(","));

            apArticle.setImages(images);
        }
        apArticle.setStaticUrl(readPath + apArticle.getStaticUrl());

    }

    /**
     * 查询自媒体文章
     * @param newsId
     * @return
     */
    private WmNews getWmNews(Integer newsId) {
        ResponseResult<WmNews> newsResult = wemediaFeign.findWmNewsById(newsId);
        //检查远程调用状态
        if (!newsResult.checkCode()) {
            log.error("文章发布失败 远程调用自媒体文章接口失败  文章id: {}",newsId);
            CustException.cust(AppHttpCodeEnum.REMOTE_SERVER_ERROR,"远程调用自媒体文章接口失败");
        }
        // 判断自媒体文章是否存在
        WmNews wmNews = newsResult.getData();
        if(wmNews == null){
            log.error("文章发布失败 未获取到自媒体文章信息  文章id: {}",newsId);
            CustException.cust(AppHttpCodeEnum.DATA_NOT_EXIST,"未查询到自媒体文章");
        }
        // 判断状态是否为 4 或 8， 如果不是  不处理
        short status = wmNews.getStatus().shortValue();
        if(status!=WmNews.Status.ADMIN_SUCCESS.getCode() && status!=WmNews.Status.SUCCESS.getCode()){
            log.error("文章发布失败 文章状态不为 4 或 8， 不予发布 , 文章id : {}",newsId);
            CustException.cust(AppHttpCodeEnum.DATA_NOT_ALLOW,"自媒体文章状态错误");
        }
        return wmNews;
    }

    /**
     * 封装apArticle
     * @param wmNews
     * @return
     */
    private ApArticle getApArticle(WmNews wmNews) {
        ApArticle apArticle = new ApArticle();
        // 拷贝属性
        BeanUtils.copyProperties(wmNews,apArticle);
        //补全其他属性 id flag layout
        apArticle.setId(wmNews.getArticleId());
        apArticle.setFlag((byte)0);
        apArticle.setCreatedTime(new Date());
        apArticle.setLayout((short) 0);
        //补全频道channel信息 ad_channel
        ResponseResult<AdChannel> channelResponseResult = adminFeign.findOne(wmNews.getChannelId());
        if (!channelResponseResult.checkCode()){
            log.error("文章发布失败 远程调用查询频道出现异常， 不予发布 , 文章id : {}  频道id : {}",wmNews.getId(),wmNews.getChannelId());
            CustException.cust(AppHttpCodeEnum.REMOTE_SERVER_ERROR,"远程调用频道接口失败");
        }
        AdChannel channel = channelResponseResult.getData();
        if (channel == null){
            log.error("文章发布失败 未查询到相关频道信息， 不予发布 , 文章id : {}  频道id : {}",wmNews.getId(),wmNews.getChannelId());
            CustException.cust(AppHttpCodeEnum.ADMING_CHANNEL_NOT_EXIST,"频道不存在");
        }
        apArticle.setChannelName(channel.getName());
        //补全作者信息 wm_user_id 去ap_author查
        ApAuthor apAuthor = authorMapper.selectOne(Wrappers.<ApAuthor>lambdaQuery().eq(ApAuthor::getWmUserId, wmNews.getUserId()));
        if (apAuthor == null){
            log.error("文章发布失败 未查询到相关作者信息， 不予发布 , 文章id : {}  自媒体用户id : {}",wmNews.getId(),wmNews.getUserId());
            CustException.cust(AppHttpCodeEnum.DATA_NOT_EXIST,"作者信息不存在");
        }
        apArticle.setAuthorId(Long.valueOf(apAuthor.getId()));
        apArticle.setAuthorName(apAuthor.getName());
        return apArticle;
    }

    /**
     * 保存或修改文章信息
     * @param apArticle
     */
    private void saveOrUpdateArticle(ApArticle apArticle) {
        // 判断wmNews之前是否关联 articleId
        //1.1 如果不存在 保存
        if (apArticle.getId() == null) {
            apArticle.setLikes(0);
            apArticle.setComment(0);
            apArticle.setViews(0);
            apArticle.setCollection(0);
            save(apArticle);
        }else {
            //2 如果存在 修改文章
            // 2.1 查询之前的article是否存在
            ApArticle oldArticle = this.getById(apArticle.getId());
            if (oldArticle == null){
                CustException.cust(AppHttpCodeEnum.DATA_NOT_EXIST,"文章信息不存在");
            }
            // 修改
            updateById(apArticle);
            // 3.3 删除之前关联的articleConfig和Author信息
            apArticleConfigMapper.delete(Wrappers.<ApArticleConfig>lambdaQuery().eq(ApArticleConfig::getArticleId, apArticle.getId()));
            apArticleContentMapper.delete(Wrappers.<ApArticleContent>lambdaQuery().eq(ApArticleContent::getArticleId,apArticle.getId()));

        }
    }

    /**
     * 保存 配置 和 内容信息
     * @param wmNews
     * @param apArticle
     */
    private void saveConfigAndContent(WmNews wmNews, ApArticle apArticle) {
        //保存文章配置
        ApArticleConfig apArticleConfig = new ApArticleConfig();
        apArticleConfig.setArticleId(apArticle.getId());
        apArticleConfig.setIsComment(true);
        apArticleConfig.setIsForward(true);
        apArticleConfig.setIsDelete(false);
        apArticleConfig.setIsDown(false);
        apArticleConfigMapper.insert(apArticleConfig);

        //保存文章详情
        ApArticleContent apArticleContent = new ApArticleContent();
        apArticleContent.setArticleId(apArticle.getId());
        apArticleContent.setContent(wmNews.getContent());
        apArticleContentMapper.insert(apArticleContent);

    }

    /**
     * 修改自媒体文章
     * @param wmNews
     * @param apArticle
     */
    private void updateWmNews(WmNews wmNews, ApArticle apArticle) {
        //修改文章状态为发布 9
        wmNews.setStatus(WmNews.Status.PUBLISHED.getCode());
        wmNews.setArticleId(apArticle.getId());
        ResponseResult updateResult = wemediaFeign.updateWmNews(wmNews);
        if (!updateResult.checkCode()) {
            log.error("文章发布失败 远程调用修改文章接口失败， 不予发布 , 文章id : {} ",wmNews.getId());
            CustException.cust(AppHttpCodeEnum.REMOTE_SERVER_ERROR,"远程调用修改文章接口失败");
        }
    }


}