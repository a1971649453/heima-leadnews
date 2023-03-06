package com.heima.datasync.service.impl;
import com.heima.datasync.mapper.ApArticleMapper;
import com.heima.datasync.service.EsDataService;
import com.heima.es.service.EsService;
import com.heima.model.article.pojos.ApArticle;
import com.heima.model.common.dtos.ResponseResult;
import com.heima.model.common.enums.AppHttpCodeEnum;
import com.heima.model.search.vos.SearchArticleVO;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.ArrayList;
import java.util.List;
@Service
public class EsDataServiceImpl implements EsDataService {
    public static final String ARTICLE_INDEX_NAME = "app_info_article";
    @Autowired
    private ApArticleMapper apArticleMapper;
    @Autowired
    private EsService<SearchArticleVO> esService;
    @Override
    public ResponseResult dataInit() {
        // 判断索引库是否存在
        boolean isExist = esService.existIndex(ARTICLE_INDEX_NAME);
        if (isExist) {
            esService.deleteIndex(ARTICLE_INDEX_NAME);
        }
        esService.createIndex(
                "{\n" +
                "    \"mappings\":{\n" +
                "        \"properties\":{\n" +
                "            \"id\":{\n" +
                "                \"type\":\"long\"\n" +
                "            },\n" +
                "            \"publishTime\":{\n" +
                "                \"type\":\"date\"\n" +
                "            },\n" +
                "            \"layout\":{\n" +
                "                \"type\":\"integer\"\n" +
                "            },\n" +
                "            \"images\":{\n" +
                "                \"type\":\"keyword\",\n" +
                "                \"index\": false\n" +
                "            },\n" +
                "           \"staticUrl\":{\n" +
                "                \"type\":\"keyword\",\n" +
                "                \"index\": false\n" +
                "            },\n" +
                "            \"authorId\": {\n" +
                "          \t\t\"type\": \"long\"\n" +
                "       \t\t},\n" +
                "          \"title\":{\n" +
                "            \"type\":\"text\",\n" +
                "            \"analyzer\":\"ik_smart\"\n" +
                "          }\n" +
                "        }\n" +
                "    }\n" +
                "}", ARTICLE_INDEX_NAME);
        // 批量导入数据:
        // 一般建议是1000-5000个文档，如果你的文档很大，可以适当减少队列，大小建议是5-15MB，默认不能超过100M
        // TODO 如果数据库数据过多 建议分段批量插入
        // 分页查询第一页文章数据
        List<ApArticle> apArticles = apArticleMapper.findAllArticles();
        if (apArticles == null || apArticles.size() == 0) {
            return ResponseResult.errorResult(AppHttpCodeEnum.SERVER_ERROR, "数据库文章信息不存在");
        }
        // 遍历页码批量插入到索引库
        List<SearchArticleVO> list = new ArrayList<>();
        for (ApArticle record : apArticles) {
            // 封装数据
            SearchArticleVO articleVo = new SearchArticleVO();
            BeanUtils.copyProperties(record, articleVo);
            list.add(articleVo);
        }
        esService.saveBatch(list, ARTICLE_INDEX_NAME);
        return ResponseResult.okResult();
    }
}