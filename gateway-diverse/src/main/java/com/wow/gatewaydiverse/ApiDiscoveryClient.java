package com.wow.gatewaydiverse;


import com.wow.gatewaydiverse.service.ServiceDefinition;
import com.wow.gatewaydiverse.service.ServiceInstance;

/**
 * @author wow
 */
public interface ApiDiscoveryClient {
    /**
     * register instance
     *

     */
    void registerInstance(ServiceDefinition serviceDefinition, ServiceInstance serviceInstance);

    /**
     * deregister instance
     *

     */
    void deregisterInstance(ServiceDefinition serviceDefinition, ServiceInstance serviceInstance);
}
