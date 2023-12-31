package org.wow.core.filter.router;

import com.alibaba.fastjson.JSON;
import com.alibaba.nacos.common.utils.BiConsumer;
import com.alibaba.nacos.common.utils.StringUtils;
import com.github.benmanes.caffeine.cache.Cache;
import com.netflix.hystrix.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.skywalking.apm.toolkit.trace.TraceCrossThread;
import org.asynchttpclient.Request;
import org.asynchttpclient.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wow.common.config.Rule;
import org.wow.common.config.ServiceInstance;
import org.wow.common.constants.FilterConst;
import org.wow.common.enums.ResponseCode;
import org.wow.common.exception.ConnectException;
import org.wow.common.exception.NotFoundException;
import org.wow.common.exception.ResponseException;
import org.wow.core.ConfigLoader;
import org.wow.core.context.GatewayContext;
import org.wow.core.filter.Filter;
import org.wow.core.filter.FilterAspect;
import org.wow.core.filter.GatewayFilterChainFactory;
import org.wow.core.filter.loadbalance.LeastActiveLoadBalanceRule;
import org.wow.core.filter.loadbalance.RoundRobinLoadBalanceRule;
import org.wow.core.helper.AsyncHttpHelper;
import org.wow.core.helper.ResponseHelper;
import org.wow.core.netty.datasourece.Connection;
import org.wow.core.netty.datasourece.unpooled.UnpooledDataSource;
import org.wow.core.request.GatewayRequest;
import org.wow.core.response.GatewayResponse;
import sun.misc.Unsafe;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.LongAdder;

import static org.wow.common.constants.BasicConst.SERVEROVERTIME;
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
    private static Logger accessLog = LoggerFactory.getLogger("accessLog");
    /**
     * 负责接收原始请求，将其发送到后端服务器（通过 AsyncHttpHelper 执行异步HTTP请求）
     * 并在请求完成时触发回调来处理结果。
     * 根据配置，它可以使用双异步或单异步模式来执行回调，以实现更灵活的异步处理
     * @param gatewayContext
     */

  

    @Override
    public void doFilter(GatewayContext gatewayContext) throws Exception {
        log.info("route");
        String protocol = gatewayContext.getRule().getProtocol();
        if(protocol.equals("dubbo")){
            completeDubbo(gatewayContext);
            return;
        }
        //使用java8 lambda拿到
        Optional<Rule.HystrixConfig> hystrixConfig = getHystrixConfig(gatewayContext);
        if(hystrixConfig.isPresent()){
            routeWithHystrix(gatewayContext,hystrixConfig);
        }else{
            route(gatewayContext,hystrixConfig);
        }


    }

    private void routeWithSentinel(GatewayContext gatewayContext, Optional<Rule.HystrixConfig> hystrixConfig){

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
                        .withExecutionTimeoutEnabled(true)
                        // 设置半开启状态的恢复时间为5秒
                .withCircuitBreakerSleepWindowInMilliseconds(5000));

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
                complete(request, response, throwable, gatewayContext,hystrixConfig);
            });
        }else{
            // 需要在另外一个线程执行
            future.whenCompleteAsync((response, throwable) -> {
                complete(request, response, throwable, gatewayContext,hystrixConfig);
            });
        }
        return future;
    }

    private void routeHttpToDubbo(GatewayContext gatewayContext,Optional<Rule.HystrixConfig> hystrixConfig){

    }

    /**
     * 可有可无
     */
