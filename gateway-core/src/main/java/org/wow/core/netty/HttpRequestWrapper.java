package org.wow.core.netty;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.http.FullHttpRequest;
import lombok.Data;

/**
 * @program: api-gateway
 * @description:
 * @author: wow
 * @create: 2023-09-27 15:06
 **/

@Data
public class HttpRequestWrapper  {
    /**
     * FullHttpRequest 是 Netty 框架中的一个类，用于表示完整的 HTTP 请求，包括请求头、请求体和其他相关信息
     */
    private FullHttpRequest request;
    /**
     * ChannelHandlerContext 是 Netty 框架中的一个关键组件，用于表示一个处理器在处理数据时的上下文信息
     */
    private ChannelHandlerContext ctx;
}
