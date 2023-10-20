package com.wow.gatewaydiverse.nacos;

import com.alibaba.cloud.nacos.NacosDiscoveryProperties;
import com.alibaba.fastjson.JSON;
import com.alibaba.nacos.api.exception.NacosException;
import com.alibaba.nacos.api.naming.pojo.Instance;
import com.wow.gatewaydiverse.ApiDiscoveryClient;
import com.wow.gatewaydiverse.gatewayConst.GatewayConst;
import com.wow.gatewaydiverse.service.ServiceDefinition;
import com.wow.gatewaydiverse.service.ServiceInstance;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.client.discovery.DiscoveryClient;

import java.util.Map;


/**
 * @author wow
 */
@Slf4j
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class ApiNacosDiscoveryClient implements ApiDiscoveryClient {
    // 或者使用NamingMaintainFactory
    private final ApiNacosDiscoveryProperties apiNacosDiscoveryProperties;


    @Override
    public void registerInstance( ServiceDefinition serviceDefinition, ServiceInstance serviceInstance) {
        // 构造nacos实例信息
        try {
            Instance nacosInstance = new Instance();
            nacosInstance.setInstanceId(serviceInstance.getServiceInstanceId());
            nacosInstance.setPort(serviceInstance.getPort());
            nacosInstance.setIp(serviceInstance.getIp());
            nacosInstance.setMetadata(Map.of(GatewayConst.META_DATA_KEY,
                    JSON.toJSONString(serviceInstance)));
            System.out.println(serviceDefinition.getEnvType());

            this.apiNacosDiscoveryProperties.namingServiceInstance().registerInstance(serviceDefinition.getServiceId(),serviceDefinition.getEnvType(),nacosInstance);
            this.apiNacosDiscoveryProperties.namingMaintainServiceInstance().updateService(serviceDefinition.getServiceId(),serviceDefinition.getEnvType(),0,
                    Map.of(GatewayConst.META_DATA_KEY,JSON.toJSONString(serviceDefinition)));


        } catch (Exception e) {
            throw new RuntimeException(e);
        }


    }

    @Override
    public void deregisterInstance(ServiceDefinition serviceDefinition, ServiceInstance serviceInstance) {
        try{
            this.apiNacosDiscoveryProperties.namingServiceInstance().deregisterInstance(serviceDefinition.getServiceId(),
                    serviceDefinition.getEnvType(),serviceInstance.getIp(),serviceInstance.getPort());
        }catch (NacosException e){
            throw new RuntimeException(e);
        }
    }
}
