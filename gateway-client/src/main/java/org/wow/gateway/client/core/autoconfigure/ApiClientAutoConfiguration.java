package org.wow.gateway.client.core.autoconfigure;

import org.apache.dubbo.config.spring.ServiceBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.DispatcherServlet;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.wow.gateway.client.core.ApiProperties;
import org.wow.gateway.client.support.dubbo.Dubbo27ClientRegisterManager;
import org.wow.gateway.client.support.springmvc.SpringMVCClientRegisterManager;

import javax.servlet.Servlet;

/**
 * @program: api-gateway
 * @description: 客户端基于Springboot实现自动装配
 * @author: wow
 * @create: 2023-09-30 10:28
 **/

@Configuration
@EnableConfigurationProperties(ApiProperties.class)
@ConditionalOnProperty(prefix = "api",name = {"registerAddress"})
/**
 * @ConditionalOnProperty
 * 表示配置的条件，只有在满足指定条件的情况下才会应用这个配置
 */
public class ApiClientAutoConfiguration {
    @Autowired
    private ApiProperties apiProperties;


    /**
     * 1. 类路径中必须存在 Servlet、DispatcherServlet 和 WebMvcConfigurer 这些类
     * 2. 只有在 Spring 容器中不存在 SpringMVCClientRegisterManager 类型的 Bean 时才会创建这个 Bean
     */
    @Bean
    @ConditionalOnClass({Servlet.class, DispatcherServlet.class, WebMvcConfigurer.class})
    @ConditionalOnMissingBean(SpringMVCClientRegisterManager.class)
    public SpringMVCClientRegisterManager springMVCClientRegisterManager(){
        return new SpringMVCClientRegisterManager(apiProperties);
    }

    @Bean
    @ConditionalOnClass({ServiceBean.class})
    @ConditionalOnMissingBean(Dubbo27ClientRegisterManager.class)
    public Dubbo27ClientRegisterManager dubbo27ClientRegisterManager(){
        return new Dubbo27ClientRegisterManager(apiProperties);
    }
}
