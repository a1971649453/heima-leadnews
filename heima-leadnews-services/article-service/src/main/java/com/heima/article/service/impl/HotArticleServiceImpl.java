package com.heima.article.service.impl;

import com.alibaba.fastjson.JSON;
import com.heima.article.mapper.ApArticleMapper;
import com.heima.article.service.HotArticleService;
import com.heima.common.exception.CustException;
import com.heima.feigns.AdminFeign;
import com.heima.model.admin.pojo.AdChannel;
import com.heima.model.article.pojos.ApArticle;
import com.heima.model.article.vos.HotArticleVo;
import com.heima.model.common.constants.article.ArticleConstants;
import com.heima.model.common.dtos.ResponseResult;
import com.heima.model.common.enums.AppHttpCodeEnum;
import com.heima.model.mess.app.AggBehaviorDTO;
import com.heima.utils.common.DateUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author 金宗文
 * @version 1.0
 */
@Service
@Slf4j
public class HotArticleServiceImpl implements HotArticleService {

    @Resource
    ApArticleMapper apArticleMapper;

    @Resource
    StringRedisTemplate stringRedisTemplate;

    @Override
    public void updateApArticle(AggBehaviorDTO aggBehavior) {
        //1.根据id查询文章数据
        ApArticle article = apArticleMapper.selectById(aggBehavior.getArticleId());
        if (article == null){
            CustException.cust(AppHttpCodeEnum.DATA_NOT_EXIST,"文章不存在");
        }
        // 2.聚合行为文章数据 和文章表中的统计数据 累加一起
        //更新阅读量
        Integer views = article.getViews() == null ? 0 : article.getViews();
        article.setViews((int) (views + aggBehavior.getView()));
        //更新点赞量
        Integer likes = article.getLikes() == null ? 0 : article.getLikes();
        article.setLikes((int) (likes + aggBehavior.getLike()));
        //更新评论量
        Integer comments = article.getComment() == null ? 0 : article.getComment();
        article.setComment((int) (comments + aggBehavior.getComment()));
        //更新收藏量
        Integer collections = article.getCollection() == null ? 0 : article.getCollection();
        article.setCollection((int) (collections + aggBehavior.getCollect()));
        apArticleMapper.updateById(article);
        //3.重新计算文章得分
        Integer score = computeScore(article);
        //4. 如果是今天发布的 热度*3
        //当前时间
        String now = DateUtils.dateToString(new Date());
        // 发布时间
        String publishTime = DateUtils.dateToString(article.getPublishTime());
        if (publishTime.equals(now)){
            score = score * 3;
        }
        //5. 更新当前文章所在的频道的缓存
        updateArticleCache(article, score, ArticleConstants.HOT_ARTICLE_FIRST_PAGE + article.getChannelId());
        //6   更新推荐列表的缓存
        updateArticleCache(article, score,  ArticleConstants.HOT_ARTICLE_FIRST_PAGE+ ArticleConstants.DEFAULT_TAG);

    }

    private void updateArticleCache(ApArticle article, Integer score, String cacheKey) {
        //1. 从redis中获取缓存的热点文章列表
        String hotArticleJson = stringRedisTemplate.opsForValue().get(cacheKey);
        // 1.2JSON字符串 转为对象
        List<HotArticleVo> hotArticleVoList = JSON.parseArray(hotArticleJson, HotArticleVo.class);
        boolean isHas = false;
        // 2.判断当前文章是否在缓存中
        for (HotArticleVo hotArticleVo : hotArticleVoList) {
            if (hotArticleVo.getId().equals(article.getId())){
                //当前文章存在于缓存热点文章列表中
                //3. 如果存在 直接更新文章的score数据
                hotArticleVo.setScore(score);
                isHas = true;
                break;
            }
        }
        //4. 如果不存在 直接将当前文章 加入到热点文章列表中
        if (!isHas){
            HotArticleVo articleVo = new HotArticleVo();
            BeanUtils.copyProperties(article,articleVo);
            articleVo.setScore(score);
            //加入到热点文章列表中
            hotArticleVoList.add(articleVo);
        }

        //5. 重新将热点文章列表 进行按照热度排序 截取前30条
        hotArticleVoList = hotArticleVoList.stream().sorted(Comparator.comparing(HotArticleVo::getScore).reversed())
                .limit(30)
                .collect(Collectors.toList());
        //6. 将文章列表重新存入redis中
        stringRedisTemplate.opsForValue().set(cacheKey, JSON.toJSONString(hotArticleVoList));
    }

