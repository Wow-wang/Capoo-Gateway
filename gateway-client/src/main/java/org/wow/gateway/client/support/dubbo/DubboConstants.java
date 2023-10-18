package org.wow.gateway.client.support.dubbo;

/**
 * Dubbo常量类
 *
 * 用键来检索 Dubbo 协议端口的配置值
 * 有助于代码的可维护性和降低硬编码的风险。
 */
public interface DubboConstants {

	String DUBBO_PROTOCOL_PORT = "dubbo.protocol.port";
	
	String DUBBO_APPLICATION_NAME = "dubbo.application.name";
	
	String DUBBO_REGISTERY_ADDRESS = "dubbo.registery.address";
	
	int DUBBO_TIMEOUT = 5000;
	
}
