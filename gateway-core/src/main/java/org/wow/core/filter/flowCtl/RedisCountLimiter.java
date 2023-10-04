package org.wow.core.filter.flowCtl;

import lombok.extern.slf4j.Slf4j;
import org.wow.core.redis.JedisUtil;

/**
 * @program: api-gateway
 * @description: 使用Redis实现分布式限流
 * @author: wow
 * @create: 2023-10-03 16:17
 **/
@Slf4j
public class RedisCountLimiter {
    protected JedisUtil jedisUtil;

    public RedisCountLimiter(JedisUtil jedisUtil) {
        this.jedisUtil = jedisUtil;
    }

    private static final int SUCCESS_RESULT = 1;
    private static final int FAILED_RESULT = 0;

    /**
     * 执行限流
     * 使用lua脚本保证线程安全
     *
     * @param key
     * @param limit
     * @param expire
     * @return
     */
    public boolean doFlowCtl(String key, int limit, int expire) {
        try {
            Object object = jedisUtil.executeScript(key, limit, expire);
            Long result = Long.valueOf(object.toString());
            if (FAILED_RESULT == result) {
                return false;
            }
        } catch (Exception e) {
            throw new RuntimeException("分布式限流发生错误");
        }
        return true;

    }
}
