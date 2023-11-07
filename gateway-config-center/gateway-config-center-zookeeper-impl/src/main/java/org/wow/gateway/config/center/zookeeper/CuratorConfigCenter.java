package org.wow.gateway.config.center.zookeeper;

import com.alibaba.fastjson.JSON;
import org.apache.curator.RetryPolicy;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.framework.recipes.cache.*;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.wow.common.config.DynamicConfigManager;
import org.wow.common.config.Rule;
import org.wow.common.config.ServiceDefinition;
import org.wow.common.config.ServiceInstance;
import org.wow.gateway.config.center.api.ConfigCenter;
import org.wow.gateway.config.center.api.RulesChangeListener;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static org.wow.common.constants.BasicConst.PATH_SEPARATOR;
import static org.wow.common.constants.CenterConst.*;

/**
 * @program: Capoo-Api-gateway
 * @description:
 * @author: wow
 * @create: 2023-11-07 17:03
 **/

public class CuratorConfigCenter implements ConfigCenter {

    private CuratorFramework client;

    private static  String PATH = "/api-gateway-config";



    private String env;

    private static byte[] data;

    @Override
    public void init(String env) {
        this.env = env;
        RetryPolicy retryPolicy = new ExponentialBackoffRetry(1000, 3);
        this.client =
                CuratorFrameworkFactory.newClient(
                        ZOOKEEPER_REGISTER_ADDRESS,
                        5000,
                        40000,
                        retryPolicy);
        client.start();
        PATH = PATH + "/" + env;
        String newPath = PATH + "/" + RULENAME;
        try {
            String Config = new String(Files.readAllBytes(Paths.get(FILEPATH)), StandardCharsets.UTF_8);
            byte[] newData = Config.getBytes(StandardCharsets.UTF_8);
            if (client.checkExists().forPath(newPath) == null) {
                // 如果路径不存在，创建它
                client.create().creatingParentsIfNeeded().forPath(newPath,newData);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void subscribeRulesChange(RulesChangeListener listener) {
        doSubscribeRulesChange(listener);
        TreeCache cache = new TreeCache(client, PATH);
        cache.getListenable().addListener(new TreeCacheListener() {

            @Override
            public void childEvent(CuratorFramework client, TreeCacheEvent  event) throws Exception {
                switch (event.getType()) {
                    case NODE_ADDED:
                        // 处理服务定义节点新增事件
                        // event.getData() 包含新增的节点信息
                        byte[] data = event.getData().getData(); // 获取节点数据
                        String configString = new String(data, StandardCharsets.UTF_8);
                        String path = event.getData().getPath();
                        path = path.substring(PATH.length()+1);
                        List<Rule> rules = JSON.parseObject(configString).getJSONArray("rules").toJavaList(Rule.class);
                        // 添加当前path下面所有rule到内存里
                        DynamicConfigManager.getInstance().putNewRules(path,rules);
                        listener.onRulesChange(null);
                        break;
                    case NODE_REMOVED:
                        path = event.getData().getPath();
                        path = path.substring(PATH.length()+1);
                        DynamicConfigManager.getInstance().deleteRules(path);
                        listener.onRulesChange(null);
                        break;
                    case NODE_UPDATED: //包含删除部分
                        // 处理节点或子节点数据更新事件
                        path = event.getData().getPath();
                        path = path.substring(PATH.length()+1);
                        data = event.getData().getData(); // 获取节点数据
                        configString = new String(data, StandardCharsets.UTF_8);
                        rules = JSON.parseObject(configString).getJSONArray("rules").toJavaList(Rule.class);
                        DynamicConfigManager.getInstance().deleteRules(path);
                        DynamicConfigManager.getInstance().putNewRules(path,rules);
                        listener.onRulesChange(null);
                        break;
                    default:
                        // 其他事件类型
                        break;
                }


            }
        });

        try {
            cache.start();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }


    }



    private void doSubscribeRulesChange(RulesChangeListener listener) {
        Map<String,List<Rule>> ruleMap = new ConcurrentHashMap<>();
        try {
            List<Rule> ruleFinal = new ArrayList<>();
            List<String> configs = client.getChildren().forPath(PATH);
            for(String configPath : configs){
                byte[] data = client.getData().forPath(PATH + "/" + configPath);
                if(data == null){
                    continue;
                }
                String configString = new String(data, StandardCharsets.UTF_8);
                ruleFinal.addAll(JSON.parseObject(configString).getJSONArray("rules").toJavaList(Rule.class));
                ruleMap.put(configPath,ruleFinal);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        listener.onRulesChange(ruleMap);
    }
}
