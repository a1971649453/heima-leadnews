package com.heima.gateway.filter;

import com.alibaba.fastjson.JSON;
import com.heima.gateway.util.AppJwtUtil;
import io.jsonwebtoken.Claims;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author 金宗文
 * @version 1.0
 */
@Component
@Order(1)
@Slf4j
public class AuthorizeFilter implements GlobalFilter {
    private static List<String> urlList = new ArrayList<>();
//    初始化白名单 url路径
    static {
        urlList.add("/login/in");
        urlList.add("/v2/api-docs");
        urlList.add("/login_auth");
    }
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        ServerHttpResponse response = exchange.getResponse();
        //获取请求uri地址
        String path = request.getURI().getPath();
        // 判断是否属于白名单路径 是就放行
        for (String allowUri : urlList) {
            if (path.contains(allowUri)) {
                return chain.filter(exchange);
            }
        }
        // 不属于 就获取请求头中的token 如果没有token 返回401 终止请求
        String token = request.getHeaders().getFirst("token");
//        if (token == null) {
//            log.error("token is null");
//            response.setStatusCode(HttpStatus.UNAUTHORIZED);
//            return response.setComplete();
//        }
        if(StringUtils.isBlank(token)){
            //如果不存在，向客户端返回错误提示信息
            return writeMessage(exchange, "需要登录");
        }

        // 有就校验并解析token
        try {
            Claims body = AppJwtUtil.getClaimsBody(token);
            // -1：有效，0：有效，1：过期，2：过期
            int i = AppJwtUtil.verifyToken(body);
            if (i >0){
                return writeMessage(exchange, "认证失效，请重新登录");
            }
            // 解析成功 获取token中存放的userId 将用户id存入请求头 传递给后续服务
            Object id = body.get("id");
            request.mutate().header("userId",String.valueOf(id));

            //认证成功 放行请求
            return chain.filter(exchange);
        } catch (Exception e) {
            log.error("token 校验失败 :{}", e);
            return writeMessage(exchange, "认证失效，请重新登录");
        }

        // 解析成功 获取token中存放的userId 将用户id存入请求头 传递给后续服务
    }

    /**
     * 返回错误提示信息
     * @return
     */
    private Mono<Void> writeMessage(ServerWebExchange exchange, String message) {
        Map<String, Object> map = new HashMap<>();
        map.put("code", HttpStatus.UNAUTHORIZED.value());
        map.put("errorMessage", message);
        //获取响应对象
        ServerHttpResponse response = exchange.getResponse();
        //设置状态码
        response.setStatusCode(HttpStatus.UNAUTHORIZED);
        //response.setStatusCode(HttpStatus.OK);
        //设置返回类型
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
        //设置返回数据
        DataBuffer buffer = response.bufferFactory().wrap(JSON.toJSONBytes(map));
        //响应数据回浏览器
        return response.writeWith(Flux.just(buffer));
    }
}
