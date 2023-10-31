package org.producer.impl;


import com.alibaba.dubbo.config.annotation.Service;
import org.apache.dubbo.rpc.RpcContext;
import org.producer.service.HelloServiceAPI;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
//import org.wow.gateway.client.core.ApiProtocol;
//import org.wow.gateway.client.core.ApiService;



//@Service(interfaceClass = HelloServiceAPI.class, loadbalance = "roundrobin", weight = 60,
//        cluster = "failover", retries = 3, version = "1.0")
//@Component
//@ApiService(serviceId = "dubbo-test", protocol = ApiProtocol.DUBBO, patternPath = "/")
public class HelloServiceImpl implements HelloServiceAPI {
    @Value("${server.port}")
    private int serverPort;

    @Override
    public String sayHello(String message) {
        System.out.println("word : "+ RpcContext.getContext().getAttachment("word"));

        return "Producer response 2 : Hello " +message + " , port : "+ serverPort;
    }
}
