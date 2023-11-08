package org.wow.core.netty;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.epoll.Epoll;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.epoll.EpollServerSocketChannel;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.ssl.SslHandler;
import io.netty.util.concurrent.DefaultThreadFactory;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.wow.common.utils.RemotingUtil;
import org.wow.core.Config;
import org.wow.core.LifeCycle;
import org.wow.core.netty.processor.NettyProcessor;


import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import java.io.FileInputStream;
import java.net.InetSocketAddress;
import java.security.KeyStore;

/**
 * @program: api-gateway
 * @description: HTTPServer用于接受和处理客户端的HTTP请求
 *  nettyhttpserver实现
 *
 *  步骤
 *  1,封装属性
 *  2,实现构造方法
 *  3,实现init方法
 *  4,epoll优化
 *  5,实现start方法
 *  6,实现shutdown方法
 *
 * @author: wow
 * @create: 2023-09-27 14:41
 **/

@Slf4j
public class NettyHttpServer implements LifeCycle {
    private final Config config;

    /**
     * ServerBootstrap 是 Netty 框架中用于配置和启动服务器的类
     */
    private ServerBootstrap serverBootstrap;


    private EventLoopGroup eventLoopGroupBoss;

    @Getter
    private EventLoopGroup eventLoopGroupWorker;
    private final NettyProcessor nettyProcessor;



    public NettyHttpServer(Config config, NettyProcessor nettyProcessor){
        this.config = config;
        this.nettyProcessor = nettyProcessor;
        init();
    }
    @Override
    public void init() {
        if(useEpoll()) {
            this.serverBootstrap = new ServerBootstrap();
            this.eventLoopGroupBoss = new EpollEventLoopGroup(config.getEventLoopGroupBossNum(),new DefaultThreadFactory("netty-boss-nio"));
            this.eventLoopGroupWorker = new EpollEventLoopGroup(config.getEventLoopGroupWorkerNum(),new DefaultThreadFactory("netty-worker-nio"));
        }else{
            this.serverBootstrap = new ServerBootstrap();
            this.eventLoopGroupBoss = new NioEventLoopGroup(config.getEventLoopGroupBossNum(),new DefaultThreadFactory("netty-boss-nio"));
            this.eventLoopGroupWorker = new NioEventLoopGroup(config.getEventLoopGroupWorkerNum(),new DefaultThreadFactory("netty-worker-nio"));
        }

    }

    public boolean useEpoll(){
        return RemotingUtil.isLinuxPlatform() && Epoll.isAvailable();
    }

    @Override
    public void start() {
        // Netty服务器会一直运行，直到显式地停止它或发生不可恢复的错误
        this.serverBootstrap
                .group(eventLoopGroupBoss,eventLoopGroupWorker)
                .channel(useEpoll() ? EpollServerSocketChannel.class : NioServerSocketChannel.class)
                .localAddress(new InetSocketAddress(config.getPort()))
                // 用于配置服务器的处理器链（ChannelPipeline）
                .childHandler(new ChannelInitializer<Channel>() {
                    @Override
                    protected void initChannel(Channel ch) throws Exception {

                        if(config.isStartSSL()) {
                            // 添加sslhandler
                            // 创建 SSL 上下文
                            char[] passArray = "hsc123".toCharArray(); //jks密码
                            SSLContext sslContext = SSLContext.getInstance("TLSv1");
                            KeyStore ks = KeyStore.getInstance("JKS");

                            //加载keytool 生成的文件
                            FileInputStream inputStream = new FileInputStream("f:/local.jks");
                            ks.load(inputStream, passArray);

                            // 初始化 KeyManagerFactory
                            KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
                            kmf.init(ks, passArray);

                            // 使用 KeyManagers 初始化 SSL 上下文
                            sslContext.init(kmf.getKeyManagers(), null, null);
                            inputStream.close();

                            // 创建 SSL 引擎并设置为服务器模式
                            SSLEngine sslEngine = sslContext.createSSLEngine();
                            sslEngine.setUseClientMode(false);

                            ch.pipeline().addLast(new SslHandler(sslEngine));
                        }

                        ch.pipeline().addLast(
                                /**
                                 * 用于处理HTTP请求和响应的编码和解码
                                 */
                                new HttpServerCodec(), //http编码器

                                /**
                                 * 将 HTTP 消息的碎片聚合成完整的 HTTP 请求或响应
                                 */
                                new HttpObjectAggregator(config.getMaxContentLength()),// http聚合

                                /**
                                 * 负责处理连接的生命周期事件、异常情况和空闲连接
                                 */
                                new NettyServerConnectManagerHandler(),

                                /**
                                 * 处理 HTTP 请求的业务逻辑
                                 */
                                new NettyHttpServerHandler(nettyProcessor)
                                // 处理Rpc 请求的业务逻辑
//                                new ProtocolDataHandler()
                        );
                    }
                });
        try{
            this.serverBootstrap.bind().sync();
            log.info("server startup on port{}",config.getPort());
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void shutdown() {
        if(eventLoopGroupBoss != null){
            eventLoopGroupBoss.shutdownGracefully();
        }
        if(eventLoopGroupWorker != null){
            eventLoopGroupWorker.shutdownGracefully();
        }
    }
}
