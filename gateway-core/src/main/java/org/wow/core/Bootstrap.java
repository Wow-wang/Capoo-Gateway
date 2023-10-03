package org.wow.core;

import com.alibaba.fastjson.JSON;
import lombok.extern.slf4j.Slf4j;
import org.wow.common.config.DynamicConfigManager;
import org.wow.common.config.ServiceDefinition;
import org.wow.common.config.ServiceInstance;
import org.wow.common.utils.NetUtils;
import org.wow.common.utils.TimeUtil;
import org.wow.gateway.config.center.api.ConfigCenter;
import org.wow.gateway.register.center.api.RegisterCenter;
import org.wow.gateway.register.center.api.RegisterCenterListener;

import java.util.Map;
import java.util.ServiceLoader;
import java.util.Set;

import static org.wow.common.constants.BasicConst.COLON_SEPARATOR;


@Slf4j
public class Bootstrap {
    public static void main(String[] args) {
        // 加载网关核心静态配置
        Config config = ConfigLoader.getInstance().load(args);
        System.out.println(config.getPort());
        System.out.println(config.getRegistryAddress());
        System.out.println(config.getEnv());

        //插件初始化
        //配置中心管理器初始化 连接配置中心，监听配置的新增 修改 删除
        ServiceLoader<ConfigCenter> serviceLoader = ServiceLoader.load(ConfigCenter.class);

        final ConfigCenter configCenter = serviceLoader.findFirst().orElseThrow(() -> {
            log.error("not found ConfigCenter impl");
            return new RuntimeException("not found ConfigCenter impl");
        });
        configCenter.init(config.getRegistryAddress(), config.getEnv());
        /**
         * listener 参数实际上是用于在 configCenter.subscribeRulesChange 方法内部执行规则变更操作的回调函数。
         * Lambda 表达式 (rules -> DynamicConfigManager.getInstance().putAllRule(rules))
         * 作为回调函数传递给了 listener，以便在规则变更时调用它
         */
        configCenter.subscribeRulesChange(rules -> DynamicConfigManager.getInstance()
                .putAllRule(rules));


        // 启动容器
        Container container = new Container(config);
        container.start();

        // 连接注册中心 将注册中心的实例加载到本地
        final RegisterCenter registerCenter = registerAndSubscribe(config);

        // 服务优雅关机
        // 收到kill信号时候调用
        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                registerCenter.deregister(buildGatewayServiceDefinition(config),
                        buildGatewayServiceInstance(config));
                container.shutdown();
            }
        });
    }

    private static RegisterCenter registerAndSubscribe(Config config) {
        ServiceLoader<RegisterCenter> serviceLoader = ServiceLoader.load(RegisterCenter.class);
        final RegisterCenter registerCenter = serviceLoader.findFirst().orElseThrow(() -> {
            log.error("not found RegisterCenter impl");
            return new RuntimeException("not found RegisterCenter impl");
        });
        registerCenter.init(config.getRegistryAddress(), config.getEnv());


        //构造网关服务定义和服务实例
        ServiceDefinition serviceDefinition = buildGatewayServiceDefinition(config);
        ServiceInstance serviceInstance = buildGatewayServiceInstance(config);

        //注册
        registerCenter.register(serviceDefinition, serviceInstance);
        log.info("注册的serviceDefinition：{}",serviceDefinition);

        //订阅
        registerCenter.subscribeAllServices(new RegisterCenterListener() {
            @Override
            public void onChange(ServiceDefinition serviceDefinition, Set<ServiceInstance> serviceInstanceSet) {
                log.info("refresh service and instance: {} {}", serviceDefinition.getUniqueId(),
                        JSON.toJSON(serviceInstanceSet));
                DynamicConfigManager manager = DynamicConfigManager.getInstance();
                manager.addServiceInstance(serviceDefinition.getUniqueId(), serviceInstanceSet);
                manager.putServiceDefinition(serviceDefinition.getUniqueId(),serviceDefinition);
            }
        });
        return registerCenter;
    }

    private static ServiceInstance buildGatewayServiceInstance(Config config) {
        String localIp = NetUtils.getLocalIp();
        int port = config.getPort();
        ServiceInstance serviceInstance = new ServiceInstance();
        serviceInstance.setServiceInstanceId(localIp + COLON_SEPARATOR + port);
        serviceInstance.setIp(localIp);
        serviceInstance.setPort(port);
        serviceInstance.setRegisterTime(TimeUtil.currentTimeMillis());
        return serviceInstance;
    }

    private static ServiceDefinition buildGatewayServiceDefinition(Config config) {
        ServiceDefinition serviceDefinition = new ServiceDefinition();
        serviceDefinition.setInvokerMap(Map.of());
        serviceDefinition.setUniqueId(config.getApplicationName());
        serviceDefinition.setServiceId(config.getApplicationName());
        serviceDefinition.setEnvType(config.getEnv());
        return serviceDefinition;
    }
}
