package org.wow.backend.http.server.controller;

import lombok.extern.slf4j.Slf4j;
import org.wow.gateway.client.core.ApiInvoker;
import org.wow.gateway.client.core.ApiProperties;
import org.wow.gateway.client.core.ApiProtocol;
import org.wow.gateway.client.core.ApiService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.apache.tomcat.jni.Time.sleep;

@Slf4j
@RestController
@ApiService(serviceId = "backend-http-server", protocol = ApiProtocol.HTTP, patternPath = "/http-server/**")
public class PingController {

    @Autowired
    private ApiProperties apiProperties;

    @ApiInvoker(path = "/http-server/ping")
    @GetMapping("/http-server/ping")
    public String ping() throws InterruptedException {
        log.info("{}", apiProperties);
//        Thread.sleep(200000);
        return "pong";
    }

    @ApiInvoker(path = "/http-server111/ping")
    @GetMapping("/http-server111/ping")
    public String ping111() {
        log.info("{}", apiProperties);
        return "pong111";
    }

}
