package org.wow.core.filter.loadbalance;

import org.wow.common.config.ServiceInstance;
import org.wow.core.context.GatewayContext;

/**
 * @program: api-gateway
 * @description: 负载均衡顶级接口
 * @author: wow
 * @create: 2023-10-02 10:57
 **/

public interface IGatewayLoadBalanceRule {
    /**
     * 通过上下文参数获取服务实例
     * @param ctx
     * @return
     */
    ServiceInstance choose(GatewayContext ctx);


    /**
     * 通过uniqueID拿到对应的服务实例
     *
     * @param serviceId
     * @param gray
     * @return
     */
    ServiceInstance choose(String uniqueId, boolean gray);

}
