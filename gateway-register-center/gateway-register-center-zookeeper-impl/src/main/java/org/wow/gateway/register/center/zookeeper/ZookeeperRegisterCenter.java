package org.wow.gateway.register.center.zookeeper;

import com.alibaba.fastjson.JSON;
import lombok.extern.slf4j.Slf4j;
import org.apache.zookeeper.*;
import org.apache.zookeeper.data.Stat;
import org.wow.common.config.ServiceDefinition;
import org.wow.common.config.ServiceInstance;
import org.wow.gateway.register.center.api.RegisterCenter;
import org.wow.gateway.register.center.api.RegisterCenterListener;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.*;

import static org.wow.common.constants.BasicConst.PATH_SEPARATOR;
import static org.wow.common.constants.CenterConst.*;

/**
 * @program: api-gateway
 * @description:
 * @author: wow
 * @create: 2023-10-10 10:38
 **/

@Slf4j
public class ZookeeperRegisterCenter implements RegisterCenter {
    private ZooKeeper zooKeeper;

    private static final String PATH = "/api-gateway-service";

    private String registerAddress;
    private String env;

    //监听器列表
    /**
     * CopyOnWriteArrayList 是线程安全的，可以在多线程环境下安全地进行读取操作，而不需要额外的同步措施。这使得它适用于读多写少的场景
     */
    private List<RegisterCenterListener> registerCenterListenerList  = new CopyOnWriteArrayList<>();
    @Override
    public void init(String registerAddress, String env) {
        this.registerAddress = registerAddress;
        this.env = env;
        try {
            zooKeeper = new ZooKeeper(ZOOKEEPER_REGISTER_ADDRESS,40000,null);
            Stat stat;
            // 创建service目录
            stat = zooKeeper.exists(PATH,null);

            if(stat == null){
               String result = zooKeeper.create(
                       PATH,
                       new byte[0],
                       ZooDefs.Ids.OPEN_ACL_UNSAFE,
                       CreateMode.PERSISTENT
               );
               // 注册过的用户必须通过addAuthInfo才能操作节点，参考命令行 addauth
                // zooKeeper.addAuthInfo("digest", "user1:123456a".getBytes());

            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (KeeperException e) {
        throw new RuntimeException(e);
        } catch (InterruptedException e) {
        throw new RuntimeException(e);
        }


    }

    @Override
    public void register(ServiceDefinition serviceDefinition, ServiceInstance serviceInstance) {
        // 查看当前目录下面是否有 serviceDefinition
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
    public void deregister(ServiceDefinition serviceDefinition, ServiceInstance serviceInstance) {
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

    @Override
    public void subscribeAllServices(RegisterCenterListener registerCenterListener) {
        registerCenterListenerList.add(registerCenterListener);
        doSubscribeAllServices();

        // 可能有新服务加入 所以需要一个定时任务来检查 也是发送心跳
        ScheduledExecutorService scheduledThreadPool = Executors.newScheduledThreadPool(1);

        // 每次任务开始执行时 安排一个任务按照固定的时间间隔执行
        scheduledThreadPool.scheduleWithFixedDelay(()->doSubscribeAllServices(),
                10,10, TimeUnit.SECONDS);
    }

    private void doSubscribeAllServices(){
        // 获取主node 下面有多少个node 每个node是一个服务定义
        try {
            // 子节点只能感知子节点 不能感知子节点的子节点变化
            List<String> childrenDefinition = zooKeeper.getChildren(PATH, null);
            // child是serviceId
            for(String definition : childrenDefinition){
                byte[] data = zooKeeper.getData(PATH + PATH_SEPARATOR + definition, false, new Stat());
                // 定位不到serviceDefinition直接跳过
                if(data == null){
                    continue;
                }
                String definitionString = new String(data, StandardCharsets.UTF_8);
//                System.out.println(definitionString);
                // 循环遍历服务node 查看下面的node 每个是一个实例
                ServiceDefinition serviceDefinition = JSON.parseObject(definitionString).toJavaObject(ServiceDefinition.class);
//                log.info("读取到的serviceDefinition : {}",serviceDefinition);
                Set<ServiceInstance> set = new HashSet<>();
                List<String> childrenInstance = zooKeeper.getChildren(PATH + PATH_SEPARATOR + definition, false);
                // 如果服务实例为空 删除当前服务定义
                // 不行 也需要上传空set覆盖原来的

                if(childrenInstance.isEmpty()){
                    // 相比于走下面for循环 增加delete操作
                    zooKeeper.delete(PATH + PATH_SEPARATOR + definition,-1);
                    registerCenterListenerList.stream()
                            .forEach(l->l.onChange(serviceDefinition,null));
                    continue;
                }


                for(String instance : childrenInstance){
                    byte[] dataInstance = zooKeeper.getData(PATH + PATH_SEPARATOR + definition + PATH_SEPARATOR + instance, false, new Stat());
                    if(dataInstance == null){
                        continue;
                    }
                    String instanceString = new String(dataInstance,StandardCharsets.UTF_8);
//                    System.out.println(instanceString);
                    ServiceInstance serviceInstance = JSON.parseObject(instanceString).toJavaObject(ServiceInstance.class);
//                    log.info("读取到的serviceInstance : {}",serviceInstance);
                    set.add(serviceInstance);
                }
                registerCenterListenerList.stream()
                        .forEach(l->l.onChange(serviceDefinition,set));
            }

        } catch (KeeperException e) {
            throw new RuntimeException(e);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }





    }
}
