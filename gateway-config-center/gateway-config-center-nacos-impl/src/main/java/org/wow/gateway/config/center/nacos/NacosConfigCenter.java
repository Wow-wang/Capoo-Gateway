package org.wow.gateway.config.center.nacos;

import com.alibaba.fastjson.JSON;
import com.alibaba.nacos.api.NacosFactory;
import com.alibaba.nacos.api.PropertyKeyConst;
import com.alibaba.nacos.api.config.ConfigService;
import com.alibaba.nacos.api.config.listener.Listener;
import com.alibaba.nacos.api.exception.NacosException;
import lombok.extern.slf4j.Slf4j;
import org.wow.common.config.Rule;
import org.wow.gateway.config.center.api.ConfigCenter;
import org.wow.gateway.config.center.api.RulesChangeListener;

import java.util.List;
import java.util.Properties;
import java.util.concurrent.Executor;

/**
 * @program: api-gateway
 * @description: 配置信息 configService
 * @author: wow
 * @create: 2023-09-30 14:19
 **/

@Slf4j
public class NacosConfigCenter implements ConfigCenter {
    private static final String DATA_ID = "api-gateway";
    private String serverAddr;

    private String env;

    /**
     * nacos提供的专门用来做配置中心交互
     */
    private ConfigService configService;

    @Override
    public void init(String serverAddr1, String env) {
        this.serverAddr = serverAddr1;
        this.env = env;

        try {
//            Properties properties = new Properties();
//            properties.put("serverAddr", "192.168.56.1:8848");
            configService = NacosFactory.createConfigService(serverAddr);
//            String content = configService.getConfig(DATA_ID, env, 5000);
//            System.out.println(content);
            // 此时 configService 已经被正确初始化和连接到Nacos服务器
            log.info("configService 注册成功");
        } catch (NacosException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void subscribeRulesChange(RulesChangeListener listener) {
        try {
            // 初始化通知 从Nacos配置中心获取配置信息
            String config = configService.getConfig(DATA_ID, env, 5000);
            // {"rules":[{},{}]}
            log.info("config from nacos: {}",config);

            /**
             * 将一个 JSON 字符串解析为 Java 对象
             * 并将其中的一个 JSON 数组转换为一个 List<Rule> 类型的对象
             */
            List<Rule> rules = JSON.parseObject(config).getJSONArray("rules").toJavaList(Rule.class);
            for (Rule rule : rules) {
                log.info("{}",rule);
//                System.out.println(rule.getId());
            }
            // 这表示在订阅规则变化后，首先会通知监听器进行初始配置的处理 比如更新DynamicConfigManager的Rules集合
            listener.onRulesChange(rules);

            // 监听变化
            configService.addListener(DATA_ID, env, new Listener() {
                /**
                 * 异步处理配置信息
                 * @return
                 */
                @Override
                public Executor getExecutor() {
                    return null;
                }

                /**
                 * 用于接收从 Nacos 配置中心传递过来的配置信息
                 * @param configInfo
                 */
                @Override
                public void receiveConfigInfo(String configInfo) {
                    log.info("config from nacos: {}",configInfo);
                    List<Rule> rules = JSON.parseObject(configInfo).getJSONArray("rules").toJavaList(Rule.class);
                    listener.onRulesChange(rules);
                }
            });


        } catch (NacosException e) {
            throw new RuntimeException(e);
        }
    }
}
