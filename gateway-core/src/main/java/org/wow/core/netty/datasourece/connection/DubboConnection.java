package org.wow.core.netty.datasourece.connection;


import org.apache.dubbo.config.ReferenceConfig;
import org.apache.dubbo.config.bootstrap.DubboBootstrap;
import org.apache.dubbo.config.utils.ReferenceConfigCache;
import org.apache.dubbo.rpc.RpcContext;
import org.apache.dubbo.rpc.service.GenericService;
import org.wow.core.netty.datasourece.Connection;
import org.wow.core.netty.datasourece.SessionResult;

import java.util.concurrent.*;


public class DubboConnection implements Connection {

    private final GenericService genericService;
    public DubboConnection(ReferenceConfig<GenericService> reference) {
//         连接远程服务
//        DubboBootstrap bootstrap = DubboBootstrap.getInstance();
//        bootstrap.reference(reference).start();
        // 获取泛化接口
//        genericService = reference.get();
        ReferenceConfigCache cache = ReferenceConfigCache.getCache();
        genericService = cache.get(reference);
    }


    /**
     * Dubbo 泛化调用：https://dubbo.apache.org/zh/docsv2.7/user/examples/generic-reference/
     */
    @Override
    public Object execute(String method, String[] parameterTypes,  Object[] args) {
        genericService.$invoke(method, parameterTypes, args);
        //获取结果
        CompletableFuture<String> future = RpcContext.getContext().getCompletableFuture();
        String result;
        try {
            result = future.get(5000, TimeUnit.MILLISECONDS);
        } catch (TimeoutException | ExecutionException | InterruptedException e) {
            result ="超时无响应";
        }
        return result;
    }

}
