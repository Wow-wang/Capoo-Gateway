package org.wow.core;

import lombok.extern.slf4j.Slf4j;
import org.wow.core.netty.NettyHttpClient;
import org.wow.core.netty.NettyHttpServer;
import org.wow.core.netty.processor.DisruptorNettyCoreProcessor;
import org.wow.core.netty.processor.NettyCoreProcessor;
import org.wow.core.netty.processor.NettyProcessor;

import static org.wow.common.constants.GatewayConst.BUFFER_TYPE_PARALLEL;

/**
 * @program: api-gateway
 * @description:
 *
 * 步骤
 * 1,实现lifecycle接口
 * 2,封装属性和实现构造方法
 * 3,实现init方法
 * 4,实现start方法
 * 5,实现shutdown方法
 *
 *
 * @author: wow
 * @create: 2023-09-27 16:17
 **/


@Slf4j
public class Container implements LifeCycle{
    private final Config config;

    private NettyProcessor nettyProcessor;
    private NettyHttpClient nettyHttpClient;
    private NettyHttpServer nettyHttpServer;

    public Container(Config config) {
        this.config = config;
        init();
    }

    @Override
    public void init() {
        NettyCoreProcessor nettyCoreProcessor = new NettyCoreProcessor();
        if(BUFFER_TYPE_PARALLEL.equals(config.getBufferType())){
            this.nettyProcessor = new DisruptorNettyCoreProcessor(config, nettyCoreProcessor);
        }else{
             this.nettyProcessor = nettyCoreProcessor;
        }
        this.nettyHttpServer = new NettyHttpServer(config,nettyProcessor);
        this.nettyHttpClient = new NettyHttpClient(config, nettyHttpServer.getEventLoopGroupWorker());
    }

    @Override
    public void start() {
        nettyProcessor.start();
        nettyHttpServer.start();
        nettyHttpClient.start();
        log.info("api gateway started!");
    }

    @Override
    public void shutdown() {
        nettyProcessor.shutdown();
        nettyHttpServer.shutdown();
        nettyHttpClient.shutdown();
    }
}
