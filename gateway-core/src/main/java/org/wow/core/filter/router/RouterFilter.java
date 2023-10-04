package org.wow.core.filter.router;

import com.alibaba.nacos.common.utils.StringUtils;
import com.netflix.hystrix.*;
import lombok.extern.slf4j.Slf4j;
import org.asynchttpclient.Request;
import org.asynchttpclient.Response;
import org.wow.common.config.Rule;
import org.wow.common.enums.ResponseCode;
import org.wow.common.exception.ConnectException;
import org.wow.common.exception.ResponseException;
import org.wow.core.ConfigLoader;
import org.wow.core.context.GatewayContext;
import org.wow.core.filter.Filter;
import org.wow.core.filter.FilterAspect;
import org.wow.core.helper.AsyncHttpHelper;
import org.wow.core.helper.ResponseHelper;
import org.wow.core.response.GatewayResponse;

import java.io.IOException;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeoutException;

import static org.wow.common.constants.FilterConst.*;

/**
 * @program: api-gateway
 * @description:
 * @author: wow
 * @create: 2023-10-02 19:17
 **/
@Slf4j
@FilterAspect(id = ROUTER_FILTER_ID,
    name = ROUTER_FILTER_NAME,
    order = ROUTER_FILTER_ORDER)
public class RouterFilter implements Filter {

    /**
     * 负责接收原始请求，将其发送到后端服务器（通过 AsyncHttpHelper 执行异步HTTP请求）
     * 并在请求完成时触发回调来处理结果。
     * 根据配置，它可以使用双异步或单异步模式来执行回调，以实现更灵活的异步处理
     * @param gatewayContext
     */

    @Override
    public void doFilter(GatewayContext gatewayContext) throws Exception {

        //使用java8 lambda拿到
        Optional<Rule.HystrixConfig> hystrixConfig = getHystrixConfig(gatewayContext);
        if(hystrixConfig.isPresent()){
            routeWithHystrix(gatewayContext,hystrixConfig);
        }else{
            route(gatewayContext,hystrixConfig);
        }


    }

    // 通过Hystrix执行熔断器操作
    private void routeWithHystrix(GatewayContext gatewayContext, Optional<Rule.HystrixConfig> hystrixConfig) {
        HystrixCommand.Setter setter = HystrixCommand.Setter.withGroupKey(HystrixCommandGroupKey
                        .Factory
                .asKey(gatewayContext.getUniqueId()))
                .andCommandKey(HystrixCommandKey.Factory
                        .asKey(gatewayContext.getRequest().getPath()))
                // 线程池大小
                .andThreadPoolPropertiesDefaults(HystrixThreadPoolProperties.Setter()
                        .withCoreSize(hystrixConfig.get().getThreadCoreSize()))
                .andCommandPropertiesDefaults(HystrixCommandProperties.Setter()
                    // 线程池
                        .withExecutionIsolationStrategy(HystrixCommandProperties.ExecutionIsolationStrategy.THREAD)
                        // 超时时间
                        .withExecutionTimeoutInMilliseconds(hystrixConfig.get().getTimeoutInMilliseconds())
                        .withExecutionIsolationThreadInterruptOnTimeout(true)
                        .withExecutionTimeoutEnabled(true));
        new HystrixCommand<Object>(setter){

            @Override
            protected Object run() throws Exception {
                route(gatewayContext,hystrixConfig).get();
                return null;
            }

            @Override
            protected Object getFallback(){
                gatewayContext.setResponse(hystrixConfig);
                gatewayContext.written();
                log.warn("服务熔断");
                return null;
            }
        }.execute();

    }

    private static Optional<Rule.HystrixConfig> getHystrixConfig(GatewayContext gatewayContext) {
        Rule rule = gatewayContext.getRule();
        Optional<Rule.HystrixConfig> hystrixConfig = rule.getHystrixConfigs().stream()
                .filter(c-> StringUtils.equals(c.getPath(),gatewayContext.getRequest().getPath()))
                .findFirst();
        return hystrixConfig;
    }

    private CompletableFuture<Response> route(GatewayContext gatewayContext,Optional<Rule.HystrixConfig> hystrixConfig){
        Request request = gatewayContext.getRequest().build();

        // 发送消息
        CompletableFuture<Response> future = AsyncHttpHelper.getInstance().executeRequest(request);

        // 拿到是双异步还是单异步的模式
        boolean whenComplete = ConfigLoader.getConfig().isWhenComplete();

        if(whenComplete){
            future.whenComplete((response, throwable) -> {
                complete(request,response,throwable,gatewayContext, hystrixConfig);
            });
        }else{
            // 需要在另外一个线程执行
            future.whenCompleteAsync((response, throwable) -> {
                complete(request,response,throwable,gatewayContext, hystrixConfig);
            });
        }
        return future;
    }


    /**
     * 释放request 记录response
     * @param request
     * @param response
     * @param throwable
     * @param gatewayContext
     */
    private void complete(Request request, Response response, Throwable throwable, GatewayContext gatewayContext, Optional<Rule.HystrixConfig> hystrixConfig) {
        try {

            // 重试
            Rule rule = gatewayContext.getRule();
            int currentRetryTimes = gatewayContext.getCurrentRetryTimes();
            int confRetryTimes = rule.getRetryConfig().getTimes();

            if((throwable instanceof  TimeoutException || throwable instanceof IOException)
                    && currentRetryTimes <= confRetryTimes
                    && hystrixConfig.isEmpty()){
                doRetry(gatewayContext,currentRetryTimes);
                return;
            }


            if(Objects.nonNull(throwable)){
                String url = request.getUrl();
                if(throwable instanceof TimeoutException){
                    log.warn("complete time out {}",url);
                    gatewayContext.setThrowable(new ResponseException(ResponseCode.REQUEST_TIMEOUT));
                    gatewayContext.setResponse(GatewayResponse.buildGatewayResponse(ResponseCode.REQUEST_TIMEOUT));
                }else{
                    gatewayContext.setThrowable(new ConnectException(throwable,
                            gatewayContext.getUniqueId(),
                            url,ResponseCode.HTTP_RESPONSE_ERROR));
                    gatewayContext.setResponse(GatewayResponse.buildGatewayResponse(ResponseCode.HTTP_RESPONSE_ERROR));
                }
            }else{
                gatewayContext.setResponse(GatewayResponse.buildGatewayResponse(response));
            }
        } catch (Throwable t) {
            gatewayContext.setThrowable(new ResponseException(ResponseCode.INTERNAL_ERROR));
            log.error("complete error",t);
        } finally{
            gatewayContext.written();
            ResponseHelper.writeResponse(gatewayContext);
        }
    }

    private void doRetry(GatewayContext gatewayContext, int currentRetryTimes)  {
        System.out.println("当前重试次数"+currentRetryTimes);
        gatewayContext.setCurrentRetryTimes(currentRetryTimes + 1);
        try {
            doFilter(gatewayContext);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
