package com.heima.feigns;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.heima.config.HeimaFeignAutoConfiguration;
import com.heima.feigns.fallback.ArticleFeignFallback;
import com.heima.model.admin.pojo.AdChannel;
import com.heima.model.article.pojos.ApArticle;
import com.heima.model.article.pojos.ApAuthor;
import com.heima.model.common.dtos.ResponseResult;
import io.swagger.annotations.ApiOperation;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * @author 金宗文
 * @version 1.0
 */
@FeignClient(value = "leadnews-article",fallbackFactory = ArticleFeignFallback.class,configuration = HeimaFeignAutoConfiguration.class)
public interface ArticleFeign  {


    @GetMapping("/api/v1/author/findByUserId/{userId}")
    public ResponseResult<ApAuthor> findByUserId(@PathVariable("userId") Integer userId);



    @PostMapping("/api/v1/author/save")
    public ResponseResult save(@RequestBody ApAuthor apAuthor);

    @PostMapping("/api/v1/article/findArticleById")
    public ResponseResult<ApArticle> findArticleById(@RequestParam(value = "articleId",required = true) Long articleId );

    // ================新增接口方法  start ================
    @GetMapping("/api/v1/channel/channels")
    ResponseResult<List<AdChannel>> selectChannels();
    // ================新增接口方法  end ================
}
