package com.heima.article.job;
import com.alibaba.fastjson.JSON;
import com.heima.article.mapper.ApArticleMapper;
import com.heima.article.service.HotArticleService;
import com.heima.common.exception.CustException;
import com.heima.model.article.pojos.ApArticle;
import com.heima.model.common.constants.article.HotArticleConstants;
import com.heima.model.common.enums.AppHttpCodeEnum;
import com.heima.model.mess.app.AggBehaviorDTO;
import com.heima.model.mess.app.NewBehaviorDTO;
import com.xxl.job.core.biz.model.ReturnT;
import com.xxl.job.core.handler.annotation.XxlJob;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.scripting.support.ResourceScriptSource;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import javax.annotation.Resource;
import java.util.*;
import java.util.function.BinaryOperator;
import java.util.stream.Collectors;

/**
 * @author 寂笙
 */
@Component
@Slf4j
public class UpdateHotArticleJob {

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Resource
    HotArticleService hotArticleService;

    @XxlJob("updateHotArticleJob")
    public ReturnT updateHotArticleHandler(String params){
        log.info("热文章分值更新 调度任务开始执行....");
        //1.获取最近10s的Redis List中产生的新行为数据
        List<NewBehaviorDTO> newBehaviorDTOList = getRedisBehaviorList();
        if (CollectionUtils.isEmpty(newBehaviorDTOList)){
            log.info(" 项目太冷清了,最近10s中没有人访问我们的项目");
            return ReturnT.SUCCESS;
        }
        //2. 分组聚合统计行为数据
        List<AggBehaviorDTO> aggBehaviorDTOList = getAggBehaviorList(newBehaviorDTOList);
        //3. 更新数据库中文章热度

        // 3. 定时更新文章热度
//        for (AggBehaviorDTO aggBehaviorDTO : aggBehaviorDTOList) {
//            hotArticleService.updateApArticle(aggBehaviorDTO);
//        }
        aggBehaviorDTOList.forEach(hotArticleService::updateApArticle);
        log.info("热文章分值更新 调度任务完成....");
        return ReturnT.SUCCESS;

    }



    /**
     *
     * @return
     */
    private List<AggBehaviorDTO> getAggBehaviorList(List<NewBehaviorDTO> newBehaviorDTOList) {
        //1. 分组 将新行为数据按照文章ID进行分组
        List<AggBehaviorDTO> aggBehaviorList = new ArrayList<AggBehaviorDTO>();
        Map<Long,List<NewBehaviorDTO>> behaviorGroup = newBehaviorDTOList.stream().collect(Collectors.groupingBy(NewBehaviorDTO::getArticleId));
        //2. 统计每个分组的数据 计算成一个AggBehavior数据
        behaviorGroup.forEach((articleId,behaviorList)->{

            // 将每个分组的行为数据 合并为一个aggBehavior
            Optional<AggBehaviorDTO> reduce = behaviorList.stream().map(newBehavior -> {
                AggBehaviorDTO aggBehaviorDTO = new AggBehaviorDTO();
                aggBehaviorDTO.setArticleId(newBehavior.getArticleId());
                switch (newBehavior.getType()) {
                    case LIKES:
                        aggBehaviorDTO.setLike(newBehavior.getAdd());
                        break;
                    case VIEWS:
                        aggBehaviorDTO.setView(newBehavior.getAdd());
                        break;
                    case COMMENT:
                        aggBehaviorDTO.setComment(newBehavior.getAdd());
                        break;
                    case COLLECTION:
                        aggBehaviorDTO.setCollect(newBehavior.getAdd());
                        break;
                    default:

                }
                return aggBehaviorDTO;
            }).reduce((a1, a2) -> {

                a1.setView(a1.getView() + a2.getView());
                a1.setLike(a1.getLike() + a2.getLike());
                a1.setComment(a1.getComment() + a2.getComment());
                a1.setCollect(a1.getCollect() + a2.getCollect());
                return a1;

            });
            if (reduce.isPresent()){
                // 聚合结果
                AggBehaviorDTO aggBehaviorDTO = reduce.get();
                log.info("热点文章聚合计算结果 ===>{}",aggBehaviorDTO);
                aggBehaviorList.add(aggBehaviorDTO);
            }
        });

        return aggBehaviorList;
    }

    /**
     * 获取最近10s的Redis List中产生的新行为数据
     * @return
     */
    private List<NewBehaviorDTO> getRedisBehaviorList() {
        //1. 创建Redis脚本对象
        DefaultRedisScript<List> redisScript = new DefaultRedisScript<>();
        //2. 设置脚本对象(返回类型 脚本地址)
        try {
            redisScript.setResultType(List.class);
            redisScript.setScriptSource(new ResourceScriptSource(new ClassPathResource("redis.lua")));

            //3. redisTemplate 执行脚本
            List<String> result = stringRedisTemplate.execute(redisScript, Arrays.asList(HotArticleConstants.HOT_ARTICLE_SCORE_BEHAVIOR_LIST));

            //4. 封装返回结果
            return result.stream().map(jsonStr -> JSON.parseObject(jsonStr,NewBehaviorDTO.class))
                    .collect(Collectors.toList());
        } catch (Exception e) {
            e.printStackTrace();
            log.error("执行lua脚本异常,原因:{}",e.getMessage());
            CustException.cust(AppHttpCodeEnum.SERVER_ERROR,"执行lua脚本出现异常");
            return null;

        }

    }
}