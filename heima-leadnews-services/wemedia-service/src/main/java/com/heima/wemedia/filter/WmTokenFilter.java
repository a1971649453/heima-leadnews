package com.heima.wemedia.filter;

import com.heima.model.threadlocal.WmThreadLocalUtils;
import com.heima.model.wemedia.pojos.WmUser;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import javax.servlet.*;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;

/**
 * @author 金宗文
 * @version 1.0
 */
@Component
@WebFilter(value = "wmTokenFilter",urlPatterns = "/*")
@Order(-1)
@Slf4j
public class WmTokenFilter extends GenericFilter {
    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
        //1.获取请求头中的userId
        HttpServletRequest request = (HttpServletRequest) servletRequest;
        String userId = request.getHeader("userId");
        // 将userId封装为WmUser对象 存入到ThreadLocal中
        if (StringUtils.isNotBlank(userId)){
            WmUser wmUser = new WmUser();
            wmUser.setId(Integer.valueOf(userId));
            WmThreadLocalUtils.setUser(wmUser);
        }
        //2.放行
        filterChain.doFilter(servletRequest,servletResponse);

        //3.清空ThreadLocal
        WmThreadLocalUtils.clear();

    }
}
