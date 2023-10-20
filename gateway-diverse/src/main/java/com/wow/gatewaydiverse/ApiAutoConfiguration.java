package com.wow.gatewaydiverse;


import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.web.client.RestTemplate;

/**
 * @program: gateway-diverse
 * @description:
 * @author: wow
 * @create: 2023-10-20 10:13
 **/
@Configuration
@EnableConfigurationProperties(ApiProperties.class)
public class ApiAutoConfiguration {
    @Bean
    @ConditionalOnMissingBean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }

    @Bean
    public ApiHealthIndicator apiHealthIndicator(ApiProperties apiProperties, RestTemplate restTemplate) {
        return new ApiHealthIndicator(apiProperties, restTemplate);
    }

    @Bean
    public ApiChecker apiCleaner(ApiDiscoveryClient apiDiscoveryClient, ApiHealthIndicator apiHealthIndicator, ApiProperties apiProperties) {
        ApiChecker cleaner = new ApiChecker(apiDiscoveryClient, apiHealthIndicator, apiProperties);
        cleaner.check();
        return cleaner;
    }
}
