package com.wow.gatewaydiverse.zookeeper;

import com.alibaba.fastjson.JSON;
import com.wow.gatewaydiverse.ApiDiscoveryClient;
import com.wow.gatewaydiverse.service.ServiceDefinition;
import com.wow.gatewaydiverse.service.ServiceInstance;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.ZooDefs;
import org.apache.zookeeper.ZooKeeper;
import org.apache.zookeeper.data.Stat;
import org.springframework.beans.factory.annotation.Autowired;

import javax.annotation.PostConstruct;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static com.wow.gatewaydiverse.gatewayConst.GatewayConst.DEFINITION;
import static com.wow.gatewaydiverse.gatewayConst.GatewayConst.INSTANCE;
import static com.wow.gatewaydiverse.gatewayConst.GatewayConst.PATH_SEPARATOR;

/**
 * @program: gateway-diverse
 * @description:
 * @author: wow
 * @create: 2023-10-20 16:55
 **/
@Slf4j
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class ApiZookeeperDiscoveryClient implements ApiDiscoveryClient {
    private String PATH;
    private ZooKeeper zooKeeper;
    private final ApiZookeeperDiscoveryProperties apiZookeeperDiscoveryProperties;

//    private ZooKeeper zooKeeper = apiZookeeperDiscoveryProperties.zooKeeper;

    @PostConstruct
    public void init(){
        PATH = apiZookeeperDiscoveryProperties.getPATH();
        zooKeeper = apiZookeeperDiscoveryProperties.getZooKeeper();
    }

    @Override
    public void registerInstance(ServiceDefinition serviceDefinition, ServiceInstance serviceInstance) {
        try {
            Stat stat;
            stat = zooKeeper.exists(PATH + PATH_SEPARATOR + serviceDefinition.getServiceId() + DEFINITION,null);
            // 更新服务定义
            String definitionPath = PATH + PATH_SEPARATOR + serviceDefinition.getServiceId() + DEFINITION;
            log.info("definitionPath:{}",definitionPath);
            if(stat == null) {
                zooKeeper.create(
                        definitionPath,
                        JSON.toJSONString(serviceDefinition).getBytes(StandardCharsets.UTF_8),
                        ZooDefs.Ids.OPEN_ACL_UNSAFE,
                        CreateMode.PERSISTENT
                );
            }else{
                zooKeeper.setData(
                        definitionPath,
                        JSON.toJSONString(serviceDefinition).getBytes(StandardCharsets.UTF_8),
                        -1
                );
            }
            // 在服务定义下面列表中创建服务实例
            String instancePath = PATH + PATH_SEPARATOR + serviceDefinition.getServiceId() + DEFINITION + PATH_SEPARATOR + serviceInstance.getServiceInstanceId() + INSTANCE;
            log.info("instancePath:{}",instancePath);
            stat = zooKeeper.exists(instancePath,null);
            if(stat == null){
                String result = zooKeeper.create(
                        instancePath,
                        JSON.toJSONString(serviceInstance).getBytes(StandardCharsets.UTF_8),
                        ZooDefs.Ids.OPEN_ACL_UNSAFE,
                        CreateMode.EPHEMERAL
                );
            }
        } catch (KeeperException e) {
            throw new RuntimeException(e);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void deregisterInstance(ServiceDefinition serviceDefinition, ServiceInstance serviceInstance) {
        try {
            Stat stat;
            String servicePath = PATH + PATH_SEPARATOR + serviceDefinition.getServiceId() + DEFINITION;
            stat = zooKeeper.exists(servicePath,null);
            if(stat != null){
                String instancePath = servicePath + PATH_SEPARATOR + serviceInstance.getServiceInstanceId() + INSTANCE;
                log.info("instancePath:{}",instancePath);
                stat = zooKeeper.exists(instancePath,null);
                if(stat != null){
                    zooKeeper.delete(instancePath,-1);
                }
                // 删除结束后 记录当前是否还有实例 如果无实例 直接进行删除
                List<String> children = zooKeeper.getChildren(servicePath, false);
                if(children.isEmpty()){
                    zooKeeper.delete(servicePath,-1);
                }
            }
        } catch (KeeperException e) {
            throw new RuntimeException(e);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}
