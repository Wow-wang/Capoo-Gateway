package org.wow.gateway.register.center.zookeeper;

import com.alibaba.fastjson.JSON;
import org.apache.curator.RetryPolicy;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.framework.recipes.cache.PathChildrenCache;
import org.apache.curator.framework.recipes.cache.PathChildrenCacheEvent;
import org.apache.curator.framework.recipes.cache.PathChildrenCacheListener;
import org.apache.curator.framework.recipes.cache.TreeCache;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.ZooDefs;
import org.apache.zookeeper.data.Stat;
import org.wow.common.config.DynamicConfigManager;
import org.wow.common.config.ServiceDefinition;
import org.wow.common.config.ServiceInstance;
import org.wow.gateway.register.center.api.RegisterCenter;
import org.wow.gateway.register.center.api.RegisterCenterListener;

import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.wow.common.constants.BasicConst.PATH_SEPARATOR;
import static org.wow.common.constants.CenterConst.*;

/**
 * @program: Capoo-Api-gateway
 * @description:
 * @author: wow
 * @create: 2023-11-07 14:40
 **/

public class CuratorRegisterCenter implements RegisterCenter {
    private CuratorFramework client;
    private String DEVPATH = "/api-gateway-service";
    private String env;

    @Override
    public void init(String registerAddress, String env) {
        this.env = env;
        RetryPolicy retryPolicy = new ExponentialBackoffRetry(1000, 3);
        this.client =
                CuratorFrameworkFactory.newClient(
                        registerAddress,
                        5000,
                        30000,
                        retryPolicy);
        client.start();
        DEVPATH = DEVPATH + "/" + env;
        try {
            if (client.checkExists().forPath(DEVPATH) == null) {
                // 如果路径不存在，创建它
                client.create().creatingParentsIfNeeded().forPath(DEVPATH);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void register(ServiceDefinition serviceDefinition, ServiceInstance serviceInstance) {
        try {
            // 检查服务定义路径是否存在，如果不存在则创建
            String definitionPath = DEVPATH + PATH_SEPARATOR + serviceDefinition.getUniqueId();
            Stat stat = client.checkExists().forPath(definitionPath);

            if (stat == null) {
                client.create().creatingParentsIfNeeded()
                        .withMode(CreateMode.PERSISTENT)
                        .withACL(ZooDefs.Ids.OPEN_ACL_UNSAFE)
                        .forPath(definitionPath, JSON.toJSONString(serviceDefinition).getBytes(StandardCharsets.UTF_8));
            } else {
                client.setData()
                        .forPath(definitionPath, JSON.toJSONString(serviceDefinition).getBytes(StandardCharsets.UTF_8));
            }

            // 创建服务实例路径
            String instancePath = DEVPATH + PATH_SEPARATOR + serviceDefinition.getUniqueId() + PATH_SEPARATOR
                    + serviceInstance.getServiceInstanceId();
            stat = client.checkExists().forPath(instancePath);

            if (stat == null) {
                client.create().creatingParentsIfNeeded()
                        .withMode(CreateMode.EPHEMERAL)
                        .withACL(ZooDefs.Ids.OPEN_ACL_UNSAFE)
                        .forPath(instancePath, JSON.toJSONString(serviceInstance).getBytes(StandardCharsets.UTF_8));
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void deregister(ServiceDefinition serviceDefinition, ServiceInstance serviceInstance) {
        try {
            // 构建服务定义路径
            String servicePath = DEVPATH + PATH_SEPARATOR + serviceDefinition.getUniqueId();

            Stat stat = client.checkExists().forPath(servicePath);

            if (stat != null) {
                // 构建服务实例路径
                String instancePath = servicePath + PATH_SEPARATOR + serviceInstance.getServiceInstanceId();
                stat = client.checkExists().forPath(instancePath);

                if (stat != null) {
                    client.delete().deletingChildrenIfNeeded().forPath(instancePath);
                }

                // 检查是否还有其他实例，如果没有，则删除服务定义路径
                if (client.getChildren().forPath(servicePath).isEmpty()) {
                    client.delete().deletingChildrenIfNeeded().forPath(servicePath);
                }
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void subscribeAllServices(RegisterCenterListener registerCenterListener) {
        doSubscribeAllServices(registerCenterListener);
        // 创建服务定义路径的监听器
        PathChildrenCache cache = new PathChildrenCache(client, DEVPATH, true);
        
        cache.getListenable().addListener(new PathChildrenCacheListener() {
            @Override
            public void childEvent(CuratorFramework client, PathChildrenCacheEvent event) {
                switch (event.getType()) {
                    case CHILD_ADDED:
                        // 处理服务定义节点新增事件
                        // event.getData() 包含新增的节点信息
                        Set<ServiceInstance> set = new HashSet<>();
                        byte[] data = event.getData().getData(); // 获取节点数据
                        String definitionString = new String(data, StandardCharsets.UTF_8);
                        ServiceDefinition serviceDefinition = JSON.parseObject(definitionString).toJavaObject(ServiceDefinition.class);
                        String path = event.getData().getPath();
                        try {
                            List<String> childrenInstance = client.getChildren().forPath(path);
                            for(String instance : childrenInstance){
                                byte[] dataInstance = client.getData().forPath(path +  PATH_SEPARATOR + instance);
                                if(dataInstance == null){
                                    continue;
                                }
                                String instanceString = new String(dataInstance,StandardCharsets.UTF_8);
                                ServiceInstance serviceInstance = JSON.parseObject(instanceString).toJavaObject(ServiceInstance.class);
                                set.add(serviceInstance);
                            }
                            registerCenterListener.onChange(serviceDefinition,set);
                        } catch (Exception e) {
                            throw new RuntimeException(e);
                        }

                        break;
                    case CHILD_REMOVED:
                        path = event.getData().getPath(); // 获取节点路径
                        String uniqueId = path.substring((DEVPATH+"/").length());
                        DynamicConfigManager manager = DynamicConfigManager.getInstance();
                        manager.removeServiceInstancesByUniqueId(uniqueId);
                        manager.removeServiceDefinition(uniqueId);
                        break;
                    default:
                        // 其他事件类型
                        break;
                }

                // 递归设置监听器，以处理服务实例节点
                if (event.getType() == PathChildrenCacheEvent.Type.CHILD_ADDED) {
                    String serviceDefinitionPath = event.getData().getPath();
                    listenForServiceInstanceChanges(serviceDefinitionPath);
                }
            }
        });

        try {
            cache.start(PathChildrenCache.StartMode.BUILD_INITIAL_CACHE);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }



    }

    public void listenForServiceInstanceChanges(String path) {
        PathChildrenCache cache = new PathChildrenCache(client, path, true);
        try {
            byte[] data = client.getData().forPath(path);
            String definitionString = new String(data, StandardCharsets.UTF_8);
            String uniqueId =
                    JSON.parseObject(definitionString).toJavaObject(ServiceDefinition.class).getUniqueId();
            cache.getListenable().addListener(new PathChildrenCacheListener() {
                @Override
                public void childEvent(CuratorFramework client, PathChildrenCacheEvent event) {
                    switch (event.getType()) {
                        case CHILD_ADDED:
                            byte[] data1 = event.getData().getData();
                            String instanceString = new String(data1,StandardCharsets.UTF_8);
                            ServiceInstance serviceInstance = JSON.parseObject(instanceString).toJavaObject(ServiceInstance.class);
                            DynamicConfigManager.getInstance().addServiceInstance(uniqueId,serviceInstance);
                            break;
                        case CHILD_REMOVED:
                            String deletePath = event.getData().getPath(); // 获取节点路径
                            String serviceInstanceId = deletePath.substring((path+"/").length());
                            DynamicConfigManager manager = DynamicConfigManager.getInstance();
                            manager.removeServiceInstance(uniqueId,serviceInstanceId);
                            break;
                        default:
                            // 其他事件类型
                            break;
                    }
                }
            });
            cache.start(PathChildrenCache.StartMode.BUILD_INITIAL_CACHE);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

    }

    private void doSubscribeAllServices(RegisterCenterListener registerCenterListener) {
        List<String> children = null;
        try {
            children = client.getChildren().forPath(DEVPATH);
            for (String definition : children) {
                byte[] data = client.getData().forPath(DEVPATH + "/" + definition);
                // 处理每个现有节点的数据，将其视为 CHILD_ADDED
                if(data == null){
                    continue;
                }
                String definitionString = new String(data, StandardCharsets.UTF_8);
//                System.out.println(definitionString);
                // 循环遍历服务node 查看下面的node 每个是一个实例
                ServiceDefinition serviceDefinition = JSON.parseObject(definitionString).toJavaObject(ServiceDefinition.class);
//                log.info("读取到的serviceDefinition : {}",serviceDefinition);
                Set<ServiceInstance> set = new HashSet<>();
                List<String> childrenInstance = client.getChildren().forPath(DEVPATH + PATH_SEPARATOR + definition);
                if(childrenInstance.isEmpty()) {
                    client.delete().forPath(DEVPATH + PATH_SEPARATOR + definition);
                    registerCenterListener.onChange(serviceDefinition,null);
                    continue;
                }
                for(String instance : childrenInstance){
                    byte[] dataInstance = client.getData().forPath(DEVPATH + PATH_SEPARATOR + definition + PATH_SEPARATOR + instance);
                    if(dataInstance == null){
                        continue;
                    }
                    String instanceString = new String(dataInstance,StandardCharsets.UTF_8);
//                    System.out.println(instanceString);
                    ServiceInstance serviceInstance = JSON.parseObject(instanceString).toJavaObject(ServiceInstance.class);
//                    log.info("读取到的serviceInstance : {}",serviceInstance);
                    set.add(serviceInstance);
                }
                registerCenterListener.onChange(serviceDefinition,set);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
