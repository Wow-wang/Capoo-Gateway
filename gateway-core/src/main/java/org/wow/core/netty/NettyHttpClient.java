package org.wow.core.netty;

import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.DefaultAddressedEnvelope;
import io.netty.channel.EventLoopGroup;
import lombok.extern.slf4j.Slf4j;
import org.asynchttpclient.AsyncHttpClient;
import org.asynchttpclient.DefaultAsyncHttpClient;
import org.asynchttpclient.DefaultAsyncHttpClientConfig;
import org.wow.core.LifeCycle;
import org.wow.core.Config;
import org.wow.core.helper.AsyncHttpHelper;

import java.io.IOException;

/**
 * @program: api-gateway
 * @description: HTTPClient用于创建和发送HTTP请求到服务器
 *
 * 步骤
 * 1,实现lifecycle接口
 * 2,封装属性
 * 3,实现init方法
 * 4,实现start方法
 * 5,实现shutdown方法
 *
 * @author: wow
 * @create: 2023-09-27 16:05
 **/

@Slf4j
public class NettyHttpClient implements LifeCycle {
    private final Config config;
    private final EventLoopGroup eventLoopGroup;

    /**
     *
     */
    private AsyncHttpClient asyncHttpClient;

    public NettyHttpClient(Config config, EventLoopGroup eventLoopGroup) {
        this.config = config;
        this.eventLoopGroup = eventLoopGroup;
        init();
    }


    /**
     * 初始化Async httpclient
     */
    @Override
    public void init() {
        DefaultAsyncHttpClientConfig.Builder builder = new DefaultAsyncHttpClientConfig.Builder()
                .setEventLoopGroup(eventLoopGroup)
                .setConnectTimeout(config.getHttpConnectTimeout())
                .setRequestTimeout(config.getHttpRequestTimeout())
                .setMaxRedirects(config.getHttpMaxRequestRetry())
                .setAllocator(PooledByteBufAllocator.DEFAULT) //池化的byteBuf分配器 提升性能
                .setCompressionEnforced(true)
                .setMaxConnections(config.getHttpMaxConnections())
                .setMaxConnectionsPerHost(config.getHttpConnectionsPerHost())
                .setPooledConnectionIdleTimeout(config.getHttpPooledConnectionIdleTimeout());
        this.asyncHttpClient = new DefaultAsyncHttpClient(builder.build());
    }

    /**
     * 装载
     */
    @Override
    public void start() {
        AsyncHttpHelper.getInstance().initialized(asyncHttpClient);
    }

    /**
     * 拆卸
     */
    @Override
    public void shutdown() {
        if(asyncHttpClient != null){
            try{
                this.asyncHttpClient.close();
            }catch (IOException e){
                log.error("NettyHttpClient shutdown error",e);
            }
        }
    }
}
