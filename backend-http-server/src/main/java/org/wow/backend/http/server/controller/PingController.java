package org.wow.backend.http.server.controller;

import lombok.extern.slf4j.Slf4j;
import org.wow.gateway.client.core.ApiInvoker;
import org.wow.gateway.client.core.ApiProperties;
import org.wow.gateway.client.core.ApiProtocol;
import org.wow.gateway.client.core.ApiService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.PostConstruct;


@Slf4j
@RestController
@ApiService(serviceId = "backend-http-server", protocol = ApiProtocol.HTTP, patternPath = "/http-server/**")
public class PingController {


    @Autowired
    private ApiProperties apiProperties;

    @ApiInvoker(path = "/http-server/ping")
    @GetMapping("/http-server/ping")
    public String ping() throws InterruptedException {
        log.info("ping pang");
        Thread.sleep(80);
        String test = "pong";
        return test.repeat(470);
    }

    @ApiInvoker(path = "/http-server111/ping")
    @GetMapping("/http-server111/ping")
    public String ping111() {
        return "pong111";
    }

}
