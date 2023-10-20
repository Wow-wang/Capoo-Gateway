package com.wow.gatewaydiverse.nacos;

import com.alibaba.cloud.nacos.discovery.NacosDiscoveryClientAutoConfiguration;
import com.wow.gatewaydiverse.ApiAutoConfiguration;
import com.wow.gatewaydiverse.ApiDiscoveryClient;
import com.wow.gatewaydiverse.ApiProperties;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;



@Configuration
@AutoConfigureBefore({NacosDiscoveryClientAutoConfiguration.class, ApiAutoConfiguration.class})
@ConditionalOnClass(com.alibaba.cloud.nacos.NacosDiscoveryProperties.class)
public class ApiNacosAutoConfiguration {
    @Bean
    @ConditionalOnMissingBean
    public ApiNacosDiscoveryProperties apiDiscoveryProperties(ApiProperties apiProperties) {
        return new ApiNacosDiscoveryProperties(apiProperties);
    }

    @Bean
    @ConditionalOnMissingBean
    public ApiNacosDiscoveryClient apiDiscoveryClient(ApiNacosDiscoveryProperties apiNacosDiscoveryProperties) {
        return new ApiNacosDiscoveryClient(apiNacosDiscoveryProperties);
    }
}
