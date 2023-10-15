package org.wow.core.netty.processor;

import com.alibaba.nacos.api.naming.pojo.healthcheck.impl.Http;
import com.lmax.disruptor.dsl.ProducerType;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpUtil;
import lombok.extern.slf4j.Slf4j;
import org.wow.common.enums.ResponseCode;
import org.wow.core.disruptor.EventListener;
import org.wow.core.disruptor.ParallelQueueHandler;
import org.wow.core.helper.ResponseHelper;
import org.wow.core.netty.HttpRequestWrapper;
import org.wow.core.Config;

/**
 * @program: api-gateway
 * @description: Disruptor流程处理类
 * @author: wow
 * @create: 2023-10-07 14:49
 **/

@Slf4j
public class DisruptorNettyCoreProcessor implements NettyProcessor {
    private static final String THREAD_NAME_PREFIX = "gateway-queue-";

    private Config config;

    private NettyCoreProcessor nettyCoreProcessor;

    private ParallelQueueHandler<HttpRequestWrapper> parallelQueueHandler;


    public DisruptorNettyCoreProcessor(Config config, NettyCoreProcessor nettyCoreProcessor) {
        this.config = config;
        this.nettyCoreProcessor = nettyCoreProcessor;
        ParallelQueueHandler.Builder<HttpRequestWrapper> builder = new ParallelQueueHandler.Builder<HttpRequestWrapper>()
                .setBufferSize(config.getBufferSize())
                .setThreads(config.getProcessThread())
                .setProducerType(ProducerType.MULTI)
                .setNamePrefix(THREAD_NAME_PREFIX)
                .setWaitStrategy(config.getWaitStrategy());

        BatchEventListenerProcess batchEventListenerProcess = new BatchEventListenerProcess();
        builder.setListener(batchEventListenerProcess);
        this.parallelQueueHandler = builder.build();

    }

    @Override
    public void process(HttpRequestWrapper wrapper) {
        this.parallelQueueHandler.add(wrapper);
    }



    public class BatchEventListenerProcess implements EventListener<HttpRequestWrapper>{

        @Override
        public void onEvent(HttpRequestWrapper event) {
            nettyCoreProcessor.process(event);
        }

        @Override
        public void onException(Throwable ex, long sequence, HttpRequestWrapper event) {
            HttpRequest request = event.getRequest();
            ChannelHandlerContext ctx = event.getCtx();
            try {
                log.error("BatchEventListenerProcessor onException 请求写回失败 request:{} errMsg:{}",request,ex.getMessage(),ex);

                // 构建访问对象
                FullHttpResponse fullHttpResponse = ResponseHelper.getHttpResponse(ResponseCode.INTERNAL_ERROR);
                if(!HttpUtil.isKeepAlive(request)){
                    ctx.writeAndFlush(fullHttpResponse).addListener(ChannelFutureListener.CLOSE);
                }else{
                    fullHttpResponse.headers().set(HttpHeaderNames.CONNECTION,HttpHeaderNames.KEEP_ALIVE);
                    ctx.writeAndFlush(fullHttpResponse);
                }
            } catch (Exception e) {
                log.error("BatchEventListenerProcessor onException 请求写回失败 request:{} errMsg:{}",request,e.getMessage(),e);
                throw new RuntimeException(e);
            }
        }
    }

    @Override
    public void start() {
        parallelQueueHandler.start();
    }

    @Override
    public void shutdown() {
        parallelQueueHandler.shutDown();
    }
}
