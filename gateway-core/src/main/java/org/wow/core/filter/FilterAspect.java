package org.wow.core.filter;

import java.lang.annotation.*;

/**
 * 过滤器注解类
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE})
public @interface FilterAspect {
    /**
     * 过滤器ID
     */
    String id();

    /**
     * 过滤器名称
     */
    String name() default "";

    /**
     * 排序
     * @return
     */
    int order() default 0;

}