    @Override
    public void computeHotArticle() {
        //1. 查询近5天的文章
        //1.1 计算5天前的时间
        String param = LocalDateTime.now().minusDays(15).format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        //1.2 使用articleMapper查询文章数据
        List<ApArticle> apArticles = apArticleMapper.selectArticleByDate(param);
        if (CollectionUtils.isEmpty(apArticles)){
            log.info("当前头条项目太冷清了,近5天没有人发布文章了");
            return;
        }
        //2. 计算每一篇文章热度得分
        List<HotArticleVo> articleVoList = getHotArticleVoList(apArticles);
        //3. 按照频道 每个频道缓存 热度最高的30条文章
        cacheToRedisByTag(articleVoList);

    }
    @Resource
    AdminFeign adminFeign;

    /**
     * 频道缓存热点较高的30条文章
     * @param articleVoList
     */
    private void cacheToRedisByTag(List<HotArticleVo> articleVoList) {
        //1. 查询频道列表
        ResponseResult<List<AdChannel>> result = adminFeign.selectChannels();
        if (!result.checkCode()){
            CustException.cust(AppHttpCodeEnum.REMOTE_SERVER_ERROR);
        }
        List<AdChannel> channelList = result.getData();

        //2. 遍历频道列表 从文章列表中筛选出对应频道的文章 调用保存方法 sortAndCache
        for (AdChannel adChannel : channelList) {
            List<HotArticleVo> hotArticlesByTag = articleVoList.stream()
                    .filter(hotArticleVo -> hotArticleVo.getChannelId().equals(adChannel.getId()))
                    .collect(Collectors.toList());
            //3. 按照 频道 每个频道进行缓存 对应的热度最高的30条文章
            sortAndCache(hotArticlesByTag,ArticleConstants.HOT_ARTICLE_FIRST_PAGE + adChannel.getId());
        }

        //3. 缓存推荐频道的文章 所有文章热度前30 调用保存方法
        sortAndCache(articleVoList,ArticleConstants.HOT_ARTICLE_FIRST_PAGE + ArticleConstants.DEFAULT_TAG);
    }



    /**
     * 缓存热点文章方法
     * @param hotArticleList 待缓存的热点文章
     * @param cacheKey redis中的Key
     */
    private void sortAndCache(List<HotArticleVo> hotArticleList, String cacheKey) {
        //1. 按照文章热度降序排序
        hotArticleList = hotArticleList.stream()
                .sorted(Comparator.comparing(HotArticleVo::getScore).reversed())
                .limit(30)
                .collect(Collectors.toList());
        //2. 使用redisTemplate缓存数据
        stringRedisTemplate.opsForValue().set(cacheKey, JSON.toJSONString(hotArticleList));
    }

    /**
     * 计算文章热点得分
     * @param apArticles
     * @return
     */
    private List<HotArticleVo> getHotArticleVoList(List<ApArticle> apArticles) {
        //1.
        List<HotArticleVo> articleVoList = apArticles.stream().map(apArticle -> {
            //将每一个文章计算得分 封装为VO对象
            HotArticleVo hotArticleVo = new HotArticleVo();
            BeanUtils.copyProperties(apArticle,hotArticleVo);
            //2. 计算文章热度得分
            Integer score = computeScore(apArticle);
            hotArticleVo.setScore(score);
            return hotArticleVo;
        }).collect(Collectors.toList());
        return articleVoList;
    }

    /**
     * 计算文章热度得分算法
     * @param apArticle
     * @return
     */
    private Integer computeScore(ApArticle apArticle) {
        int score = 0;
        Integer views = apArticle.getViews();
        if (views != null){
            score += views * ArticleConstants.HOT_ARTICLE_VIEW_WEIGHT;
        }
        Integer likes = apArticle.getLikes();
        if (likes != null){
            score += likes * ArticleConstants.HOT_ARTICLE_LIKE_WEIGHT;
        }
        Integer comments = apArticle.getComment();
        if (comments != null){
            score += comments * ArticleConstants.HOT_ARTICLE_COMMENT_WEIGHT;
        }
        Integer collections = apArticle.getCollection();
        if (collections != null){
            score += collections * ArticleConstants.HOT_ARTICLE_COLLECTION_WEIGHT;
        }
        return score;
    }
}
