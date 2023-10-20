package com.wow.gatewaydiverse;

import com.wow.gatewaydiverse.gatewayConst.TimeUtil;
import com.wow.gatewaydiverse.service.ServiceDefinition;
import com.wow.gatewaydiverse.service.ServiceInstance;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.health.Status;


import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static com.wow.gatewaydiverse.gatewayConst.GatewayConst.DEFAULT_WEIGHT;
import static com.wow.gatewaydiverse.gatewayConst.GatewayConst.PROTOCOL_VALUE;

/**
 * @program: gateway-diverse
 * @description:
 * @author: wow
 * @create: 2023-10-20 10:45
 **/

@Slf4j
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class ApiChecker {
    private final ApiDiscoveryClient apiDiscoveryClient;
    // 用于访问ApiHealthIndicator的结果
    private final ApiHealthIndicator apihealthIndicator;
    private final ApiProperties apiProperties;
    private ServiceDefinition serviceDefinition = new ServiceDefinition();
    private ServiceInstance serviceInstance = new ServiceInstance();


    public void init(){
        //服务实例
        String ip = apiProperties.getIp();
        Integer port = apiProperties.getPort();
        String serviceInstanceId = ip + ":" + port;
        String serviceId = apiProperties.getServiceId();
        String version = apiProperties.getVersion();
        String patternPath = apiProperties.getPatternPath();
        String protocol = PROTOCOL_VALUE;
        Integer weight = apiProperties.getWeight();
        String env = apiProperties.getEnv();

        serviceDefinition.setUniqueId(serviceId + ":" + version);
        serviceDefinition.setServiceId(serviceId);
        serviceDefinition.setVersion(version);
        serviceDefinition.setEnvType(env);
        serviceDefinition.setProtocol(protocol);
        serviceDefinition.setPatternPath(patternPath);
        serviceDefinition.setEnable(true);
        serviceDefinition.setInvokerMap(null);

        serviceInstance.setServiceInstanceId(serviceInstanceId);
        serviceInstance.setUniqueId(serviceDefinition.getUniqueId());
        serviceInstance.setIp(ip);
        serviceInstance.setPort(port);
        serviceInstance.setRegisterTime(TimeUtil.currentTimeMillis());
        serviceInstance.setVersion(version);
        serviceInstance.setWeight(weight);
        serviceInstance.setEnv(serviceDefinition.getEnvType());
        serviceInstance.setGray(apiProperties.getGray());

    }
    public void check() {
        init();
        ScheduledExecutorService scheduledThreadPool = Executors.newScheduledThreadPool(1);
        scheduledThreadPool.scheduleWithFixedDelay(
                        () -> {
                            Status status = apihealthIndicator.health().getStatus();

                            if (status.equals(Status.UP)) {
                                this.apiDiscoveryClient.registerInstance(serviceDefinition,serviceInstance);
                                log.debug("Health check success. register this instance. applicationName = {}, ip:port = {}, status = {}",
                                        serviceDefinition.getServiceId(), serviceInstance.getServiceInstanceId(), status
                                );
                            } else {
//                                System.out.println("服务下线");
                                log.warn("Health check failed. unregister this instance. applicationName = {}, ip:port = {}, status = {}",
                                        serviceDefinition.getServiceId(), serviceInstance.getServiceInstanceId(), status
                                );
                                this.apiDiscoveryClient.deregisterInstance(serviceDefinition,serviceInstance);
                            }

                        },
                        0,
                        30,
                        TimeUnit.SECONDS
                );
    }
}
