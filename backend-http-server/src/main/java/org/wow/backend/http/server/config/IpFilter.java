package org.wow.backend.http.server.config;

import org.springframework.security.web.util.matcher.IpAddressMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;

import javax.servlet.*;
import java.io.IOException;
import java.util.List;

/**
 * @program: api-gateway
 * @description:
 * @author: wow
 * @create: 2023-10-20 21:58
 **/

public class IpFilter implements Filter {

//    private final RequestMatcher requestMatcher;

    public IpFilter(String allowedIp) {
//        // 使用 IpAddressMatcher 匹配请求的IP地址是否与允许的IP地址匹配
//        this.requestMatcher = new RequestMatcherUtils.OrRequestMatcher(
//                List.of(new IpAddressMatcher(allowedIp))
//        );
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
//        if (requestMatcher.matches(request)) {
//            // IP地址匹配，允许请求通过
//            chain.doFilter(request, response);
//        } else {
//            // IP地址不匹配，可以根据需要执行拒绝访问的逻辑
//            // 例如，返回403 Forbidden响应
//            // ((HttpServletResponse) response).sendError(HttpServletResponse.SC_FORBIDDEN, "Access denied");
//        }
    }
}