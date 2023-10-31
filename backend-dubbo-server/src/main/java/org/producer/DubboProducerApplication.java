package org.producer;

//import org.apache.dubbo.config.spring.context.annotation.DubboComponentScan;
//import org.apache.dubbo.config.spring.context.annotation.DubboComponentScan;
//import org.apache.dubbo.config.spring.context.annotation.EnableDubbo;
//import org.apache.dubbo.config.spring.context.annotation.EnableDubboConfig;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.ImportResource;

/**
 * @program: Capoo-Api-gateway
 * @description:
 * @author: wow
 * @create: 2023-10-31 09:45
 **/
@SpringBootApplication
@ImportResource(value = { "applicationContext-dubbo.xml" })
public class DubboProducerApplication {
    public static void main(String[] args) {
        SpringApplication.run(DubboProducerApplication.class, args);
    }
}
