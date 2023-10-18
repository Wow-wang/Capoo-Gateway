package org.wow.core.filter.flowCtl;

import com.alibaba.fastjson.JSON;
import org.apache.commons.lang3.StringUtils;
import org.wow.common.config.Rule;
import org.wow.core.redis.JedisUtil;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static org.wow.common.constants.FilterConst.*;

/**
 * @program: api-gateway
 * @description: 根据服务 更大
 * @author: wow
 * @create: 2023-10-03 16:03
 **/

public class FlowCtlByServiceRule implements IGatewayFlowCtlRule {
    private String serviceId;


    private static final String LIMIT_MESSAGE = "您的请求过于频繁 请稍后重试";

    private RedisCountLimiter redisCountLimiter;
    public FlowCtlByServiceRule(String serviceId,RedisCountLimiter redisCountLimiter) {
        this.serviceId = serviceId;
        this.redisCountLimiter = redisCountLimiter;
    }

    private static ConcurrentHashMap<String,FlowCtlByServiceRule> serviceMap = new ConcurrentHashMap<>();
    public static IGatewayFlowCtlRule getInstance(String serviceId) {
        String key = serviceId;
        FlowCtlByServiceRule FlowCtlByServiceRule = serviceMap.get(key);
        if(FlowCtlByServiceRule == null){
            FlowCtlByServiceRule = new FlowCtlByServiceRule(serviceId,new RedisCountLimiter(new JedisUtil()));
            serviceMap.put(key,FlowCtlByServiceRule);
        }
        return FlowCtlByServiceRule;
    }

    /**
     * 根据服务进行流控
     * @param flowCtlConfig
     * @param serviceId
     */
    @Override
    public void doFlowCtlFilter(Rule.FlowCtlConfig flowCtlConfig, String serviceId) {
        if(flowCtlConfig == null || StringUtils.isEmpty(serviceId) | StringUtils.isEmpty(flowCtlConfig.getConfig())){
            return;
        }
        Map<String,Integer> configMap = JSON.parseObject(flowCtlConfig.getConfig(),Map.class);
        if(! configMap.containsKey(FLOW_CTL_LIMIT_DURATION) || !configMap.containsKey(FLOW_CTL_LIMIT_PERMITS)){
            return;
        }
        double duration = configMap.get(FLOW_CTL_LIMIT_DURATION);
        double permits = configMap.get(FLOW_CTL_LIMIT_PERMITS);
        boolean flag = true;
        String key = serviceId;
        if(FLOW_CTL_MODEL_DISTRIBUTED.equalsIgnoreCase(flowCtlConfig.getModel())){
            flag = redisCountLimiter.doFlowCtl(key,(int)permits,(int)duration);

        }else{
            GuavaCountLimiter guavaCountLimiter = GuavaCountLimiter.getInstance(serviceId,flowCtlConfig);
            if(guavaCountLimiter == null){
                throw new RuntimeException("获取单机限流工具类为空");
            }
            double count = Math.ceil(permits/duration);
            flag = guavaCountLimiter.acquire((int)count);
        }
        if(!flag){
            throw new RuntimeException(LIMIT_MESSAGE);
        }
    }
}
