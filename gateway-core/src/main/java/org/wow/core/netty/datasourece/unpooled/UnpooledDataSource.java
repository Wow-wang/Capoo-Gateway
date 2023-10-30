package org.wow.core.netty.datasourece.unpooled;


import org.apache.dubbo.config.ApplicationConfig;
import org.apache.dubbo.config.ReferenceConfig;
import org.apache.dubbo.config.RegistryConfig;
import org.apache.dubbo.rpc.service.GenericService;
import org.wow.core.netty.datasourece.Connection;
import org.wow.core.netty.datasourece.DataSource;
import org.wow.core.netty.datasourece.connection.DubboConnection;
import org.wow.core.netty.mapping.HttpStatement;
import org.wow.core.netty.session.Configuration;

/**
 * @author 小傅哥，微信：fustack
 * @description 无池化的连接池
 * @github https://github.com/fuzhengwei
 * @Copyright 公众号：bugstack虫洞栈 | 博客：https://bugstack.cn - 沉淀、分享、成长，让自己和他人都能有所收获！
 */
public class UnpooledDataSource implements DataSource {



    @Override
    public Connection getConnection() {
//        switch (dataSourceType) {
//            case HTTP:
//                // TODO 预留接口，暂时不需要实现
//                break;
//            case Dubbo:
                // 配置信息
                String application = httpStatement.getApplication();
                String interfaceName = httpStatement.getInterfaceName();
                // 获取服务
                ApplicationConfig applicationConfig = configuration.getApplicationConfig(application);
                RegistryConfig registryConfig = configuration.getRegistryConfig(application);
                ReferenceConfig<GenericService> reference = configuration.getReferenceConfig(interfaceName);
                return new DubboConnection(applicationConfig, registryConfig, reference);
//            default:
//                break;
//        }
//        throw new RuntimeException("DataSourceType：" + dataSourceType + "没有对应的数据源实现");
    }



}
