package org.wow.core.filter.flowCtl;

import com.google.common.util.concurrent.RateLimiter;
import org.apache.commons.lang3.StringUtils;
import org.wow.common.config.Rule;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * @program: api-gateway
 * @description: 单机限流
 * @author: wow
 * @create: 2023-10-03 16:18
 **/

public class GuavaCountLimiter {

    /**
     * RateLimiter 的工作原理是通过令牌桶算法或漏桶算法来控制速率
     * 它会按照固定的速率生成令牌，并在请求到达时检查是否有足够的令牌可用
     * 如果有足够的令牌，请求将被立即处理
     * 否则，请求将等待，直到有足够的令牌可用或达到超时时间
     */
    private RateLimiter rateLimiter;
    private double maxPermits;

    public GuavaCountLimiter(double maxPermits) {
        this.rateLimiter = RateLimiter.create(maxPermits);
        this.maxPermits = maxPermits;
    }

    public GuavaCountLimiter(double maxPermits,long warmUpPeroidAsSecond) {
        this.rateLimiter = RateLimiter.create(maxPermits,warmUpPeroidAsSecond, TimeUnit.SECONDS);
        this.maxPermits = maxPermits;
    }

    public static ConcurrentHashMap<String,GuavaCountLimiter> resourceRateLimiterMap = new ConcurrentHashMap<>();

    public static GuavaCountLimiter getInstance(String serviceId , Rule.FlowCtlConfig flowCtlConfig){
        if(StringUtils.isEmpty(serviceId) || flowCtlConfig ==null ||
                StringUtils.isEmpty(flowCtlConfig.getValue()) ||
                StringUtils.isEmpty(flowCtlConfig.getConfig()) ||
                StringUtils.isEmpty(flowCtlConfig.getType())){
            return null;
        }
        StringBuffer buffer = new StringBuffer();
        String key = buffer.append(serviceId).append(".").append(flowCtlConfig.getValue()).toString();
        GuavaCountLimiter countLimiter = resourceRateLimiterMap.get(key);
        if(countLimiter == null){
            countLimiter = new GuavaCountLimiter(100);
            resourceRateLimiterMap.putIfAbsent(key,countLimiter);
        }
        return countLimiter;
    }

    public boolean acquire(int permits){
        boolean success = rateLimiter.tryAcquire(permits);
        if(success){
            return true;
        }
        return false;
    }
}
