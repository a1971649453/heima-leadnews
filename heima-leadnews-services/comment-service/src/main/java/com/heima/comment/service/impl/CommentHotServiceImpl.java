package com.heima.comment.service.impl;

import com.heima.comment.service.CommentHotService;
import com.heima.model.comment.pojos.ApComment;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
public class CommentHotServiceImpl implements CommentHotService {
    @Autowired
    MongoTemplate mongoTemplate;
    /**
     * 处理热点评论
     * @param apComment 评论信息
     */
    // Async代表异步执行
    // taskExecutor是我们在配置中定义的线程池
    // Spring会使用线程池中的线程 异步执行此方法
    @Async("taskExecutor")
    public void hotCommentExecutor(ApComment apComment) {
        log.info("异步计算热点文章==================> 开始");
        // 1. 查询当前文章下的所有热点评论集合
        //     1.1 按照文章id   flag=1(热点文章)   点赞降序
        Query query = Query.query(Criteria.where("articleId").is(apComment.getArticleId()).and("flag").is(1)).with(Sort.by(Sort.Direction.DESC, "likes"));
        List<ApComment> apComments = mongoTemplate.find(query, ApComment.class);
        // 2. 如果 热评集合为空  或  热评数量小于5 直接将当前评论改为热评
        if (apComments.isEmpty() || apComments.size() < 5){
            apComment.setFlag((short) 1);
            mongoTemplate.save(apComment);
        }else {
            // 3. 如果热评数量大于等于 5
            // 3.1  获取热评集合中 最后点赞数量最少的热评
            ApComment lastComment = apComments.get(apComments.size() - 1);
            // 3.2 和当前评论点赞数量做对比  谁的点赞数量多 改为热评
            if (apComment.getLikes() > lastComment.getLikes()) {
                // 3.3 如果当前评论点赞数量大于最后一条热评的点赞数量
                // 3.3.1 将最后一条热评改为普通评论
                lastComment.setFlag((short) 0);
                mongoTemplate.save(lastComment);
                // 3.3.2 将当前评论改为热评
                apComment.setFlag((short) 1);
            }else {
                // 3.4 如果当前评论点赞数量小于最后一条热评的点赞数量
                // 3.4.1 将当前评论改为普通评论
                apComment.setFlag((short) 0);
            }
            mongoTemplate.save(apComment);
        }



        log.info("异步计算热点文章==================> 结束");
    }
}