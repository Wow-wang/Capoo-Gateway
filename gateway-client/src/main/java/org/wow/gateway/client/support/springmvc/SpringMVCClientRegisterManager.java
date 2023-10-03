package org.wow.gateway.client.support.springmvc;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactoryUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.web.ServerProperties;
import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;
import org.wow.common.config.ServiceDefinition;
import org.wow.common.config.ServiceInstance;
import org.wow.common.utils.NetUtils;
import org.wow.common.utils.TimeUtil;
import org.wow.gateway.client.core.ApiAnnotationScanner;
import org.wow.gateway.client.core.ApiProperties;
import org.wow.gateway.client.support.AbstractClientRegisterManager;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.wow.common.constants.BasicConst.COLON_SEPARATOR;
import static org.wow.common.constants.GatewayConst.DEFAULT_WEIGHT;

/**
 * @program: api-gateway
 * @description: 下游MVC服务实现注册中心接入
 * @author: wow
 * @create: 2023-09-30 09:23
 **/

/**
 * ApplicationListener 用于监听和处理应用程序事件ApplicationEvent
 * 当某个应用程序事件发生时，实现了 ApplicationListener 接口的类可以监听并响应这些事件
 * 需要重写 onApplicationEvent 方法，以定义在事件发生时应该执行的操作
 *
 * 实现了 ApplicationContextAware 接口的类可以通过 setApplicationContext 方法获得对应用程序上下文的引用。
 * 可以在类中访问和操作 Spring 容器中的 Bean。
 */
@Slf4j
public class SpringMVCClientRegisterManager extends AbstractClientRegisterManager implements ApplicationListener<ApplicationEvent>, ApplicationContextAware {

    private ApplicationContext applicationContext;

    @Autowired
    private ServerProperties serverProperties;

    private Set<Object> set = new HashSet<>();

    public SpringMVCClientRegisterManager(ApiProperties apiProperties) {
        super(apiProperties);
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    /**
     * SpringMVC可以直接监听启动事件
     * @param applicationEvent
     */
    @Override
    public void onApplicationEvent(ApplicationEvent applicationEvent) {
        if(applicationEvent instanceof ApplicationStartedEvent){
            try {
                doRegisterSpringMVC();
            } catch (Exception e) {
                log.error("doRegisterSpringMVC error",e);
                throw new RuntimeException(e);
            }
            log.info("SpringMVC api started");
        }
    }


    private void doRegisterSpringMVC() {
        /**
         * 从 Spring 容器中获取指定类型的 bean
         */
        Map<String, RequestMappingHandlerMapping> allRequestMappings = BeanFactoryUtils.beansOfTypeIncludingAncestors
                (applicationContext, RequestMappingHandlerMapping.class, true, false);

        for (RequestMappingHandlerMapping handlerMapping : allRequestMappings.values()) {
            Map<RequestMappingInfo, HandlerMethod> handlerMethods = handlerMapping.getHandlerMethods();

            for (Map.Entry<RequestMappingInfo, HandlerMethod> me : handlerMethods.entrySet()) {
                HandlerMethod handlerMethod = me.getValue();
                /**
                 * 将 handlerMethod 所关联的控制器类的类型信息存储在 clazz 变量中，
                 * 以便后续可以使用反射等机制来访问和操作这个控制器类
                 */
                Class<?> clazz = handlerMethod.getBeanType();

                Object bean = applicationContext.getBean(clazz);
                if(set.contains(bean)){
                    continue;
                }

                // 返回注册好的服务定义
                ServiceDefinition serviceDefinition = ApiAnnotationScanner.getInstance().scanner(bean);

                if(serviceDefinition == null){
                    continue;
                }

                serviceDefinition.setEnvType(getApiProperties().getEnv());

                //服务实例
                ServiceInstance serviceInstance = new ServiceInstance();
                String localIp = NetUtils.getLocalIp();
                int port = serverProperties.getPort();
                String serviceInstanceId = localIp + COLON_SEPARATOR + port;
                String uniqueId = serviceDefinition.getUniqueId();
                String version = serviceDefinition.getVersion();

                serviceInstance.setServiceInstanceId(serviceInstanceId);
                serviceInstance.setUniqueId(uniqueId);
                serviceInstance.setIp(localIp);
                serviceInstance.setPort(port);
                serviceInstance.setRegisterTime(TimeUtil.currentTimeMillis());
                serviceInstance.setVersion(version);
                serviceInstance.setWeight(DEFAULT_WEIGHT);

                //注册
                register(serviceDefinition,serviceInstance);

            }
        }
    }
}
