package org.wow.core;

import com.lmax.disruptor.*;
import lombok.Data;

import java.util.concurrent.ThreadPoolExecutor;

/**
 * @program: api-gateway
 * @description:
 * @author: wow
 * @create: 2023-09-27 10:53
 **/

@Data
public class Config {
    private int port = 8888;

    private int prometheusPort = 18000;

    private String applicationName = "api-gateway";


    // 自己调整
    private String zookeeperRegistryAddress = "192.168.220.131:2181";

    private String nacosRegistryAddress = "127.0.0.1:8848";

    private String registryAddress = zookeeperRegistryAddress;

    private String env = "dev";

    private String register = "zookeeper";

    //netty

    private int eventLoopGroupBossNum = 1;

    private int eventLoopGroupWorkerNum = Runtime.getRuntime().availableProcessors();

    private int maxContentLength = 64 * 1024 * 1024;

    //默认单异步模式
    private boolean whenComplete = true;

    //	Http Async 参数选项：

    //	连接超时时间
    private int httpConnectTimeout = 30 * 1000;

    //	请求超时时间
    private int httpRequestTimeout = 30 * 1000;

    //	客户端请求重试次数
    private int httpMaxRequestRetry = 2;

    //	客户端请求最大连接数
    private int httpMaxConnections = 10000;

    //	客户端每个地址支持的最大连接数
    private int httpConnectionsPerHost = 8000;

    //	客户端空闲连接超时时间, 默认60秒
    private int httpPooledConnectionIdleTimeout = 60 * 1000;


    private String bufferType = "parallel";


    private int bufferSize = 1024 * 16;

    private int processThread = Runtime.getRuntime().availableProcessors()/4;


    private String waitStrategy = "busySpin";



    public WaitStrategy getWaitStrategy(){
        switch(waitStrategy){
            case "blocking": //释放CPU
                return new BlockingWaitStrategy();
            case "busySpin": //不释放CPU 低延迟
                return new BusySpinWaitStrategy();
            case "yielding": //自旋100次
                return new YieldingWaitStrategy();
            case "sleeping":
                return new SleepingWaitStrategy();
            default:
                return new BlockingWaitStrategy();
        }
    }

    //扩展.......
}
