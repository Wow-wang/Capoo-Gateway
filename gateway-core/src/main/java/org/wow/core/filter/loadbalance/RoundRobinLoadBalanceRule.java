package org.wow.core.filter.loadbalance;

import lombok.extern.slf4j.Slf4j;
import org.wow.common.config.DynamicConfigManager;
import org.wow.common.config.ServiceInstance;
import org.wow.common.enums.ResponseCode;
import org.wow.common.exception.NotFoundException;
import org.wow.core.context.GatewayContext;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @program: api-gateway
 * @description: 负载均衡 - 轮询
 * @author: wow
 * @create: 2023-10-02 11:14
 **/

@Slf4j
public class RoundRobinLoadBalanceRule implements IGatewayLoadBalanceRule{

    private AtomicInteger position = new AtomicInteger(1);

    private final String serviceId;

    private Set<ServiceInstance> serviceInstanceSet;

    public RoundRobinLoadBalanceRule( String serviceId) {
        this.serviceId = serviceId;
    }

    private static ConcurrentHashMap<String,RoundRobinLoadBalanceRule> serviceMap = new ConcurrentHashMap<>();

    public static RoundRobinLoadBalanceRule getInstance(String serviceId){
        RoundRobinLoadBalanceRule loadBalanceRule = serviceMap.get(serviceId);
        if(loadBalanceRule == null){
            loadBalanceRule = new RoundRobinLoadBalanceRule(serviceId);
            serviceMap.put(serviceId,loadBalanceRule);
        }
        return loadBalanceRule;
    }

    @Override
    public ServiceInstance choose(GatewayContext ctx) {
        return choose(ctx.getUniqueId(), ctx.isGray());
    }

    @Override
    public ServiceInstance choose(String serviceId, boolean gray) {
        serviceInstanceSet = DynamicConfigManager.getInstance().getServiceInstanceByUniqueId(serviceId,gray);
        if(serviceInstanceSet.isEmpty()){
            log.warn("No instance available for : {}", serviceId);
            throw new NotFoundException(ResponseCode.SERVICE_INSTANCE_NOT_FOUND);
        }
        List<ServiceInstance> instances = new ArrayList<ServiceInstance>(serviceInstanceSet);
        if(instances.isEmpty()){
            log.warn("No instance available for service: {}", serviceId);
            return null;
        }else{
            int pos = Math.abs(this.position.incrementAndGet());
            return instances.get(pos % instances.size());
        }
    }
}
