package org.wow.gateway.client.support.dubbo;

import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.spring.ServiceBean;
import org.apache.dubbo.config.spring.context.event.ServiceBeanExportedEvent;
import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.wow.common.config.ServiceDefinition;
import org.wow.common.config.ServiceInstance;
import org.wow.common.utils.NetUtils;
import org.wow.common.utils.TimeUtil;
import org.wow.gateway.client.core.ApiAnnotationScanner;
import org.wow.gateway.client.core.ApiProperties;
import org.wow.gateway.client.support.AbstractClientRegisterManager;

import java.util.HashSet;
import java.util.Set;

import static org.wow.common.constants.BasicConst.COLON_SEPARATOR;
import static org.wow.common.constants.GatewayConst.DEFAULT_WEIGHT;

/**
 * @program: api-gateway
 * @description: 下游Dubbo实现注册中心接入
 * @author: wow
 * @create: 2023-09-30 09:49
 **/

@Slf4j
public class Dubbo27ClientRegisterManager extends AbstractClientRegisterManager implements ApplicationListener<ApplicationEvent> {
    private Set<Object> set = new HashSet<>();
    public Dubbo27ClientRegisterManager(ApiProperties apiProperties) {
        super(apiProperties);
    }

    @Override
    public void onApplicationEvent(ApplicationEvent applicationEvent) {

        /**
         * ServiceBeanExportedEvent 是 Dubbo 框架中的一个事件类，用于表示 Dubbo 服务成功导出（即发布）的事件。
         * 在 Dubbo 中，服务提供者通常会将自己的服务导出，以便服务消费者可以访问和调用这些服务
         */
        if(applicationEvent instanceof ServiceBeanExportedEvent){
            try{
                /**
                 * 从事件对象中提取并获取与该事件相关的 ServiceBean 对象
                 */
                ServiceBean serviceBean = ((ServiceBeanExportedEvent) applicationEvent).getServiceBean();
                doRegisterDubbo(serviceBean);
            }catch(Exception e){
                log.error("doRegisterDubbo error",e);
                throw new RuntimeException(e);
            }
            // ApplicationStartedEvent 是 Spring Framework 中的一个事件，
            // 表示整个 Spring 应用程序已经启动完成的事件
        }else if(applicationEvent instanceof ApplicationStartedEvent){
            log.info("dubbo api started");
        }
    }

    private void doRegisterDubbo(ServiceBean serviceBean) {
        Object bean = serviceBean.getRef();
        if(set.contains(bean)){
            return;
        }
        ServiceDefinition serviceDefinition = ApiAnnotationScanner.getInstance().scanner(bean, serviceBean);
        if(serviceDefinition == null){
            return;
        }

        serviceDefinition.setEnvType(getApiProperties().getEnv());
        ServiceInstance serviceInstance = new ServiceInstance();
        String localIp = NetUtils.getLocalIp();
        int port = serviceBean.getProtocol().getPort();
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
