package org.wow.core.filter;

import org.wow.common.constants.GatewayConst;
import org.wow.core.context.GatewayContext;

/**
 * 过滤器顶级接口
 *
 */
public interface Filter {

    void doFilter(GatewayContext ctx) throws Exception;

    /**
     * 1. default 是一个关键字，通常用于接口中的方法声明或接口中的接口方法的默认实现
     * 2. 通过反射来检查当前类（调用该代码的类）上是否存在 FilterAspect 注解
     * @return
     */
    default int getOrder(){
        FilterAspect annotation = this.getClass().getAnnotation(FilterAspect.class);
        if(annotation != null){
            return annotation.order();
        }
        return Integer.MAX_VALUE;
    }
}
