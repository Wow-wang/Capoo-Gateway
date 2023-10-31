package org.wow.gateway.client.core;

import java.lang.annotation.*;

/**
 * 服务定义
 */
@Target(ElementType.TYPE) // 这个注解应用到类上
@Retention(RetentionPolicy.RUNTIME) // 该注解会在运行时保留
@Documented // 这个元注解表示这个自定义注解应该包含在生成的Java文档
public @interface ApiService {
    /**
     * 定义注解所需要指定的元素
     * @return
     */
    String serviceId();

    String version() default "1.0.0";

    ApiProtocol protocol();

    String patternPath();

    String interfaceName() default "";

}
