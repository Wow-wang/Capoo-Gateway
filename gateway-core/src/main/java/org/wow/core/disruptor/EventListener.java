package org.wow.core.disruptor;

/**
 * 监听接口
 */
public interface EventListener<E> {

    void onEvent(E event);

    /**
     *
     * @param ex 异常
     * @param sequence 执行顺序
     * @param event 事件流
     */
    void onException(Throwable ex, long sequence, E event);
}
