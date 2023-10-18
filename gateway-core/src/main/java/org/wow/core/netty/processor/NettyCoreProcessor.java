package org.wow.core.netty.processor;

import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.util.ReferenceCountUtil;
import io.netty.util.Timeout;
import lombok.extern.slf4j.Slf4j;
import org.asynchttpclient.Request;
import org.asynchttpclient.Response;
import org.wow.common.enums.ResponseCode;
import org.wow.common.exception.BaseException;
import org.wow.common.exception.ConnectException;
import org.wow.common.exception.ResponseException;
import org.wow.core.ConfigLoader;
import org.wow.core.context.GatewayContext;
import org.wow.core.filter.FilterFactory;
import org.wow.core.filter.GatewayFilterChainFactory;
import org.wow.core.helper.AsyncHttpHelper;
import org.wow.core.helper.RequestHelper;
import org.wow.core.helper.ResponseHelper;
import org.wow.core.netty.HttpRequestWrapper;
import org.wow.core.response.GatewayResponse;

import java.lang.ref.Reference;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeoutException;

/**
 * @program: api-gateway
 * @description: nettyProcessor实现
 *
 * 串行
 *
 * 步骤
 * 1,定义接口
 * 2,最小可用/最核心版本实现
 * 3,路由函数实现
 * 4,获取异步配置，实现complete方法
 * 5,异常处理
 * 6 写回响应信息并释放资源
 *
 * @author: wow
 * @create: 2023-09-27 15:13
 **/
@Slf4j
public class NettyCoreProcessor implements NettyProcessor{

    private FilterFactory filterFactory = GatewayFilterChainFactory.getInstance();
    @Override
    public void process(HttpRequestWrapper wrapper) {
        FullHttpRequest request = wrapper.getRequest();
        ChannelHandlerContext ctx = wrapper.getCtx();

        try {
            GatewayContext gatewayContext = RequestHelper.doContext(request, ctx);

            /**
             * 开始执行过滤器逻辑!!!
             */
            filterFactory.buildFilterChain(gatewayContext).doFilter(gatewayContext);


            /**
             * 调用httpclient执行转发
             */
//            route(gatewayContext);
        } catch (BaseException e) {
            log.error("process error {} {}",e.getCode().getCode(),e.getCode().getMessage());
//            System.out.println(e.getCode().getCode());
//            System.out.println(e.getCode().getMessage());
            FullHttpResponse httpResponse = ResponseHelper.getHttpResponse(e.getCode());
            doWriteAndRelease(ctx,request,httpResponse);
        } catch (Exception e){
            log.error("process unknown error",e);
            GatewayResponse gatewayResponse = GatewayResponse.buildGatewayResponse(e.getMessage());
            FullHttpResponse httpResponse = ResponseHelper.getHttpResponse(gatewayResponse);
            doWriteAndRelease(ctx,request,httpResponse);
        }
    }

    @Override
    public void start() {

    }

    @Override
    public void shutdown() {

    }

    private void doWriteAndRelease(ChannelHandlerContext ctx, FullHttpRequest request, FullHttpResponse httpResponse) {
        ctx.writeAndFlush(httpResponse)
                .addListener(ChannelFutureListener.CLOSE); // 释放资源后关闭
        ReferenceCountUtil.release(request);
    }



}
