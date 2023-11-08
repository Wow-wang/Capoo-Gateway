package org.wow.core.filter.Auth;

import org.wow.core.context.GatewayContext;
import org.wow.core.filter.Filter;

/**
 * @program: Capoo-Api-gateway
 * @description:
 * @author: wow
 * @create: 2023-11-08 18:20
 **/
// TODO 自定义 OAUTH FIlter 接口

public class OAUTHFilter implements Filter {
    @Override
    public void doFilter(GatewayContext ctx) throws Exception {

    }

    @Override
    public int getOrder() {
        return Filter.super.getOrder();
    }
}
