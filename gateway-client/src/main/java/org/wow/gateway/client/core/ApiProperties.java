package org.wow.gateway.client.core;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @program: api-gateway
 * @description:
 * @author: wow
 * @create: 2023-09-29 22:20
 **/

@Data
@ConfigurationProperties(prefix = "api")
/**
 * @ConfigurationProperties
 * 这个注解用于告诉 Spring Boot 从配置文件中读取以 "api" 为前缀的属性，并将它们映射到 ApiProperties 类的属性
 */

 public class ApiProperties {
    private String registerAddress;

    private String env = "dev";

    private boolean gray;
}
