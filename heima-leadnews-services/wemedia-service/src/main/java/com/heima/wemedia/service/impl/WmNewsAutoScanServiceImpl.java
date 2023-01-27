package com.heima.wemedia.service.impl;


import com.alibaba.fastjson.JSON;
import com.heima.aliyun.scan.GreenScan;
import com.heima.aliyun.scan.ScanResult;
import com.heima.common.exception.CustException;
import com.heima.feigns.AdminFeign;
import com.heima.model.common.constants.wemedia.WemediaConstants;
import com.heima.model.common.dtos.ResponseResult;
import com.heima.model.common.enums.AppHttpCodeEnum;
import com.heima.model.wemedia.pojos.WmNews;
import com.heima.utils.common.SensitiveWordUtil;
import com.heima.wemedia.mapper.WmNewsMapper;
import com.heima.wemedia.service.WmNewsAutoScanService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import javax.annotation.Resource;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author 金宗文
 * @version 1.0
 */
@Slf4j
@Service
public class WmNewsAutoScanServiceImpl implements WmNewsAutoScanService {
    @Resource
    private WmNewsMapper wmNewsMapper;

    @Resource
    AdminFeign adminFeign;

    @Resource
    GreenScan greenScan;
    @Value("${file.oss.web-site}")
    String webSite;
    @Override
    public void autoScanWmNews(Integer id) {
        log.info("文章自动审核方法触发 待审核的文章id为: {} ",id);
        //1.判断文章id是否为空
        if (id == null){
            CustException.cust(AppHttpCodeEnum.PARAM_INVALID,"待审核id不能为空");
        }

        //2.根据文章id查询自媒体文章
        WmNews wmNews = wmNewsMapper.selectById(id);
        if (wmNews == null){
            CustException.cust(AppHttpCodeEnum.DATA_NOT_EXIST,"文章不存在");
        }

        //3.判断文章状态 必须为1待审核 (避免重复消费)
        Short status = wmNews.getStatus();
        if (!WemediaConstants.COLLECT_MATERIAL.equals(status)){
            log.info("当前文章状态为: {} ,不是待审核状态,不需要审核",status);
            return;
        }

        //4.  抽取文章所有的文本内容和图片内容
        Map<String, Object> contentAndImages = handleTextAndImages(wmNews);
        //5. DFA 进行自管理敏感词审核 2 有敏感词不通过
        boolean scanSensitive = handleSensitive((String) contentAndImages.get("content"),wmNews);
        if (!scanSensitive){
            //返回结果为false不通过
            log.info("文章id为: {} 的文章有敏感词,不通过",id);
            return;
        }
        //6.  阿里云的文本审核  2 有违规词汇, 3 不确定/aliyun 未调用成功 通过继续
        boolean scanContent = handleTextScan((String) contentAndImages.get("content"), wmNews);
        if (!scanContent){
            //返回结果为false不通过
            log.info("文章审核不通过,原因: 内容包含违规词汇");
            return;
        }
        //7.  阿里云的图片审核  2 有违规图片, 3 不确定/aliyun 未调用成功 通过继续
        List<String> images = (List<String>) contentAndImages.get("images");
        if (!CollectionUtils.isEmpty(images)){
            boolean imageScan = handleImageScan(images, wmNews);
            if (!imageScan){
                //返回结果为false不通过
                log.info("文章审核不通过,原因: 图片包含违规内容");
                return;
            }
        }



        //8.  将文章状态改为8
        updateWmNews(WmNews.Status.SUCCESS.getCode(), "审核通过",wmNews);

        //9. TODO 根据文章发布时间,发布延迟消息,用于定时发布文章(9 已发布)

    }

    /**
     *
     * @param iamges 图片地址集合
     * @param wmNews 文章对象
     * @return
     */
    private boolean handleImageScan(List<String> iamges,WmNews wmNews){
        boolean flag = false;
        ScanResult scanResult = null;
        try {
            scanResult = greenScan.imageUrlScan(iamges);
            String suggestion = scanResult.getSuggestion();
            switch (suggestion) {
                case "block":
                    // 违规
                    updateWmNews(WmNews.Status.FAIL.getCode(), "文章图片中有违规: " + scanResult.getLabel(), wmNews);
                    break;
                case "review":
                    // 需要人工审核
                    updateWmNews(WmNews.Status.ADMIN_AUTH.getCode(), "文章图片中有不确定因素,需要人工审核", wmNews);
                    break;
                case "pass":
                    flag = true;
                    break;
                default:
                    // 人工
                    updateWmNews(WmNews.Status.ADMIN_AUTH.getCode(), "阿里云调用状态异常,需要人工审核", wmNews);
                    break;
            }
        } catch (Exception e) {
            e.printStackTrace();
            // 人工
            updateWmNews(WmNews.Status.ADMIN_AUTH.getCode(), "阿里云调用状态异常,需要人工审核", wmNews);
        }
        return flag;

    }

