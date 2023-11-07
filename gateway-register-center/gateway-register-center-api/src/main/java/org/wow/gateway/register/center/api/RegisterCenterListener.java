package org.wow.gateway.register.center.api;

import org.wow.common.config.ServiceDefinition;
import org.wow.common.config.ServiceInstance;

import java.util.Set;

/**
 * @program: api-gateway
 * @description:
 * @author: wow
 * @create: 2023-09-27 21:26
 **/

public interface RegisterCenterListener {

    void onChange(ServiceDefinition serviceDefinition, Set<ServiceInstance> serviceInstanceSet);


}
