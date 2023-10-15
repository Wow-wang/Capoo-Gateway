package org.wow.gateway.register.center.nacos;

import com.alibaba.fastjson.JSON;
import com.alibaba.nacos.api.exception.NacosException;
import com.alibaba.nacos.api.naming.NamingFactory;
import com.alibaba.nacos.api.naming.NamingMaintainFactory;
import com.alibaba.nacos.api.naming.NamingMaintainService;
import com.alibaba.nacos.api.naming.NamingService;
import com.alibaba.nacos.api.naming.listener.Event;
import com.alibaba.nacos.api.naming.listener.EventListener;
import com.alibaba.nacos.api.naming.listener.NamingEvent;
import com.alibaba.nacos.api.naming.pojo.Instance;
import com.alibaba.nacos.api.naming.pojo.Service;
import com.alibaba.nacos.api.naming.pojo.ServiceInfo;
import com.alibaba.nacos.common.executor.NameThreadFactory;
import com.alibaba.nacos.common.utils.CollectionUtils;
import lombok.extern.slf4j.Slf4j;
import org.wow.common.config.ServiceDefinition;
import org.wow.common.config.ServiceInstance;
import org.wow.common.constants.GatewayConst;
import org.wow.gateway.register.center.api.RegisterCenter;
import org.wow.gateway.register.center.api.RegisterCenterListener;

import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * @program: api-gateway
 * @description:
 * @author: wow
 * @create: 2023-09-27 21:59
 **/

@Slf4j
public class NacosRegisterCenter implements RegisterCenter {
    // 每个注册中心 registerAddress不一样
    private String registerAddress;
    private String env;

    // 主要用于维护服务实例信息
    private NamingService namingService;

    // 主要维护服务定义信息
    private NamingMaintainService namingMaintainService;

    //监听器列表
    /**
     * CopyOnWriteArrayList 是线程安全的，可以在多线程环境下安全地进行读取操作，而不需要额外的同步措施。这使得它适用于读多写少的场景
     */
    private List<RegisterCenterListener> registerCenterListenerList  = new CopyOnWriteArrayList<>();


    // 每个registerCenter对应了唯一Address 和 env
    @Override
    public void init(String registerAddress, String env) {
        this.registerAddress = registerAddress;
        this.env = env;

        try {
            this.namingMaintainService = NamingMaintainFactory.createMaintainService(registerAddress);
            this.namingService = NamingFactory.createNamingService(registerAddress);
            System.out.println(namingService == null);
        } catch (NacosException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void register(ServiceDefinition serviceDefinition, ServiceInstance serviceInstance) {
        try{
            // 构造nacos实例信息
            Instance nacosInstance = new Instance();
            nacosInstance.setInstanceId(serviceInstance.getServiceInstanceId());
            nacosInstance.setPort(serviceInstance.getPort());
            nacosInstance.setIp(serviceInstance.getIp());
            nacosInstance.setMetadata(Map.of(GatewayConst.META_DATA_KEY,
                    JSON.toJSONString(serviceInstance)));

            // 注册
            // System.out.println(namingService == null);
            namingService.registerInstance(serviceDefinition.getServiceId(),env,nacosInstance);

            // 更新服务定义
            namingMaintainService.updateService(serviceDefinition.getServiceId(),env,0,
                    Map.of(GatewayConst.META_DATA_KEY,JSON.toJSONString(serviceDefinition)));

            log.info("register {} {}",serviceDefinition,serviceInstance);
        } catch (NacosException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void deregister(ServiceDefinition serviceDefinition, ServiceInstance serviceInstance) {
        try{
            namingService.registerInstance(serviceDefinition.getServiceId(),
                    env,serviceInstance.getIp(),serviceInstance.getPort());
        }catch (NacosException e){
            throw new RuntimeException(e);
        }
    }

    @Override
    public void subscribeAllServices(RegisterCenterListener registerCenterListener) {
        registerCenterListenerList.add(registerCenterListener);
        doSubscribeAllServices();

        // 可能有新服务加入 所以需要一个定时任务来检查
        ScheduledExecutorService scheduledThreadPool = Executors.newScheduledThreadPool(1,
                new NameThreadFactory("doSubscribeAllServices"));
        // 每次任务开始执行时 安排一个任务按照固定的时间间隔执行
        scheduledThreadPool.scheduleWithFixedDelay(()->doSubscribeAllServices(),
                10,10, TimeUnit.SECONDS);
    }

    private void doSubscribeAllServices() {
        try {
            // 已经订阅的环境
            Set<String> subscribeService = namingService.getSubscribeServices().stream()
                    .map(ServiceInfo::getName).collect(Collectors.toSet());

            int pageNo = 1;
            int pageSize = 100;


            // 分页从nacos拿到服务列表
            List<String> serviceList = namingService
                    .getServicesOfServer(pageNo, pageSize, env).getData();

            while(CollectionUtils.isNotEmpty(serviceList)){
                log.info("service list size {}",serviceList.size());

                for(String service : serviceList){
                    if(subscribeService.contains(service)){
                        // 如果原本就注册过监听器 就不需要监听了
                        continue;
                    }

                    /**
                     * nacos事件监听器
                     * 如果nacos事件有改变就会触发onEvent
                     */
                    EventListener eventListener = new NacosRegisterListener();
                    eventListener.onEvent(new NamingEvent(service, null));

                    /**
                     * 当服务的状态发生变化时，例如新的实例注册或注销，
                     * Nacos会触发事件，然后您可以通过事件监听器 (eventListener) 来处理这些事件。
                     */
                    namingService.subscribe(service,env,eventListener);
                    log.info("subscribe {} {}",service,env);
                }



                serviceList = namingService
                        .getServicesOfServer(++pageNo,pageSize,env).getData();
            }

        }catch (NacosException e){
            throw new RuntimeException(e);
        }
    }

    // 监听更新服务
    public class NacosRegisterListener implements EventListener{

        @Override
        public void onEvent(Event event) {
            log.info("onEvent监听到注册的服务发生变化");
            if(event instanceof NamingEvent){
                NamingEvent namingEvent = (NamingEvent) event;
                String serviceName = namingEvent.getServiceName();
                try {
                    // 获取服务定义信息
                    Service service = namingMaintainService.queryService(serviceName, env);
                    log.info("注册服务发生变化获取到的 service ： {}",service);

                    // 从一个包含JSON格式数据的字符串中解析出一个Java对象
                    ServiceDefinition serviceDefinition = JSON.parseObject(service.getMetadata()
                            .get(GatewayConst.META_DATA_KEY), ServiceDefinition.class);
                    log.info("注册服务发生变化获取到的 serviceDefinition ： {}",serviceDefinition);

                     // 获取服务实例信息
//                    List<Instance> allInstances = namingService.getAllInstances(serviceName, env);
//                    System.out.println(serviceName+":"+service.getName());
                    List<Instance> allInstances = namingService.getAllInstances(service.getName(), env);
                    Set<ServiceInstance> set = new HashSet<>();

                    for (Instance instance : allInstances) {
                        ServiceInstance serviceInstance = JSON.parseObject(instance.getMetadata()
                                .get(GatewayConst.META_DATA_KEY), ServiceInstance.class);
                        set.add(serviceInstance);
                    }
                    /**
                     * 调用存放在NacosRegisterCenter的list
                     */
                    registerCenterListenerList.stream()
                            .forEach(l->l.onChange(serviceDefinition,set));

                } catch (NacosException e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }
}
