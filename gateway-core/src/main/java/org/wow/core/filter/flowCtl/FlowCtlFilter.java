package org.wow.core.filter.flowCtl;

import com.alibaba.csp.sentinel.slots.block.RuleConstant;
import com.alibaba.csp.sentinel.slots.block.flow.FlowRule;
import lombok.extern.slf4j.Slf4j;
import org.wow.common.config.Rule;
import org.wow.core.context.GatewayContext;
import org.wow.core.filter.Filter;
import org.wow.core.filter.FilterAspect;
import org.wow.core.request.GatewayRequest;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import static org.wow.common.constants.FilterConst.*;
/**
 * @program: api-gateway
 * @description: 限流流控过滤器
 * @author: wow
 * @create: 2023-10-03 15:45
 **/
@Slf4j
@FilterAspect(id = FLOW_CTL_FILTER_ID,
        name = FLOW_CTL_FILTER_NAME,
        order = FLOW_CTL_FILTER_ORDER)
public class FlowCtlFilter implements Filter {
    @Override
    public void doFilter(GatewayContext ctx) throws Exception {

        Rule rule = ctx.getRule();
        if(rule != null){
            Set<Rule.FlowCtlConfig> flowCtlConfigs = rule.getFlowCtlConfigs();
            Iterator iterator = flowCtlConfigs.iterator();
            Rule.FlowCtlConfig flowCtlConfig;
            while(iterator.hasNext()){
                IGatewayFlowCtlRule flowCtlRule = null;
                flowCtlConfig = (Rule.FlowCtlConfig)iterator.next();
                if(flowCtlConfig == null){
                    continue;
                }
                String path = ctx.getRequest().getPath();
                if(flowCtlConfig.getType().equalsIgnoreCase(FLOW_CTL_TYPE_PATH) &&
                path.equals(flowCtlConfig.getValue())){
                    flowCtlRule = FlowCtlByPathRule.getInstance(rule.getServiceId(),path);
                }else if(flowCtlConfig.getType().equalsIgnoreCase(FLOW_CTL_TYPE_SERVICE)){

                    GatewayRequest request = ctx.getRequest();
                    if(request == null){
                        throw new RuntimeException("没有服务对象");
                    }

                    flowCtlRule = FlowCtlByServiceRule.getInstance(rule.getServiceId());
                }
                if(flowCtlRule != null){
                    flowCtlRule.doFlowCtlFilter(flowCtlConfig,rule.getServiceId());
                }
            }
        }

    }
}
