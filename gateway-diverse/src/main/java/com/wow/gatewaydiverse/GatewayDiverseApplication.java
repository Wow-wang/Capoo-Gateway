package com.wow.gatewaydiverse;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;


@SpringBootApplication(scanBasePackages = "com.wow")
public class GatewayDiverseApplication {

    public static void main(String[] args) {
        SpringApplication.run(GatewayDiverseApplication.class, args);
    }

}
