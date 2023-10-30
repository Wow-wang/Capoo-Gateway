package org.wow.core.netty.datasourece.unpooled;


import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.apache.dubbo.config.ApplicationConfig;
import org.apache.dubbo.config.ReferenceConfig;
import org.apache.dubbo.config.RegistryConfig;
import org.apache.dubbo.rpc.service.GenericService;
import org.wow.common.config.ServiceInstance;
import org.wow.core.netty.datasourece.Connection;
import org.wow.core.netty.datasourece.connection.DubboConnection;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;



public class UnpooledDataSource {


    private final Cache<String, Connection> serviceCache = Caffeine.newBuilder().recordStats().expireAfterWrite(10, TimeUnit.MINUTES).build();

    static UnpooledDataSource INSTANCE = new UnpooledDataSource();

    static public UnpooledDataSource getInstance(){
        return INSTANCE;
    }
    public Connection getConnection(String address) {
        return serviceCache.get(address,k->{
            // 服务引用
            ReferenceConfig<GenericService> reference = new ReferenceConfig<GenericService>();
            reference.setUrl("dubbo://"+ address);
            reference.setGeneric(true);  // 泛化
            reference.setAsync(true);    // 异步
            reference.setCache("lru");
            return new DubboConnection(reference);
        });
    }



}
