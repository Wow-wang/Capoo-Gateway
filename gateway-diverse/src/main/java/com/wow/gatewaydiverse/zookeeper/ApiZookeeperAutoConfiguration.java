package com.wow.gatewaydiverse.zookeeper;

import com.alibaba.cloud.nacos.discovery.NacosDiscoveryClientAutoConfiguration;
import com.wow.gatewaydiverse.ApiAutoConfiguration;
import com.wow.gatewaydiverse.ApiDiscoveryClient;
import com.wow.gatewaydiverse.ApiProperties;
import com.wow.gatewaydiverse.nacos.ApiNacosDiscoveryClient;
import com.wow.gatewaydiverse.nacos.ApiNacosDiscoveryProperties;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @program: gateway-diverse
 * @description:
 * @author: wow
 * @create: 2023-10-20 16:52
 **/
@Configuration
@AutoConfigureBefore({ApiAutoConfiguration.class})
public class ApiZookeeperAutoConfiguration {
    @Bean
    @ConditionalOnMissingBean
    public ApiZookeeperDiscoveryProperties apiDiscoveryProperties(ApiProperties apiProperties) {
        return new ApiZookeeperDiscoveryProperties(apiProperties);
    }

    @Bean
    @ConditionalOnMissingBean
    public ApiZookeeperDiscoveryClient apiDiscoveryClient(ApiZookeeperDiscoveryProperties apiZookeeperDiscoveryProperties) {
        return new ApiZookeeperDiscoveryClient(apiZookeeperDiscoveryProperties);
    }
}
