package org.wow.core.filter;

import lombok.extern.slf4j.Slf4j;
import org.wow.common.constants.GatewayConst;
import org.wow.core.context.GatewayContext;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * @program: api-gateway
 * @description: 过滤器链条类
 * 工厂模式+责任链模式实现可插拔式、可扩展性的动态过滤器链
 *
 * 责任链模式（Chain of Responsibility Pattern）是一种行为设计模式
 * 它允许你将请求沿着处理链传递，并且每个处理器（或处理者）都可以选择处理请求或将其传递给链中的下一个处理器
 * 这种模式将请求的发送者和接收者解耦，使得多个对象都有机会处理请求
 *
 * @author: wow
 * @create: 2023-10-02 09:26
 **/

@Slf4j
public class GatewayFilterChain {
    private List<Filter> filters = new ArrayList<>();

    public GatewayFilterChain addFilter(Filter filter){
        filters.add(filter);
        return this;
    }

    public GatewayFilterChain addFilterList(List<Filter> filter){
        filters.addAll(filter);
        return this;
    }

    public GatewayContext doFilter(GatewayContext ctx) throws Throwable{
        // 为空？？？
        if(filters.isEmpty()){
            return ctx;
        }

        try {
            for(Filter fl : filters){
                fl.doFilter(ctx);
            }
        } catch (Exception e) {
            log.error("执行过滤器发生异常 异常信息: {}" ,e.getMessage());
            throw e;
        }
        return ctx;
    }

}
