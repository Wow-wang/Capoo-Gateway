package org.wow.core.filter.flowCtl;

import org.wow.common.config.Rule;
import org.wow.core.context.GatewayContext;

/**
 * 执行限流的接口
 */
public interface IGatewayFlowCtlRule {

    void doFlowCtlFilter(Rule.FlowCtlConfig flowCtlConfig, String serviceId);
}
