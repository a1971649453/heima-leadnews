package com.heima.comment.service.impl;
import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

import com.heima.aliyun.scan.GreenScan;
import com.heima.aliyun.scan.ScanResult;
import com.heima.comment.service.CommentHotService;
import com.heima.comment.service.CommentService;
import com.heima.common.exception.CustException;
import com.heima.feigns.ArticleFeign;
import com.heima.feigns.UserFeign;
import com.heima.feigns.WemediaFeign;
import com.heima.model.article.pojos.ApArticle;
import com.heima.model.comment.dtos.CommentDTO;
import com.heima.model.comment.dtos.CommentLikeDTO;
import com.heima.model.comment.dtos.CommentSaveDTO;
import com.heima.model.comment.pojos.ApComment;
import com.heima.model.comment.pojos.ApCommentLike;
import com.heima.model.comment.vo.ApCommentVo;
import com.heima.model.common.dtos.ResponseResult;
import com.heima.model.common.enums.AppHttpCodeEnum;
import com.heima.model.threadlocal.AppThreadLocalUtils;
import com.heima.model.user.pojos.ApUser;
import com.heima.model.wemedia.pojos.WmNews;
import lombok.extern.slf4j.Slf4j;
import org.redisson.Redisson;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.BeanUtils;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

/**
 * @author 金宗文
 * @version 1.0
 */
@Service
@Slf4j
public class CommentServiceImpl implements CommentService {
    @Resource
    private UserFeign userFeign;

    @Resource
    private GreenScan greenScan;

    @Resource
    private MongoTemplate mongoTemplate;


    @Resource
    private ArticleFeign articleFeign;

    @Resource
    private CommentHotService commentHotService;

    @Resource
    private RedissonClient redisson;
    @Override
    public ResponseResult saveComment(CommentSaveDTO dto) {
        //1.校验是否登录 文章id不能为空 校验内容不能为空 校验内容长度不能大于140个字符 使用注解校验
        ApUser user = AppThreadLocalUtils.getUser();
        if(user == null){
            return ResponseResult.errorResult(AppHttpCodeEnum.NEED_LOGIN);
        }
        String content = dto.getContent();

        //2. 阿里云校验评论内容是否合法
        String finalContent = handleTextScan(content);

        //3.远程查询当前用户信息
        ResponseResult<ApUser> responseResult = userFeign.findUserById(user.getId());
        if (!responseResult.checkCode()){
            log.error("远程查询用户信息失败 用户ID:{}",user.getId());
            CustException.cust(AppHttpCodeEnum.REMOTE_SERVER_ERROR,"远程查询用户信息失败");
        }
        ApUser apUser = responseResult.getData();

        //4.创建评论信息 并保存到mongodb
        //4.1远程查询 当前文章信息
        ResponseResult<ApArticle> articleResponseResult = articleFeign.findArticleById(dto.getArticleId());
        if (!articleResponseResult.checkCode()){
            CustException.cust(AppHttpCodeEnum.REMOTE_SERVER_ERROR,"远程查询文章信息失败");
        }
        //得到当前文章对象
        ApArticle apArticle = articleResponseResult.getData();
        if (apArticle == null){
            CustException.cust(AppHttpCodeEnum.DATA_NOT_EXIST,"当前文章不存在!");
        }
        //创建评论对象
        ApComment apComment = new ApComment();
        apComment.setAuthorId(user.getId());
        apComment.setAuthorName(apUser.getName());
        apComment.setArticleId(dto.getArticleId());
        apComment.setChannelId(apArticle.getChannelId());
        apComment.setContent(finalContent);
        apComment.setImage(apArticle.getImages());
        apComment.setLikes(0);
        apComment.setReply(0);
        apComment.setFlag((short)0);
        apComment.setCreatedTime(new Date());
        apComment.setUpdatedTime(new Date());
        mongoTemplate.save(apComment);

        return ResponseResult.okResult();
    }