    /**
     *  使用阿里云内容安全审核
     * @param content 待审核文本
     * @param wmNews 文本对象
     * @return
     */
    private boolean handleTextScan(String content,WmNews wmNews){
        boolean flag = false;
        try {
            ScanResult scanResult = greenScan.greenTextScan(content);
            // 阿里云建议 block:违规, review:需要人工审核, pass:通过
            String suggestion = scanResult.getSuggestion();
            switch (suggestion) {
                case "block":
                    // 违规
                    updateWmNews(WmNews.Status.FAIL.getCode(), "包涵违规词汇: " + scanResult.getLabel(), wmNews);
                    break;
                case "review":
                    // 需要人工审核
                    updateWmNews(WmNews.Status.ADMIN_AUTH.getCode(), "文章内容中有不确定因素,需要人工审核", wmNews);
                    break;
                case "pass":
                    flag = true;
                    break;
                default:
                    // 人工
                    updateWmNews(WmNews.Status.ADMIN_AUTH.getCode(), "阿里云调用状态异常,需要人工审核", wmNews);
                    break;
            }
        } catch (Exception e) {
            e.printStackTrace();;
            // 人工
            updateWmNews(WmNews.Status.ADMIN_AUTH.getCode(), "阿里云调用状态异常,需要人工审核", wmNews);
        }
        return flag;
    }


    /**
     *
     * @param content 文本内容
     * @param wmNews 文章对象
     * @return
     */
    private boolean handleSensitive(String content, WmNews wmNews) {
        boolean flag = true;
        //1. 远程调用敏感词接口 查询敏感词列表 使用adminFeign接口
        ResponseResult<List<String>> sensitives = adminFeign.sensitives();
        if (!sensitives.checkCode()) {
            CustException.cust(AppHttpCodeEnum.REMOTE_SERVER_ERROR);
        }
        //2. 获取敏感词列表
        List<String> sensitivesData = sensitives.getData();
        // 3.装敏感词转为DFA数据模型
        SensitiveWordUtil.initMap(sensitivesData);
        // 4.基于DFA数据模型进行敏感词扫描
        Map<String, Integer> map = SensitiveWordUtil.matchWords(content);
        // 5.如果有敏感词,将文章状态改为2
        if (!CollectionUtils.isEmpty(map)){
            updateWmNews(WmNews.Status.FAIL.getCode(),"内容中包涵敏感词: " + map ,wmNews);
            flag = false;
        }

        return flag;
    }

    private void updateWmNews(Short status,String message,WmNews wmNews) {
        wmNews.setStatus(status);
        wmNews.setReason(message);
        wmNewsMapper.updateById(wmNews);
    }

    /**
     * 抽取文章中的文本内容和图片内容
     * @param wmNews
     * @return Map<String,Object> key: content value:文本内容 key: images value:图片内容 List<String></String>
     */
    private Map<String,Object> handleTextAndImages(WmNews wmNews){
        HashMap<String, Object> result = new HashMap<>();
        //1. 判断内容不能为空 转为List<Map>
        String contentJson = wmNews.getContent();
        if (StringUtils.isBlank(contentJson)){
            CustException.cust(AppHttpCodeEnum.PARAM_INVALID,"文章内容不能为空");
        }
        List<Map> contentMapList = JSON.parseArray(contentJson, Map.class);

        //2. 抽取内容中所有文本内容,抽取为一个字符串
//        [{type:text,value:文本内容}]
        //家乡好没_hmtt_你好
        String content = contentMapList.stream().filter(m -> "text".equals(m.get("type"))).map(m -> m.get("value").toString()).collect(Collectors.joining("_hmtt_)"));
        //2.1 将文本内容与标题进行拼接
        content += wmNews.getTitle() + "_hmtt_";

        //2.2 将文本内容装入map
        result.put("content",content);

        //3 抽取文章中所有图片内容 得到图片列表
        //3.1 抽取content 中所有图片 得到图片列表
        List<String> images = contentMapList.stream().filter(m -> "image".equals(m.get("type"))).map(m -> m.get("value").toString()).collect(Collectors.toList());

        //3.2 抽取封面中所有的图片 得到图片列表
        //url1,url2,url3不带前缀
        String coverStr = wmNews.getImages();
        //将封面图片加上前缀

        if (StringUtils.isNotBlank(coverStr)) {
            List<String> coverImages = Arrays.stream(coverStr.split(",")).map(url -> webSite + url).collect(Collectors.toList());
            // 合并内容图片和封面图片
            images.addAll(coverImages);
        }
        // 3.4 去除重复图片
        images = images.stream().distinct().collect(Collectors.toList());

        // 3.5 将图片列表装入map
        result.put("images",images);

        //返回结果
        return result;

    }
}
