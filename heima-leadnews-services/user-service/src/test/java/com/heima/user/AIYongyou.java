package com.heima.user;

import com.alibaba.fastjson.JSON;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;

/**
 * @author 金宗文
 * @version 1.0
 */
public class AIYongyou {


    @Test
    public void idCardTest(){
        RestTemplate restTemplate = new RestTemplate();

        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.setContentType(MediaType.APPLICATION_JSON);
        httpHeaders.set("apicode","6fa3e7c1d012469b8ecc9f5e40a2a36d");

        //请求参数
        HashMap<String, String> map = new HashMap<>();
        map.put("idNumber","210103195103222113");
        map.put("userName","王东镇");

        HttpEntity<String> httpEntity = new HttpEntity<>(JSON.toJSONString(map), httpHeaders);

        ResponseEntity<String> responseEntity = restTemplate.postForEntity("https://api.yonyoucloud.com/apis/dst/matchIdentity/matchIdentity", httpEntity, String.class);
        System.out.println(responseEntity.getBody());

    }
}