    @Override
    public ResponseResult like(CommentLikeDTO dto) {
        //1.参数校验评论id不能为空  operation必须为 0  或  1
        ApUser user = AppThreadLocalUtils.getUser();
        if (user == null){
            return ResponseResult.errorResult(AppHttpCodeEnum.NEED_LOGIN);
        }

        ApComment apComment;
        Short operation = dto.getOperation();
        // 0点赞 1取消
        //3. 如果是点赞操作 判断是否已经点赞过  如果已经点赞过 请勿重复点赞
        ApCommentLike apCommentLike = mongoTemplate.findOne(
                Query.query(Criteria.where("commentId").is(dto.getCommentId()).and("authorId").is(user.getId())), ApCommentLike.class);
        RLock lock = redisson.getLock("likes-lock");
        //分布式锁
        lock.lock();
        try {
            //2.根据评论ID查询评论数据 为null返回错误信息
            apComment = mongoTemplate.findById(dto.getCommentId(),ApComment.class);
            if (apComment == null){
                CustException.cust(AppHttpCodeEnum.DATA_NOT_EXIST,"当前评论不存在!");
            }
            if (operation == 0) {
//                if (apCommentLike != null) {
//                    return ResponseResult.errorResult(AppHttpCodeEnum.DATA_NOT_ALLOW, "请勿重复点赞!");
//                } else {
                    // 保存点赞信息
                    apCommentLike = new ApCommentLike();
                    apCommentLike.setCommentId(dto.getCommentId());
                    apCommentLike.setAuthorId(user.getId());
                    mongoTemplate.save(apCommentLike);
                    //评论点赞数 +1
                    apComment.setLikes(apComment.getLikes() + 1);
                    mongoTemplate.save(apComment);
//                }
            }
            //4. 如果是取消点赞操作 删除点赞信息 并修改评论的点赞数 -1 判断不能减到负数
            if (operation == 1) {
                if (apCommentLike == null) {
                    return ResponseResult.errorResult(AppHttpCodeEnum.DATA_NOT_ALLOW, "请勿重复取消点赞!");
                } else {
                    // 删除点赞信息
                    mongoTemplate.remove(apCommentLike);
                    //评论点赞数 -1
                    if (apComment.getLikes() >= 1) {
                        apComment.setLikes(apComment.getLikes() - 1);
                        mongoTemplate.save(apComment);
                    }
                }
            }
        }
        finally {
            lock.unlock();
        }


        //5. 返回结果 需要返回点赞数量 likes:1
        Integer likes = apComment.getLikes();
        // 判断热点评论 点赞数大于等于10
        if (likes >= 10 && apComment.getFlag() == 0){
            commentHotService.hotCommentExecutor(apComment);
        }
        Map<String, Integer> map = new HashMap<>();
        map.put("likes",likes);
        return ResponseResult.okResult(map);
    }

    @Override
    public ResponseResult findByArticleId(CommentDTO dto) {
        //1.参数校验 articleId不能为空
        Date minDate = dto.getMinDate();
        if (minDate == null){
            minDate = new Date();
        }
        Integer size = dto.getSize();
        if (size == null || size == 0){
            size = 10;
        }
        //   ========================需要变更处 start =============================
        //2 查询Mongo文章所有评论列表
        List<ApComment> apComments = null;
        if(dto.getIndex().intValue() == 1){ // 判断当前是否是第一页评论
            // 先查询热点评论集合  （最多5条） (条件: flag=1, 文章id, 点赞降序)
            Query hotQuery = Query.query(Criteria.where("articleId").is(dto.getArticleId()).and("flag").is(1)).with(Sort.by(Sort.Direction.DESC, "likes"));
            apComments = mongoTemplate.find(hotQuery, ApComment.class);
            // 新size = size - 热评数量
            size = size - apComments.size();
            // 查询第一页剩余普通评论 (条件: 文章id, flag=0, 时间降序, limit:新size)
            Sort sort = Sort.by(Sort.Direction.DESC,"createdTime");
            Query commonQuery = Query.query(Criteria.where("articleId").is(dto.getArticleId()).and("createdTime").lt(minDate).and("flag").is(0)).with(sort).limit(size);
            List<ApComment> apCommonComments = mongoTemplate.find(commonQuery, ApComment.class);
            // 合并 热点评论  普通评论   热点list.addAll(普通list)
            apComments.addAll(apCommonComments);
        }else {
            // 不是第一页直接查询普通评论
            // (条件: 文章id,flag=0,createdTime小于最小时间,时间降序,limit:size)
            Sort sort = Sort.by(Sort.Direction.DESC,"createdTime");
            Query commonQuery = Query.query(Criteria.where("articleId").is(dto.getArticleId()).and("createdTime").lt(minDate)).with(sort).limit(size);
            apComments = mongoTemplate.find(commonQuery, ApComment.class);
        }
        // ========================需要变更处 end =============================
        //如果用户未登录 直接返回评论列表
        ApUser user = AppThreadLocalUtils.getUser();
        if (user == null){
            return ResponseResult.okResult(apComments);
        }
        //如果登陆了 需要检查在当前评论列表中 那些评论登录人点赞过
        // 得到commentID集合
        List<String> commentIds = apComments.stream().map(ApComment::getId).collect(Collectors.toList());

        // 根据当前列表中评论ID 和 登录人ID 查询评论点赞表 得到点赞数据
        List<ApCommentLike> apCommentLikes = mongoTemplate.find(Query.query(Criteria.where("commentId").in(commentIds).and("authorId").is(user.getId())), ApCommentLike.class);

        // 遍历点赞列表 将每一个评论APComment转为ApCommentVO 其中如果当前评论在点赞记录中存在 设置operation为0 不存在不做任何处理
        if (apCommentLikes.isEmpty()){
            return ResponseResult.okResult(apComments);
        }
        List<ApCommentVo> apCommentVos = new ArrayList<>();
        for (ApCommentLike apCommentLike : apCommentLikes) {
            for (ApComment apComment : apComments) {
                ApCommentVo apCommentVo = new ApCommentVo();
                BeanUtils.copyProperties(apComment, apCommentVo);
                if (apCommentLike.getCommentId().equals(apComment.getId())){
                    apCommentVo.setOperation((short) 0);
                }
                apCommentVos.add(apCommentVo);
            }
        }
        return ResponseResult.okResult(apCommentVos);
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
