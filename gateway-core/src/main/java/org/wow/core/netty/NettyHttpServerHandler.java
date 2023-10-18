package org.wow.core.netty;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.http.FullHttpRequest;
import org.wow.core.netty.processor.NettyProcessor;

/**
 * @program: api-gateway
 * @description: nettyHttpServerHandler实现
 *
 * 步骤
 * 1,继承channelInboundHandlerAdapter
 * 2,实现channelRead
 * 3,把逻辑委托给nettyProcessor
 *
 * @author: wow
 * @create: 2023-09-27 14:58
 **/

public class NettyHttpServerHandler extends ChannelInboundHandlerAdapter {

    private final NettyProcessor nettyProcessor;

    public NettyHttpServerHandler(NettyProcessor nettyProcessor){
        this.nettyProcessor = nettyProcessor;
    }


    /**
     * 当有数据从通道中读取时，Netty 会自动调用这个方法，并将读取的数据传递给它
     * @param ctx
     * @param msg
     * @throws Exception
     */
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception{
        FullHttpRequest request = (FullHttpRequest) msg;
        // 对message处理 并发送
        HttpRequestWrapper wrapper = new HttpRequestWrapper();
        wrapper.setRequest(request);
        wrapper.setCtx(ctx);

        nettyProcessor.process(wrapper);

    }
}
