package org.wow.core.filter.loadbalance;

import com.alibaba.fastjson.JSON;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.wow.common.config.Rule;
import org.wow.common.config.ServiceInstance;
import org.wow.common.enums.ResponseCode;
import org.wow.common.exception.NotFoundException;
import org.wow.core.context.GatewayContext;
import org.wow.core.filter.Filter;
import org.wow.core.filter.FilterAspect;
import org.wow.core.request.GatewayRequest;
import org.wow.core.request.IGatewayRequest;

import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import static org.wow.common.constants.FilterConst.*;

/**
 * @program: api-gateway
 * @description:
 * @author: wow
 * @create: 2023-10-02 10:39
 **/

@FilterAspect(id = LOAD_BALANCE_FILTER_ID,name = LOAD_BALANCE_FILTER_NAME,
order = LOAD_BALANCE_FILTER_ORDER)
@Slf4j
public class LoadBalanceFilter implements Filter {
    @Override
    public void doFilter(GatewayContext ctx) throws Exception {
        String serviceId = ctx.getUniqueId();
        IGatewayLoadBalanceRule gatewayLoadBalanceRule = getLoadBalanceRule(ctx);
        if(gatewayLoadBalanceRule == null) return;
        ServiceInstance serviceInstance = gatewayLoadBalanceRule.choose(serviceId,ctx.isGray());
        GatewayRequest request = ctx.getRequest();
        if(serviceInstance != null && request != null){
            String host = serviceInstance.getIp() + ":" + serviceInstance.getPort();
            request.setModifyHost(host);
        }else{
            log.warn("No instance available for : {}" , serviceId);
            throw new NotFoundException(ResponseCode.SERVICE_INSTANCE_NOT_FOUND);
        }
    }


    /**
     * 根据配置获取负载均衡器
     * @return
     */
    public IGatewayLoadBalanceRule getLoadBalanceRule(GatewayContext ctx){

        IGatewayLoadBalanceRule loadBalanceRule = null;
        Rule configRule = ctx.getRule();
        if(configRule != null){
            Set<Rule.FilterConfig> filterConfigs = configRule.getFilterConfigs();
            Iterator iterator = filterConfigs.iterator();
            Rule.FilterConfig filterConfig;
            while(iterator.hasNext()){
                filterConfig = (Rule.FilterConfig)iterator.next();
                if(filterConfig == null){
                    continue;
                }
                String filterId = filterConfig.getId();
                if(filterId.equals(LOAD_BALANCE_FILTER_ID)){
                    String config = filterConfig.getConfig();
                    String strategy = "LOAD_BALANCE_STRATEGY_RANDOM"; // 默认策略是随机
                    Map<String,String> mayTypeMap = null;
                    if(!StringUtils.isEmpty(config)){
                        mayTypeMap = JSON.parseObject(config,Map.class);
                        strategy = mayTypeMap.get(LOAD_BALANCE_KEY);
                    }
                    switch (strategy) {
                        case LOAD_BALANCE_STRATEGY_RANDOM:
                            loadBalanceRule = RandomLoadBalanceRule.getInstance(configRule.getServiceId());
                            break;
                        case LOAD_BALANCE_STRATEGY_ROUND_ROBIN:
                            loadBalanceRule = RoundRobinLoadBalanceRule.getInstance(configRule.getServiceId());
                            break;
                        case LOAD_BALANCE_STRATEGY_LEAST_ACTIVE:
                            loadBalanceRule = LeastActiveLoadBalanceRule.getInstance(configRule.getServiceId());
                            break;
                        case LOAD_BALANCE_CONSISTENT:
                            loadBalanceRule = null;
                            String host = mayTypeMap.get(HOST);
                            ctx.getRequest().setModifyHost(host);
                            break;
                        default:
                            log.warn("No loadBalance strategy for service:{}", strategy);
                            loadBalanceRule = RandomLoadBalanceRule.getInstance(configRule.getServiceId());
                            break;
                    }
                }
            }
        }
        return loadBalanceRule;
    }

}
