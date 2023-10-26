package org.wow.core.filter.monitor;

import com.alibaba.nacos.api.naming.pojo.healthcheck.impl.Http;
import com.alibaba.nacos.client.naming.utils.RandomUtils;
import com.sun.net.httpserver.HttpServer;
import io.micrometer.core.instrument.Timer;
import io.micrometer.prometheus.PrometheusConfig;
import io.micrometer.prometheus.PrometheusMeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.wow.core.ConfigLoader;
import org.wow.core.context.GatewayContext;
import org.wow.core.filter.Filter;
import org.wow.core.filter.FilterAspect;

import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.wow.common.constants.FilterConst.*;

/**
 * @program: api-gateway
 * @description:
 * @author: wow
 * @create: 2023-10-04 21:46
 **/
@Slf4j
@FilterAspect(id = MONITOR_END_FILTER_ID,
        name = MONITOR_END_FILTER_NAME,
        order = MONITOR_END_FILTER_ORDER)
public class MonitorEndFilter implements Filter {
    // 普罗米修斯注册表
    private final PrometheusMeterRegistry prometheusMeterRegistry;

    public MonitorEndFilter() {
        this.prometheusMeterRegistry = new PrometheusMeterRegistry(PrometheusConfig.DEFAULT);

        // 暴露接口来提供普罗米修斯指标数据拉去
        try {
            /**
             * 创建一个 HTTP 服务器，并将其绑定到指定的网络地址和端口上，以便监听来自客户端的 HTTP 请求
             * backlog 表示在服务器忙碌时等待处理的请求队列的最大长度 暂且设置为 0
             */
            HttpServer server = HttpServer.create(new InetSocketAddress(ConfigLoader.getConfig().getPrometheusPort()),0);
            server.createContext("/prometheus",exchange -> {
                //获取指定数据的文本内容
                String scrape = prometheusMeterRegistry.scrape();

                //指标数据返回
                exchange.sendResponseHeaders(200,scrape.getBytes().length);
                try(OutputStream os = exchange.getResponseBody()){
                    os.write(scrape.getBytes());
                }
            });

            new Thread(server::start).start();
        } catch (Exception e) {
            throw new RuntimeException("prometheus http server start error", e);
        }
        log.info("prometheus http server start successful, port:{}", ConfigLoader.getConfig().getPrometheusPort());


        // mock
        Executors.newScheduledThreadPool(1000).scheduleAtFixedRate(() -> {
            Timer.Sample sample = Timer.start();
            try {
                Thread.sleep(RandomUtils.nextInt(100));
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            Timer timer = prometheusMeterRegistry.timer("gateway_request",
                    "uniqueId", "backend-http-server:1.0.0",
                    "protocol", "http",
                    "path", "/http-server/ping" + RandomUtils.nextInt(10));
            sample.stop(timer);
        },200, 100, TimeUnit.MILLISECONDS);

    }

    @Override
    public void doFilter(GatewayContext ctx) throws Exception {
        // 用于测量一个网页请求的处理时间
        Timer timer = prometheusMeterRegistry.timer("gateway_request", "UniqueId", ctx.getUniqueId()
                , "protocol", ctx.getProtocol(),
                "path", ctx.getRequest().getPath());
        ctx.getTimerSample().stop(timer);
    }
}
