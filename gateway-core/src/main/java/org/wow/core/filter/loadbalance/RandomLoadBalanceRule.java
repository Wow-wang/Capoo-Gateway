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
import java.util.concurrent.ThreadLocalRandom;

/**
 * @program: api-gateway
 * @description: 负载均衡-权重随机
 * @author: wow
 * @create: 2023-10-02 10:59
 **/

@Slf4j
public class RandomLoadBalanceRule implements IGatewayLoadBalanceRule{
    private final String serviceId;

    private Set<ServiceInstance> serviceInstanceSet;

    public RandomLoadBalanceRule(String serviceId) {
        this.serviceId = serviceId;
    }

    private static ConcurrentHashMap<String,RandomLoadBalanceRule> serviceMap = new ConcurrentHashMap<>();

    public static RandomLoadBalanceRule getInstance(String serviceId){
        RandomLoadBalanceRule loadBalanceRule = serviceMap.get(serviceId);
        if(loadBalanceRule == null){
            loadBalanceRule = new RandomLoadBalanceRule(serviceId);
            serviceMap.put(serviceId,loadBalanceRule);
        }
        return loadBalanceRule;
    }
    @Override
    public ServiceInstance choose(GatewayContext ctx) {
        String serviceId = ctx.getUniqueId();
        return choose(serviceId, ctx.isGray());
    }

    @Override
    public ServiceInstance choose(String serviceId, boolean gray) {
        serviceInstanceSet = DynamicConfigManager.getInstance().getServiceInstanceByUniqueId(serviceId,gray);
        if(serviceInstanceSet.isEmpty()){
            serviceInstanceSet = DynamicConfigManager.getInstance().getServiceInstanceByUniqueId(serviceId,gray);
        }
        if(serviceInstanceSet.isEmpty()){
            log.warn("No instance available for : {}", serviceId);
            throw new NotFoundException(ResponseCode.SERVICE_INSTANCE_NOT_FOUND);
        }
        List<ServiceInstance> instances = new ArrayList<ServiceInstance>(serviceInstanceSet);

        // ThreadLocalRandom 类来生成一个随机整数，用于选择一个列表（instances）中的元素索引
        int index = indexRandomChooseByWeight(instances);
        ServiceInstance instance = (ServiceInstance)instances.get(index);
        return instance;
    }

    public Integer indexRandomChooseByWeight(List<ServiceInstance> instances){
        List<Integer> indexes = new ArrayList<>();
        Integer temp = 0;
        for(int i = 0; i < instances.size(); i++){
            temp += instances.get(0).getWeight();
            indexes.add(temp);
        }
        int index = ThreadLocalRandom.current().nextInt(temp);

        int left = 0, right = instances.size() - 1;
        while(left < right){
            int mid = (left + right) >> 1;
            if(indexes.get(mid) >= index){
                right = mid;
            }else{
                left = mid +1;
            }
        }
        return left;
    }



}
