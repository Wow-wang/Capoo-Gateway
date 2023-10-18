package org.wow.core.disruptor;

/**
 * @program: api-gateway
 * @description: 多生产者多消费者处理接口
 * @author: wow
 * @create: 2023-10-07 10:49
 **/

public interface ParallelQueue <E> {
    void add(E event);
    void add(E... event);

    boolean tryAdd(E event);
    boolean tryAdd(E... event);

    void start();

    void shutDown();

    /**
     * 判断是否已经销毁
     * @return
     */
    boolean isShutDown();
}
