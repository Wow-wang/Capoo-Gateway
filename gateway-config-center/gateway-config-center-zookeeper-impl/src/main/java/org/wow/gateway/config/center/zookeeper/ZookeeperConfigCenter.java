package org.wow.gateway.config.center.zookeeper;

import com.alibaba.fastjson.JSON;
import org.apache.zookeeper.*;
import org.apache.zookeeper.data.Stat;
import org.apache.zookeeper.ZooDefs;
import org.wow.common.config.DynamicConfigManager;
import org.wow.common.config.Rule;
import org.wow.gateway.config.center.api.ConfigCenter;
import org.wow.gateway.config.center.api.RulesChangeListener;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static org.wow.common.constants.CenterConst.*;

/**
 * @program: api-gateway
 * @description:
 * @author: wow
 * @create: 2023-10-09 22:23
 **/

public class ZookeeperConfigCenter implements ConfigCenter {
    private ZooKeeper zooKeeper;

    private static final String PATH = "/api-gateway-config";


    private String env;


    @Override
    public void init(String env) {
        Stat stat;
        try {
            zooKeeper = new ZooKeeper(ZOOKEEPER_REGISTER_ADDRESS,40000,null);
            this.env = env;
            stat = zooKeeper.exists(PATH+env,null);
            String Config = null;
            Config = new String(Files.readAllBytes(Paths.get(FILEPATH)), StandardCharsets.UTF_8);
            if(stat == null) {
                String result = zooKeeper.create(
                        PATH + env,
                        Config.getBytes(StandardCharsets.UTF_8),
                        ZooDefs.Ids.OPEN_ACL_UNSAFE,
                        CreateMode.PERSISTENT
                );
            }else{
                zooKeeper.setData(
                        PATH + env,
                        Config.getBytes(StandardCharsets.UTF_8),
                        -1
                );
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (InterruptedException ex) {
            throw new RuntimeException(ex);
        } catch (KeeperException ex) {
            throw new RuntimeException(ex);
        }


    }

    @Override
    public void subscribeRulesChange(RulesChangeListener listener) {

            registerWatcher(listener);
            // 可能有新服务加入 所以需要一个定时任务来检查 也是发送心跳
            ScheduledExecutorService scheduledThreadPool = Executors.newScheduledThreadPool(1);

            // 每次任务开始执行时 安排一个任务按照固定的时间间隔执行
            scheduledThreadPool.scheduleWithFixedDelay(()->registerWatcher(listener),
                    10,10, TimeUnit.SECONDS);
    }

    private void registerWatcher(RulesChangeListener listener)  {
        // 使用getData方法注册Watcher
        try {
            Stat stat = zooKeeper.exists(PATH+env, null
    //                new Watcher() {
    //            @Override
    //            public void process(WatchedEvent event) {
    //                // 处理节点变化事件
    //                if (event.getType() == Event.EventType.NodeDataChanged) {
    //                    System.out.println("Node data changed.");
    //
    //                    try {
    //                        // 重新注册Watcher
    //                        registerWatcher(listener);
    //                    } catch (KeeperException | InterruptedException e) {
    //                        e.printStackTrace();
    //                    }
    //                }
    //            }
    //        }
            );

            if (stat != null) {
                // 节点存在，开始监听节点的数据变化
                byte[] data = zooKeeper.getData(PATH+env, false, stat);
                String config = new String(data, StandardCharsets.UTF_8);
                System.out.println("Node data: " + new String(data));
                List<Rule> rules = JSON.parseObject(config).getJSONArray("rules").toJavaList(Rule.class);
                listener.onRulesChange(rules);
            } else {
                System.out.println("Node does not exist.");
            }
        } catch (KeeperException e) {
            throw new RuntimeException(e);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    public static void main(String[] args) throws InterruptedException, KeeperException {
        ZookeeperConfigCenter zookeeperConfigCenter = new ZookeeperConfigCenter();
        zookeeperConfigCenter.init("dev");
        zookeeperConfigCenter.subscribeRulesChange(rules -> DynamicConfigManager.getInstance()
                .putAllRule(rules));
        List<String> list = zookeeperConfigCenter.zooKeeper.getChildren("/", false);
        for (String string : list) {
            System.out.println(string);
        }
        Thread.sleep(100000);


    }
}
