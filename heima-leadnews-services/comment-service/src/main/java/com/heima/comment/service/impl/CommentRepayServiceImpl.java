package com.heima.comment.service.impl;
import java.math.BigDecimal;

import com.heima.aliyun.scan.GreenScan;
import com.heima.aliyun.scan.ScanResult;
import com.heima.comment.service.CommentRepayService;
import com.heima.common.exception.CustException;
import com.heima.feigns.ArticleFeign;
import com.heima.feigns.UserFeign;
import com.heima.model.article.pojos.ApArticle;
import com.heima.model.comment.dtos.CommentRepayDTO;
import com.heima.model.comment.dtos.CommentRepayLikeDTO;
import com.heima.model.comment.dtos.CommentRepaySaveDTO;
import com.heima.model.comment.pojos.ApComment;
import com.heima.model.comment.pojos.ApCommentLike;
import com.heima.model.comment.pojos.ApCommentRepay;
import com.heima.model.comment.pojos.ApCommentRepayLike;
import com.heima.model.comment.vo.ApCommentRepayVO;
import com.heima.model.common.dtos.ResponseResult;
import com.heima.model.common.enums.AppHttpCodeEnum;
import com.heima.model.threadlocal.AppThreadLocalUtils;
import com.heima.model.user.pojos.ApUser;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author 金宗文
 * @version 1.0
 */
@Service
@Slf4j
public class CommentRepayServiceImpl implements CommentRepayService {
    @Resource
    private UserFeign userFeign;

    @Resource
    private GreenScan greenScan;

    @Resource
    private MongoTemplate mongoTemplate;


    @Resource
    private ArticleFeign articleFeign;

    @Override
    public ResponseResult loadCommentRepay(CommentRepayDTO dto) {
        //1.校验参数
        // 评论id不能为空  size为null 或 0 设置为10  最小日期为空 设置为当前
        Integer size = dto.getSize();
        if (size == null || size == 0){
            size = 10;
        }
        dto.setSize(size);
        if (dto.getMinDate() == null){
            dto.setMinDate(new Date());
        }
        //2.条件查询评论回复信息
        //	条件:  commentId = 评论id and createdTime 小于 minDate  创建时间 降序  limit size
        Query query = Query.query(Criteria.where("commentId").is(dto.getCommentId()).and("createdTime").lt(dto.getMinDate()))
                .with(Sort.by(Sort.Order.desc("createdTime")))
                .limit(size);
        List<ApCommentRepay> apCommentRepays = mongoTemplate.find(query, ApCommentRepay.class);
        //3.检查是否登录
        //3.1 如果未登录直接 返回评论回复列表
        ApUser user = AppThreadLocalUtils.getUser();
        if (user == null){
            return ResponseResult.okResult(apCommentRepays);
        }

        //3.2  如果登录，需要判断哪些回复点过赞
        // 基于当前列表 回复id集合  及  登录人id 查询回复点赞列表
        //得到回复ID集合
        List<String> repayIdList = apCommentRepays.stream().map(ApCommentRepay::getId).collect(Collectors.toList());
        List<ApCommentRepayLike> apCommentRepayLikes = mongoTemplate.find(
                Query.query(Criteria.where("repayId").in(repayIdList).and("userId").is(user.getId())), ApCommentRepayLike.class);
        if (apCommentRepayLikes.isEmpty()){
            // 直接返回评论集合
            return ResponseResult.okResult(apCommentRepays);
        }
        //遍历 当前回复列表，将每一个回复信息 封装成 vo
        //判断当前回复id 在点赞信息中是否存在，如果存在 operation设置为0 不存在不做处理
        ArrayList<ApCommentRepayVO> apCommentRepayVOS = new ArrayList<>();
        apCommentRepays.forEach(apCommentRepay -> {
            apCommentRepayLikes.forEach(apCommentRepayLike -> {
                ApCommentRepayVO apCommentRepayVO = new ApCommentRepayVO();
                BeanUtils.copyProperties(apCommentRepay,apCommentRepayVO);
                if (apCommentRepay.getId().equals(apCommentRepayLike.getApCommentRepay())){
                    apCommentRepayVO.setOperation((short) 0);
                }
                apCommentRepayVOS.add(apCommentRepayVO);
            });
        });
        return ResponseResult.okResult(apCommentRepayVOS);


    }

