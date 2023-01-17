package com.heima.user;

import com.heima.feigns.ArticleFeign;
import com.heima.feigns.WemediaFeign;
import com.heima.model.article.pojos.ApAuthor;
import com.heima.model.common.dtos.ResponseResult;
import com.heima.model.wemedia.pojos.WmUser;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import javax.annotation.Resource;
import java.util.Date;

/**
 * @author 金宗文
 * @version 1.0
 */
@SpringBootTest
public class FeignTest {

    @Resource
    WemediaFeign wemediaFeign;

    @Test
    public void test(){
        ResponseResult<WmUser> responseResult = wemediaFeign.findByName("admin");
        System.out.println(responseResult);
    }

    @Test
    public void save(){
        WmUser wmUser = new WmUser();
        wmUser.setApUserId(1);
        wmUser.setName("dsa");
        wmUser.setPassword("dsa");
        wmUser.setSalt("dsa");
        wmUser.setNickname("a");
        wmUser.setImage("dsa");
        wmUser.setLocation("dsa");
        wmUser.setPhone("dsa");
        wmUser.setStatus(0);
        wmUser.setEmail("dsa");
        wmUser.setType(0);
        wmUser.setScore(0);
        wmUser.setLoginTime(new Date());
        wmUser.setCreatedTime(new Date());
        ResponseResult<WmUser> responseResult = wemediaFeign.save(wmUser);
        System.out.println(responseResult);


    }

    @Resource
    ArticleFeign articleFeign;

    @Test
    public void findByUserId(){
        ResponseResult<ApAuthor> responseResult = articleFeign.findByUserId(4);
        System.out.println(responseResult);


        ApAuthor apAuthor = new ApAuthor();
        apAuthor.setName("newname");
        apAuthor.setType(2);
        apAuthor.setUserId(10);
        apAuthor.setCreatedTime(new Date());
        apAuthor.setWmUserId(11);
        articleFeign.save(apAuthor);
    }
}

