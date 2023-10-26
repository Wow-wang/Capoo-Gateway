package org.wow.core.filter.loadbalance;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.wow.common.config.DynamicConfigManager;
import org.wow.common.config.ServiceInstance;
import org.wow.common.enums.ResponseCode;
import org.wow.common.exception.NotFoundException;
import org.wow.core.context.GatewayContext;
import org.wow.core.filter.GatewayFilterChain;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.LongAdder;

/**
 * @program: api-gateway
 * @description:
 * @author: wow
 * @create: 2023-10-26 19:10
 **/

@Slf4j
public class LeastActiveLoadBalanceRule implements IGatewayLoadBalanceRule{
    private final String serviceId;

    private Set<ServiceInstance> serviceInstanceSet;

    public LeastActiveLoadBalanceRule(String serviceId) {
        this.serviceId = serviceId;
    }

    // key : serviceInstanceId  value: 并发数
    @Getter
    public Cache<String, LongAdder> activeCache = Caffeine.newBuilder().recordStats().expireAfterWrite(10, TimeUnit.SECONDS).build();

    /**
     * 缓存 负载均衡策略
     */
    private static ConcurrentHashMap<String,LeastActiveLoadBalanceRule> serviceMap = new ConcurrentHashMap<>();
    public static LeastActiveLoadBalanceRule getInstance(String serviceId){
        LeastActiveLoadBalanceRule loadBalanceRule = serviceMap.get(serviceId);
        if(loadBalanceRule == null){
            loadBalanceRule = new LeastActiveLoadBalanceRule(serviceId);
            serviceMap.put(serviceId,loadBalanceRule);
        }
        return loadBalanceRule;
    }
    @Override
    public ServiceInstance choose(GatewayContext ctx) {
        String uniqueId = ctx.getUniqueId();
        return choose(uniqueId, ctx.isGray());
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

    private ServiceInstance doSelect(String serviceId, List<ServiceInstance> instances) {
        int length = instances.size();
        // The least active value of all instances
        int leastActive = -1;
        // The number of instance having the same least active value (leastActive)
        int leastCount = 0;
        // The index of instance having the same least active value (leastActive)
        int[] leastIndexes = new int[length];
        // the weight of every instance
        int[] weights = new int[length];
        // The sum of the warmup weights of all the least active instance
        int totalWeight = 0;
        // The weight of the first least active instance
        int firstWeight = 0;
        // Every least active instance has the same weight value?
        boolean sameWeight = true;


        // Filter out all the least active instance
        for (int i = 0; i < length; i++) {
            ServiceInstance instance = instances.get(i);
            // 获取当前这个instance并发数
            int active = activeCache.get(instance.getServiceInstanceId(),k->{return new LongAdder();}).intValue();
            // 计算权重值
            int weight = instance.getWarmWeight(); // Weight

            // 第一个元素的后    或者 当前instance并发数 小于 最小并发数（初始值是-1）
            if (leastActive == -1 || active < leastActive) {
                // 记录leastActive 为当前的活跃数
                leastActive = active; // Record the current least active value
                //重置最小计数，基于当前最小计数重新计数
                leastCount = 1; // Reset leastCount, count again based on current leastCount

                //在0下标出放入这个索引
                leastIndexes[0] = i; // Reset

                // 总权重就是 当前instance的权重
                totalWeight = weight; // Reset
                //第一个权重
                firstWeight = weight; // Record the weight the first instance

                sameWeight = true;
            } else if (active == leastActive) {

                // 当前instance的活跃数 与 leastActive相等

                // 记录索引位置，具有相同最小活跃数的计数器 +1
                leastIndexes[leastCount++] = i;

                //总权重 =  总权重 + 当前权重
                totalWeight += weight;
                if (sameWeight && i > 0
                        && weight != firstWeight) {
                    sameWeight = false;
                }
            }
        }
        // assert(leastCount > 0)
        if (leastCount == 1) {//如果我们恰好有一个调用程序具有最少的活动值，那么直接返回这个调用程序。
            // If we got exactly one instance having the least active value, return this instance directly.
            activeCache.get(instances.get(leastIndexes[0]).getServiceInstanceId(),k->{return new LongAdder();}).add(1);
            return instances.get(leastIndexes[0]);
        }
        // -----------------------------------------------------------------------------------------------------------
        // 如果每个instance有不同的权重 &&  totalWeight > 0
        if (!sameWeight && totalWeight > 0) {
            // If (not every instance has the same weight & at least one instance's weight>0), select randomly based on totalWeight.

            // 在totalWeight 范围内随机一个值
            int offsetWeight = ThreadLocalRandom.current().nextInt(totalWeight) + 1;
            // Return a instance based on the random value.
            for (int i = 0; i < leastCount; i++) {
                // 获取i位置的那个最小活跃 在instances 里面的位置信息
                int leastIndex = leastIndexes[i];

                //offsetWeight - leastIndex 位置instance的权重
                offsetWeight -= instances.get(leastIndex).getWeight();

                // offsetWeight 小于0的话
                if (offsetWeight <= 0)
                    activeCache.get(instances.get(leastIndex).getServiceInstanceId(),k->{return new LongAdder();}).add(1);
                    // 返回这个位置的这个
                    return instances.get(leastIndex);
            }
        }
        ServiceInstance serviceInstance = instances.get(leastIndexes[ThreadLocalRandom.current().nextInt(leastCount)]);
        activeCache.get(serviceInstance.getServiceInstanceId(),k->{return new LongAdder();}).add(1);
        // 具有相同权重或者是 总权重=0 的话就均匀返回
        return serviceInstance;

    }
}