//    @TraceCrossThread
//    public class CompleteBiConsumer implements BiConsumer<Response,Throwable>{
//        private Request request;
//        private Response response;
//
//        private Throwable throwable;
//        private GatewayContext gatewayContext;
//
//        private Optional<Rule.HystrixConfig> hystrixConfig;
//
//        public CompleteBiConsumer(Request request, GatewayContext gatewayContext, Optional<Rule.HystrixConfig> hystrixConfig) {
//            this.request = request;
//            this.gatewayContext = gatewayContext;
//            this.hystrixConfig = hystrixConfig;
//        }
//
//        @Override
//        public void accept(Response response, Throwable throwable) {
//            this.response = response;
//            this.throwable = throwable;
//            this.run();
//        }
//
//        public void run(){
//            //skywalking 会通过 @TraceCrossThread会增强run方法
//            complete(request,response,throwable,gatewayContext, hystrixConfig);
//        }
//    }

    /**
     * 释放request 记录response
     * @param request
     * @param response
     * @param throwable
     * @param gatewayContext
     */
    private void complete(Request request, Response response, Throwable throwable, GatewayContext gatewayContext, Optional<Rule.HystrixConfig> hystrixConfig) {
        try {

            log.info("complete");
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
                // 真正的无错误码response
                gatewayContext.setResponse(GatewayResponse.buildGatewayResponse(response));
            }
        } catch (Throwable t) {
            gatewayContext.setThrowable(new ResponseException(ResponseCode.INTERNAL_ERROR));
            log.error("complete error",t);
        } finally{
            gatewayContext.written();
            ResponseHelper.writeResponse(gatewayContext);

            updateActive(gatewayContext);
            accessLog.info("{} {} {} {} {} {} {}",
                    System.currentTimeMillis() - gatewayContext.getRequest().getBeginTime(),
                    gatewayContext.getRequest().getClientIp(),
                    gatewayContext.getRequest().getUniqueId(),
                    gatewayContext.getRequest().getMethod(),
                    gatewayContext.getRequest().getPath(),
                    gatewayContext.getResponse().getHttpResponseStatus().code(),
                    gatewayContext.getResponse().getFutureResponse().getResponseBodyAsBytes().length);

        }
    }

    private void updateActive(GatewayContext gatewayContext) {
        Rule rule = gatewayContext.getRule();
        Set<Rule.FilterConfig> filterConfigs = gatewayContext.getRule().getFilterConfigs();
        Iterator iterator = filterConfigs.iterator();
        Rule.FilterConfig filterConfig;
        while(iterator.hasNext()){
            filterConfig = (Rule.FilterConfig)iterator.next();
            if(filterConfig == null){
                continue;
            }
            String filterId = filterConfig.getId();
            if(filterId.equals(LOAD_BALANCE_FILTER_ID)) {
                String config = filterConfig.getConfig();
                if(!org.apache.commons.lang3.StringUtils.isEmpty(config)){
                    Map<String,String> mayTypeMap = JSON.parseObject(config,Map.class);
                    if(mayTypeMap.get(LOAD_BALANCE_KEY).equals(LOAD_BALANCE_STRATEGY_LEAST_ACTIVE)){
                        // 并发数减少
                        LeastActiveLoadBalanceRule.getInstance(rule.getServiceId()).getActiveCache()
                                .get(gatewayContext.getRequest().getModifyHost(),k->{return new LongAdder();})
                                .decrement();
                    }
                }
            }
        }
    }

    private void doRetry(GatewayContext gatewayContext, int currentRetryTimes)  {
        System.out.println("当前重试次数"+currentRetryTimes);
        gatewayContext.setCurrentRetryTimes(currentRetryTimes + 1);
        try {
            chooseNewInstance(gatewayContext);
            doFilter(gatewayContext);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void chooseNewInstance(GatewayContext gatewayContext) {
        String serviceId = gatewayContext.getRule().getServiceId();
        RoundRobinLoadBalanceRule loadBalance = RoundRobinLoadBalanceRule.getInstance(serviceId);
        ServiceInstance serviceInstance = loadBalance.choose(serviceId, gatewayContext.isGray());
        GatewayRequest request = gatewayContext.getRequest();
        if(serviceInstance != null && request != null){
            String host = serviceInstance.getIp() + ":" + serviceInstance.getPort();
            request.setModifyHost(host);
        }else{
            log.warn("No instance available for : {}" , serviceId);
            throw new NotFoundException(ResponseCode.SERVICE_INSTANCE_NOT_FOUND);
        }
    }

    private void completeDubbo(GatewayContext gatewayContext) {
        // 构造请求： url 请求方法 参数类型 参数
        GatewayRequest request = gatewayContext.getRequest();
        String url = request.getModifyHost();
        String interfaceName = request.getRpcInterfaceName();
        Connection connection = UnpooledDataSource.getInstance().getConnection(url,interfaceName);
        String rpcMethod = request.getRpcMethod();
        String[] parameterTypes = request.getParameterTypes();
        String[] arguments = request.getArguments();
        Object result = connection.execute(gatewayContext.getRequest().getRpcMethod(), parameterTypes, arguments);

        // 执行重试流程
        Rule rule = gatewayContext.getRule();
        int currentRetryTimes = gatewayContext.getCurrentRetryTimes();
        int confRetryTimes = rule.getRetryConfig().getTimes();
        if(result == SERVEROVERTIME && currentRetryTimes <= confRetryTimes){
            doRetry(gatewayContext,currentRetryTimes);
            return;
        }

        // 输出最终结果
        gatewayContext.setResponse(GatewayResponse.buildGatewayResponse(result));
        gatewayContext.written();
        ResponseHelper.writeResponse(gatewayContext);

        // 更新活跃度
        updateActive(gatewayContext);

        accessLog.info("{} {} {} {} {} {} {}",
                System.currentTimeMillis() - gatewayContext.getRequest().getBeginTime(),
                gatewayContext.getRequest().getClientIp(),
                gatewayContext.getRequest().getUniqueId(),
                gatewayContext.getRequest().getMethod(),
                gatewayContext.getRequest().getPath(),
                gatewayContext.getResponse().getHttpResponseStatus().code(),
                gatewayContext.getResponse().getFutureResponse().getResponseBodyAsBytes().length);
    }
}
