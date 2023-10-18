package org.wow.core.filter;


import org.wow.common.constants.GatewayConst;
import org.wow.core.context.GatewayContext;

/**
 * 工厂接口
 */
public interface FilterFactory {

    /**
     * 构建过滤器链条
     * @param ctx
     * @return
     * @throws Exception
     */
    GatewayFilterChain buildFilterChain(GatewayContext ctx) throws Exception;


    /**
     * 通过过滤器ID获取过滤器
     * @param filterId
     * @return
     * @param <T>
     * @throws Exception
     */
    Filter getFilterInfo(String filterId) throws Exception;

}
