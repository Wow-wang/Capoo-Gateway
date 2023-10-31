package com.wow.gatewaydiverse.zookeeper;

import com.wow.gatewaydiverse.ApiProperties;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.apache.zookeeper.*;
import org.apache.zookeeper.data.Stat;
import org.springframework.beans.factory.annotation.Autowired;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static com.wow.gatewaydiverse.gatewayConst.GatewayConst.PATH_SEPARATOR;

/**
 * @program: gateway-diverse
 * @description:
 * @author: wow
 * @create: 2023-10-20 16:50
 **/
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class ApiZookeeperDiscoveryProperties {
    private final ApiProperties apiProperties;
    @Getter
    private  ZooKeeper zooKeeper;
    @Getter
    private String PATH = "/api-gateway-service";

    @PostConstruct
    public void init(){
        String registerAddress = "192.168.88.128" + ":2181";

        try {
            zooKeeper = new ZooKeeper(registerAddress,40000,null); Stat stat;
            // 创建service目录
            stat = zooKeeper.exists(PATH,null);
            if(stat == null){
                String result = zooKeeper.create(
                        PATH,
                        new byte[0],
                        ZooDefs.Ids.OPEN_ACL_UNSAFE,
                        CreateMode.PERSISTENT
                );
            }
            PATH = PATH + PATH_SEPARATOR + apiProperties.getEnv();
            stat = zooKeeper.exists(PATH,null);
            if(stat == null){
                String result = zooKeeper.create(
                        PATH,
                        new byte[0],
                        ZooDefs.Ids.OPEN_ACL_UNSAFE,
                        CreateMode.PERSISTENT
                );
            }
            ScheduledExecutorService scheduledThreadPool = Executors.newScheduledThreadPool(1);
            scheduledThreadPool.scheduleWithFixedDelay(()->{
                        try {
                            zooKeeper.exists(PATH,null);
                        } catch (KeeperException e) {
                            throw new RuntimeException(e);
                        } catch (InterruptedException e) {
                            throw new RuntimeException(e);
                        }
                    },0,
                    5,
                    TimeUnit.SECONDS
            );
        } catch (IOException | KeeperException | InterruptedException e) {
            throw new RuntimeException(e);
        }

    }
}
