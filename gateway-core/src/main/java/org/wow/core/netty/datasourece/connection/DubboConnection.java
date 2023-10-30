package org.wow.core.netty.datasourece.connection;

import org.apache.dubbo.config.ApplicationConfig;
import org.apache.dubbo.config.ReferenceConfig;
import org.apache.dubbo.config.RegistryConfig;
import org.apache.dubbo.config.bootstrap.DubboBootstrap;
import org.apache.dubbo.config.utils.ReferenceConfigCache;
import org.apache.dubbo.rpc.service.GenericService;
import org.wow.core.netty.datasourece.Connection;
import org.wow.core.netty.datasourece.SessionResult;


public class DubboConnection implements Connection {

    private final GenericService genericService;

    public DubboConnection(ReferenceConfig<GenericService> reference) {
        // 连接远程服务
        DubboBootstrap bootstrap = DubboBootstrap.getInstance();
        bootstrap.reference(reference).start();
        // 获取泛化接口
        ReferenceConfigCache cache = ReferenceConfigCache.getCache();
        genericService = cache.get(reference);
    }



    /**
     * Dubbo 泛化调用：https://dubbo.apache.org/zh/docsv2.7/user/examples/generic-reference/
     */
    @Override
    public SessionResult execute(String method, String[] parameterTypes, String[] parameterNames, Object[] args) {
        return (SessionResult)genericService.$invoke(method, parameterTypes, args);
    }

}