    @Override
    public ResponseResult saveCommentRepay(CommentRepaySaveDTO dto) {
        //1.校验是否登录 文章id不能为空 校验内容不能为空 校验内容长度不能大于140个字符 使用注解校验
        ApUser user = AppThreadLocalUtils.getUser();
        if(user == null){
            return ResponseResult.errorResult(AppHttpCodeEnum.NEED_LOGIN);
        }
        String content = dto.getContent();

        //2. 阿里云校验评论内容是否合法
        String finalContent = handleTextScan(content);

        //3.根据评论id查询评论信息  检查是否存在
        ApComment comment = mongoTemplate.findOne(Query.query(Criteria.where("id").is(dto.getCommentId())), ApComment.class);
        if (comment == null){
            CustException.cust(AppHttpCodeEnum.DATA_NOT_EXIST,"当前评论不存在!");
        }

        //保存评论回复信息
        ApCommentRepay apCommentRepay = new ApCommentRepay();
        apCommentRepay.setAuthorId(user.getId());
        apCommentRepay.setAuthorName(user.getName());
        apCommentRepay.setCommentId(comment.getId());
        apCommentRepay.setContent(finalContent);
        apCommentRepay.setLikes(0);
        apCommentRepay.setCreatedTime(new Date());
        apCommentRepay.setUpdatedTime(new Date());

        //修改评论信息中回复数量 + 1
        comment.setReply(comment.getReply() +1);
        mongoTemplate.save(comment);

        mongoTemplate.save(apCommentRepay);


        return ResponseResult.okResult();
    }

    @Override
    public ResponseResult saveCommentRepayLike(CommentRepayLikeDTO dto) {
        //1.参数校验评论id不能为空  operation必须为 0  或  1
        ApUser user = AppThreadLocalUtils.getUser();
        if (user == null){
            return ResponseResult.errorResult(AppHttpCodeEnum.NEED_LOGIN);
        }
        //2.校验回复信息是否存在
        ApCommentRepay apCommentRepay = mongoTemplate.findOne(Query.query(Criteria.where("id").is(dto.getCommentRepayId())), ApCommentRepay.class);
        if (apCommentRepay == null){
            CustException.cust(AppHttpCodeEnum.DATA_NOT_EXIST,"当前评论回复信息不存在!");
        }
        Short operation = dto.getOperation();
        // 0点赞 1取消
        //3. 如果是点赞操作 判断是否已经点赞过  如果已经点赞过 请勿重复点赞
        ApCommentRepayLike commentRepayLike = mongoTemplate.findOne(
                Query.query(Criteria.where("apCommentRepay").is(dto.getCommentRepayId()).and("authorId").is(user.getId())), ApCommentRepayLike.class);
        if (operation == 0){
            if (commentRepayLike != null){
                return ResponseResult.errorResult(AppHttpCodeEnum.DATA_NOT_ALLOW,"请勿重复点赞!");
            }else {
                // 保存点赞信息
                commentRepayLike = new ApCommentRepayLike();
                commentRepayLike.setAuthorId(user.getId());
                commentRepayLike.setOperation((short) 0);
                commentRepayLike.setApCommentRepay(apCommentRepay.getId());
                mongoTemplate.save(commentRepayLike);
                //评论点赞数 +1
                apCommentRepay.setLikes(apCommentRepay.getLikes() + 1);
                mongoTemplate.save(apCommentRepay);
            }
        }

        //4. 如果是取消点赞操作 删除点赞信息 并修改评论的点赞数 -1 判断不能减到负数
        if (operation == 1){
            if (commentRepayLike == null){
                return ResponseResult.errorResult(AppHttpCodeEnum.DATA_NOT_ALLOW,"请勿重复取消点赞!");
            }else {
                // 删除点赞信息
                mongoTemplate.remove(commentRepayLike);
                //评论点赞数 -1
                if (apCommentRepay.getLikes() >= 1){
                    apCommentRepay.setLikes(apCommentRepay.getLikes() - 1);
                    mongoTemplate.save(apCommentRepay);
                }
            }
        }

        //5. 返回结果 需要返回点赞数量 likes:1
        Integer likes = apCommentRepay.getLikes();
        Map<String, Integer> map = new HashMap<>();
        map.put("likes",likes);
        return ResponseResult.okResult(map);

    }

    /**
     *  使用阿里云内容安全审核
     * @param content 待审核文本

     * @return
     */
    private String handleTextScan(String content){

        try {
            ScanResult scanResult = greenScan.greenTextScan(content);
            // 阿里云建议 block:违规, review:需要人工审核, pass:通过
            String suggestion = scanResult.getSuggestion();
            switch (suggestion) {
                case "block":
                    // 违规
                    content = scanResult.getFilteredContent();
                    break;
                case "review":
                    // 需要人工审核

                    break;
                case "pass":
                    break;
                default:
                    // 人工
                    break;
            }
        } catch (Exception e) {
            log.error("远程调用阿里云内容安全审核异常 异常信息{}",e.getMessage());
            // 人工
        }
        return content;
    }
}
