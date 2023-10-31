package org.wow.core.netty.datasourece.unpooled;



import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.apache.dubbo.config.ApplicationConfig;
import org.apache.dubbo.config.ReferenceConfig;
import org.apache.dubbo.rpc.service.GenericService;
import org.wow.core.netty.datasourece.Connection;
import org.wow.core.netty.datasourece.connection.DubboConnection;

import java.util.concurrent.TimeUnit;



public class UnpooledDataSource {


    private final Cache<String, Connection> serviceCache = Caffeine.newBuilder().recordStats().expireAfterWrite(10, TimeUnit.MINUTES).build();

    static UnpooledDataSource INSTANCE = new UnpooledDataSource();

    static public UnpooledDataSource getInstance(){
        return INSTANCE;
    }
    public Connection getConnection(String address,String interfaceName) {
        return serviceCache.get(address, k -> {
            // 服务引用 注意JVM参数
            ReferenceConfig<GenericService> reference = new ReferenceConfig<>();
            ApplicationConfig application = new ApplicationConfig();
            application.setName("pangu-client-consumer-generic");
            reference.setUrl("dubbo://" + address);
            reference.setInterface(interfaceName);
            reference.setGeneric(true);  // 泛化
            reference.setApplication(application);
            reference.setAsync(true);    // 异步
            reference.setCheck(true);
            reference.setCache("lru");
            return new DubboConnection(reference);
        });
    }

    public static void main(String[] args) {
        ReferenceConfig<GenericService> reference = new ReferenceConfig<>();
        ApplicationConfig application = new ApplicationConfig();
        application.setName("pangu-client-consumer-generic");
        reference.setUrl("dubbo://" + "10.180.172.27:20881");
        reference.setInterface("org.producer.service.HelloServiceAPI");
        reference.setApplication(application);
        reference.setGeneric(true);  // 泛化
        reference.setId("222222");
        reference.setAsync(true);    // 异步
        reference.setCache("lru");
        new DubboConnection(reference);
    }



}
