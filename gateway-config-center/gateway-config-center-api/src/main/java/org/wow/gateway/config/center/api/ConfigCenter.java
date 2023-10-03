package org.wow.gateway.config.center.api;

public interface ConfigCenter {
    void init(String serverAddr,String env);

    /**
     * 定义规则和方法
     */
    void subscribeRulesChange(RulesChangeListener listener);
}
