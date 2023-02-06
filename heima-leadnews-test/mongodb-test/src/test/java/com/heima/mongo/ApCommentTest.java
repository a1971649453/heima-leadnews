package com.heima.mongo;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Random;

import com.heima.mongo.pojo.ApComment;
import com.mongodb.client.result.UpdateResult;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;

import javax.annotation.Resource;

/**
 * @author 金宗文
 * @version 1.0
 */
@SpringBootTest
public class ApCommentTest {

    @Resource
    MongoTemplate mongoTemplate;

    @Test
    public void saveDoc(){
        ArrayList<ApComment> list = new ArrayList<>();
        for (int i = 0; i < 20; i++) {
            ApComment apComment = new ApComment();

            apComment.setContent("这是一条有味道的评论");
            apComment.setLikes(new Random().nextInt(100));
            apComment.setReply(new Random().nextInt(100));
            apComment.setAddress("撒好难过还");
            apComment.setCreatedTime(new Date());
            list.add(apComment);
        }

        mongoTemplate.insertAll(list);

    }

    @Test
    public void updateDoc(){
            ApComment apComment = new ApComment();
            apComment.setId("63ddf61c278f476b567f56f9");
            apComment.setContent("这是一条有味道的评论");
            apComment.setLikes(new Random().nextInt(100));
            apComment.setReply(new Random().nextInt(100));
            apComment.setAddress("芜湖");
            apComment.setCreatedTime(new Date());
            mongoTemplate.save(apComment);

    }

    @Test
    public void updateDocByFiled(){
        // 条件 likes = 29
        Query query = Query.query(Criteria.where("likes").is(29));
        Update update = new Update();
        update.set("address","北京");
        // 只更新第一个
//        UpdateResult updateResult = mongoTemplate.updateFirst(query, update, ApComment.class);
        //更新全部
        UpdateResult updateResult1 = mongoTemplate.updateMulti(query, update, ApComment.class);
        System.out.println(updateResult1);
//        System.out.println(updateResult);

    }

    @Test
    public void findOneTest(){

        Query query = Query.query(Criteria.where("likes").is(29));
        ApComment one = mongoTemplate.findOne(query, ApComment.class);
        List<ApComment> all1 = mongoTemplate.findAll(ApComment.class);
        for (ApComment apComment : all1) {
            System.out.println(apComment);
        }
        System.out.println(one);

    }
    @Test
    public void findOneByQueryTest(){

        List<ApComment> apComments = mongoTemplate.find(Query.query(Criteria.where("likes").gt(50).and("address").is("北京")), ApComment.class);
        for (ApComment apComment : apComments) {
            System.out.println(apComment);
        }
    }
    // 分页查询
    @Test
    public void testFindPage() throws Exception {
        Query query = Query.query(Criteria
                .where("address").is("北京")
        );
        //分页方式1
        //跳过几条数据
//        query.skip(2);
        //截取几条数据
//        query.limit(3);

        int page = 1;
        int size = 5;
        //sping分页参数是从0开始
        query.with(PageRequest.of(page-1,size));

        //排序
        query.with(Sort.by(Sort.Direction.DESC,"likes"));
        List<ApComment> apComments = mongoTemplate.find(query, ApComment.class);
        apComments.forEach(System.out::println);
    }

}
