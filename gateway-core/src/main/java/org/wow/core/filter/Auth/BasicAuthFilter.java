package org.wow.core.filter.Auth;

import lombok.extern.slf4j.Slf4j;
import org.wow.core.context.GatewayContext;
import org.wow.core.filter.Filter;
import org.wow.core.filter.FilterAspect;

import static org.wow.common.constants.FilterConst.*;

/**
 * @program: Capoo-Api-gateway
 * @description:
 * @author: wow
 * @create: 2023-11-08 16:54
 **/
@Slf4j
@FilterAspect(id= BASIC_AUTH_FILTER_ID,
        name = BASIC_AUTH_FILTER_NAME,
        order = BASIC_AUTH_FILTER_ORDER )
public class BasicAuthFilter implements Filter {

    private static final String COOKIE_NAME = "Authorization";

    @Override
    public void doFilter(GatewayContext ctx) throws Exception {
        // TODO 认证信息缓存
        // 用户认证
    }
}
