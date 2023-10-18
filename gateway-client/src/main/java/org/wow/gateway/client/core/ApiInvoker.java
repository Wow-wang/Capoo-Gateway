package org.wow.gateway.client.core;


import java.lang.annotation.*;

/**
 * 必须要在服务的方法上面强制声明
 *
 */
@Target(ElementType.METHOD) // 这个注解应用到方法上
@Retention(RetentionPolicy.RUNTIME) // 该注解会在运行时保留
@Documented // 这个元注解表示这个自定义注解应该包含在生成的Java文档
public @interface ApiInvoker {
    String path();

}
