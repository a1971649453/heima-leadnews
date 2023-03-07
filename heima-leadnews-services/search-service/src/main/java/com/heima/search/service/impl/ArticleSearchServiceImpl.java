package com.heima.search.service.impl;

import com.heima.common.exception.CustException;
import com.heima.es.service.EsService;
import com.heima.model.common.constants.search.SearchConstants;
import com.heima.model.common.dtos.PageResponseResult;
import com.heima.model.common.dtos.ResponseResult;
import com.heima.model.common.enums.AppHttpCodeEnum;
import com.heima.model.search.dtos.UserSearchDTO;
import com.heima.model.search.vos.SearchArticleVO;
import com.heima.model.threadlocal.AppThreadLocalUtils;
import com.heima.model.user.pojos.ApUser;
import com.heima.search.service.ApUserSearchService;
import com.heima.search.service.ArticleSearchService;
import org.apache.commons.lang3.StringUtils;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightBuilder;
import org.elasticsearch.search.sort.SortOrder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author 金宗文
 * @version 1.0
 */
@Service
public class ArticleSearchServiceImpl implements ArticleSearchService {
    @Resource
    private EsService<SearchArticleVO> esService;
    @Value("${file.minio.readPath}")
    private String readPath;

    @Value("${file.oss.web-site}")
    private String webSite;

    @Resource
    private ApUserSearchService apUserSearchService;

    @Override
    public ResponseResult search(UserSearchDTO userSearchDto) {

        // 1.检查参数 (关键字)
        String searchWords = userSearchDto.getSearchWords();
        if (StringUtils.isBlank(searchWords)){
            CustException.cust(AppHttpCodeEnum.PARAM_INVALID,"搜索内容不能为空");
        }
        if (userSearchDto.getSearchWords() == null) {
            userSearchDto.setMinBehotTime(new Date());
        }
        // 2. 构建请求对象 SearchSourceBuilder
        SearchSourceBuilder builder = new SearchSourceBuilder();

        // 登录用户ID
        ApUser user = AppThreadLocalUtils.getUser();
        // 在异步方法中无法获取当前线程中的用户信息
        userSearchDto.setLoginUserId(user == null ? null : user.getId());
        apUserSearchService.insert(userSearchDto);
        // 2.1 构建查询条件 builder.query
            // 2.1.1 构建bool查询条件
        BoolQueryBuilder boolQuery = QueryBuilders.boolQuery();
        // 2.1.2 创建 分词条件 must  match 查询title 加入到bool
        boolQuery.must(QueryBuilders.matchQuery("title",searchWords));
            // 2.1.3 创建 范围查询条件 range 查询 publishTIme  ==> filter
        boolQuery.filter(QueryBuilders.rangeQuery("publishTime").lt(userSearchDto.getMinBehotTime()));
        builder.query(boolQuery);
        // 2.2 构建高亮条件 builder.highlighter
        HighlightBuilder highlightBuilder = new HighlightBuilder();
        // 2.2.1 哪个字段高亮
        highlightBuilder.field("title");
            // 2.2.2 前置标签
        highlightBuilder.preTags("<font style='color: red; font-size: inherit;'>");
            //2.2.3 后置标签
        highlightBuilder.postTags("</font>");
        builder.highlighter(highlightBuilder);
        // 2.3 构建排序条件 builder.sort 发布时间降序 (默认: sort("publishTime", SortOrder.DESC))
        builder.sort("publishTime", SortOrder.DESC);
        // 2.4 构建分页条件 builder.from(0) builder.size(pageSize)
        builder.size(userSearchDto.getPageSize());
        // 3 执行搜素 封装返回结果
        PageResponseResult result = esService.search(builder, SearchArticleVO.class, SearchConstants.ARTICLE_INDEX_NAME);

        List<SearchArticleVO> list  = (List<SearchArticleVO>) result.getData();
        for (SearchArticleVO searchArticleVO : list) {
            // 3.1 封装返回前缀
            searchArticleVO.setStaticUrl(readPath + searchArticleVO.getStaticUrl());

            // 3.2 images 封面
            String images = searchArticleVO.getImages();
            if (StringUtils.isNotBlank(images)) {
                images = Arrays.stream(images.split(","))
                        .map(url -> webSite + url)
                        .collect(Collectors.joining(","));
                searchArticleVO.setImages(images);
            }
        }

        return result;
    }

    @Override
    public void saveArticle(SearchArticleVO article) {
        esService.save(article,SearchConstants.ARTICLE_INDEX_NAME);
    }

    @Override
    public void deleteArticle(String articleId) {
        esService.deleteById(articleId,SearchConstants.ARTICLE_INDEX_NAME);
    }
}
