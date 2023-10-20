package com.wow.gatewaydiverse;

import lombok.Data;
import org.jetbrains.annotations.NotNull;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

import java.net.URI;

/**
 * @program: gateway-diverse
 * @description:
 * @author: wow
 * @create: 2023-10-20 10:14
 **/

@Component
@ConfigurationProperties("api")
@Data
@Validated
public class ApiProperties {
    /**
     * polyglot service's ip
     */
    private String ip;

    /**
     * polyglot service's port
     */

    private Integer port;

    /**
     * polyglot service's health check url.
     * this endpoint must return json and the format must follow spring boot actuator's health endpoint.
     * eg. {"status": "UP"}
     */
    private URI healthCheckUrl;

    private String ServiceId;

    private String version;

    private String env;

    private String patternPath;

    private Integer weight = 100;

    private Boolean gray = false;
}
