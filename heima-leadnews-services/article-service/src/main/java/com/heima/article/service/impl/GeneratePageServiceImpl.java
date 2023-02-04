package com.heima.article.service.impl;

import com.alibaba.fastjson.JSON;
import com.heima.article.mapper.ApArticleMapper;
import com.heima.article.mapper.AuthorMapper;
import com.heima.article.service.GeneratePageService;
import com.heima.common.exception.CustException;
import com.heima.file.service.FileStorageService;
import com.heima.model.article.pojos.ApArticle;
import com.heima.model.article.pojos.ApAuthor;
import com.heima.model.common.enums.AppHttpCodeEnum;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;

/**
 * @author 金宗文
 * @version 1.0
 */
@Service
@Slf4j
public class GeneratePageServiceImpl implements GeneratePageService {
    @Resource
    private Configuration configuration;
    @Resource
    private AuthorMapper authorMapper;

    @Resource
            @Qualifier("minIOFileStorageService")
    private FileStorageService fileStorageService;

    @Value("${file.minio.prefix}")
    private String minioPrefix;

    @Resource
    private ApArticleMapper apArticleMapper;

    @Value("${file.minio.readPath}")
    private String readPath;


    @Override
    public void generateArticlePage(String content, ApArticle apArticle) {
        //1.获取模板
        try {
            Template template = configuration.getTemplate("article.ftl");
            //2.准备数据
            Map params = new HashMap();
            //文章对象
            params.put("article",apArticle);
            //文章 内容
            params.put("content", JSON.parseArray(content,Map.class));
            //作者对应的ap_user_id
            Long authorId = apArticle.getAuthorId();
            ApAuthor apAuthor = authorMapper.selectById(authorId);
            if (apAuthor == null){
                CustException.cust(AppHttpCodeEnum.DATA_NOT_EXIST,"对应作者信息不存在");
            }
            params.put("authorApUserId",apAuthor.getUserId());
            //3.替换数据  输出到StringWriter
            StringWriter stringWriter = new StringWriter();
            template.process(params,stringWriter);
            String htmlStr = stringWriter.toString();
            //4.封装输入流 字节数组输入流
            ByteArrayInputStream inputStream = new ByteArrayInputStream(htmlStr.getBytes());
            //5.将静态页内容上传到minio
            String path = fileStorageService.store(minioPrefix, apArticle.getId() + ".html", "text/html", inputStream);
            //6.修改文章static_url字段
            apArticle.setStaticUrl(path);
            apArticleMapper.updateById(apArticle);
            log.info("静态页生成成功，路径为：{}", readPath + path);
        } catch (IOException | TemplateException e) {
            log.info("静态页生成失败 原因:{} 请检查模板是否存在或者 变量是否正确",e.getMessage());
            CustException.cust(AppHttpCodeEnum.SERVER_ERROR,"生成静态页失败");
        }













    }
}
