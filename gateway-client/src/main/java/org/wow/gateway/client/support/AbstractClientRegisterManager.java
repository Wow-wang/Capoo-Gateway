package org.wow.gateway.client.support;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.wow.common.config.ServiceDefinition;
import org.wow.common.config.ServiceInstance;
import org.wow.gateway.client.core.ApiProperties;
import org.wow.gateway.register.center.api.RegisterCenter;

import java.util.ServiceLoader;

/**
 * @program: api-gateway
 * @description: 连接初始化注册中心（nacos） 和 下游MVC服务或者下游Dubbo服务
 * @author: wow
 * @create: 2023-09-29 22:18
 **/

@Slf4j
public abstract class AbstractClientRegisterManager {

    @Getter
    private ApiProperties apiProperties;

    private RegisterCenter registerCenter;

    protected AbstractClientRegisterManager(ApiProperties apiProperties){
        this.apiProperties = apiProperties;

        // 初始化注册中心对象
        /**
         * 使用 Java 的 ServiceLoader 机制来初始化注册中心对象。
         * ServiceLoader 是 Java 的一个标准库，用于加载和管理服务提供者的实现。
         * 在这里，它被用来加载实现了 RegisterCenter 接口的类
         *
         * SPI 是一种 Java 设计模式，允许开发者定义接口，并通过配置文件来指定接口的具体实现类 RegisterCenter.class
         * 这些配置文件通常存放在 JAR 文件或类路径中的 META-INF/services 目录下 org.wow.gateway.register.center.api.RegisterCenter
         * 当需要获取某个接口的实现时，可以使用 ServiceLoader 类来加载并获取这个接口的所有实现
         */
        ServiceLoader<RegisterCenter> serviceLoader = ServiceLoader.load(RegisterCenter.class);
        registerCenter = serviceLoader.findFirst().orElseThrow(() -> {
            log.error("not found RegisterCenter impl");
            return new RuntimeException("not found RegisterCenter impl");
        });
        // 注册中心需要初始化
        registerCenter.init(apiProperties.getRegisterAddress(), apiProperties.getEnv());

    }

    protected void register(ServiceDefinition serviceDefinition, ServiceInstance serviceInstance){
        registerCenter.register(serviceDefinition,serviceInstance);
    }


}
