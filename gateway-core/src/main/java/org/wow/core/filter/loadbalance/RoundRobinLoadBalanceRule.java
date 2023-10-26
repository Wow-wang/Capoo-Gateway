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
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * @program: api-gateway
 * @description: 负载均衡 - 轮询 参考Dubbo
 * @author: wow
 * @create: 2023-10-02 11:14
 **/

@Slf4j
public class RoundRobinLoadBalanceRule implements IGatewayLoadBalanceRule{

    private static final int RECYCLE_PERIOD = 60000;

    protected static class WeightedRoundRobin {
        private int weight;// 权重值
        private AtomicLong current = new AtomicLong(0);
        private long lastUpdate;// 最后修改时间
        public int getWeight() {
            return weight;
        }
        public void setWeight(int weight) {
            this.weight = weight;
            current.set(0);// 设置成0
        }

        // 将权重值设置到atomiclong中
        public long increaseCurrent() {
            return current.addAndGet(weight);
        }
        public void sel(int total) {
            current.addAndGet(-1 * total);
        }
        public long getLastUpdate() {
            return lastUpdate;
        }
        public void setLastUpdate(long lastUpdate) {
            this.lastUpdate = lastUpdate;
        }
    }
    // 缓存   key = serviceId      value = map<ServiceInstanceId，WeightedRoundRobin >
    private ConcurrentMap<String, ConcurrentMap<String, WeightedRoundRobin>> methodWeightMap = new ConcurrentHashMap<String, ConcurrentMap<String, WeightedRoundRobin>>();


//    private AtomicInteger position = new AtomicInteger(1);

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
            return doSelect(serviceId,instances);
        }
    }

    private ServiceInstance doSelect(String serviceId,List<ServiceInstance> instances) {
        ConcurrentMap<String, WeightedRoundRobin> map = methodWeightMap.computeIfAbsent(serviceId, k -> new ConcurrentHashMap<>());
        int totalWeight = 0;
        long maxCurrent = Long.MIN_VALUE;
        long now = System.currentTimeMillis();
        ServiceInstance selectedServiceInstance = null;
        WeightedRoundRobin selectedWRR = null;
        for (ServiceInstance instance : instances) {
            String serviceInstanceId = instance.getServiceInstanceId();
            int weight = instance.getWarmWeight();
            WeightedRoundRobin weightedRoundRobin = map.computeIfAbsent(serviceInstanceId, k -> {
                WeightedRoundRobin wrr = new WeightedRoundRobin();
                wrr.setWeight(weight);
                return wrr;
            });

            if (weight != weightedRoundRobin.getWeight()) {
                //weight changed
                weightedRoundRobin.setWeight(weight);
            }
            long cur = weightedRoundRobin.increaseCurrent();
            weightedRoundRobin.setLastUpdate(now);
            if (cur > maxCurrent) {
                maxCurrent = cur;
                selectedServiceInstance = instance;
                selectedWRR = weightedRoundRobin;
            }
            totalWeight += weight;
        }

        // 新添加如果之前有实例存活时间超过60s 就重置
        if (instances.size() != map.size()) {
            map.entrySet().removeIf(item -> now - item.getValue().getLastUpdate() > RECYCLE_PERIOD);
        }
        if (selectedServiceInstance != null) {
            selectedWRR.sel(totalWeight);
            return selectedServiceInstance;
        }
        // should not happen here
        return instances.get(0);
    }
}
