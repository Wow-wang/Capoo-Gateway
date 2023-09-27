package org.wow.core;

import lombok.Data;

/**
 * @program: api-gateway
 * @description:
 * @author: wow
 * @create: 2023-09-27 10:53
 **/

@Data
public class Config {
    private int port = 8888;

    // 微服务服务发现
    private String applicationName = "wow-api-gateway";

    private String registryAddress = "127.0.0.1:8848";

    // 环境
    private String env = "dev";

    // netty
    private int eventLoopGroupBossNum = 1;
    private int eventLoopGroupWorkerNum = Runtime.getRuntime().availableProcessors();
    private int maxContentLength = 64 * 1024 * 1024;

    //默认单异步模式 单双异步配置
    private boolean whenComplete = true;

    //等待扩展


}
