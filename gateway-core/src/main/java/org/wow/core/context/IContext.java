package org.wow.core.context;


import io.netty.channel.ChannelHandlerContext;
import org.wow.core.config.Rule;

import java.util.function.Consumer;

/**
 * 首先我们来思考一个问题，网关有生命周期吗？我们可以看我们的线程池吗？
 * 其实是有的，线程池一共有5种生命周期，有Running，ShutDown、STOP等，
 * 那么网关也需要生命周期，我们的
 * 网关上下文接口定义
 * 主要分3部分
 *
 * 1.上下文生命周期相关：
 * 		1.1 定义状态
 * 		1.2 状态流转方法
 * 		1.3 判断状态方法
 *
 * 	2.获取 转换协议，请求对象，响应对象，异常
 *
 * 	3.设置 响应对象，异常
 *
 * 	主要用到的就是这些，后面有其它的我们再继续扩展
 */
public interface IContext {

    /**
     * 一个请求正在执行中的状态
     */
    int RUNNING = 0;
    /**
     * 标志请求结束，写回Response
     */
    int WRITTEN = 1;
    /**
     * 写回成功后，设置该标识，如果是Netty ，ctx.WriteAndFlush(response)
     */
    int COMPLETED = 2;
    /**
     * 整个网关请求完毕，彻底结束
     */
    int TERMINATED = -1;

    /**
     * 设置上下文状态为正常运行状态
     */
    void running();

    /**
     * 设置上下文状态为标记写回
     */
    void written();
    /**
     * 设置上下文状态为标记写回成功
     */
    void completed();
    /**
     * 设置上下文状态为标记写回成功
     */
    void terminated();

    /**
     * 判断网关状态运行状态
     * @return
     */
    boolean isRunning();
    boolean isWritten();
    boolean isCompleted();
    boolean isTerminated();

    /**
     * 获取请求转换协议
     * @return
     */
    String getProtocol();
    /**
     * 获取请求转换协议
     * @return
     */
    Rule getRule();
    /**
     * 获取请求对象
     * @return
     */
    Object getRequest();
    /**
     * 获取请求结果
     * @return
     */
    Object getResponse();

    /**
     * 获取异常信息
     * @return
     */
    Throwable getThrowable();
    /**
     * 获取上下文参数
     * @return
     */
    <T> T getAttribute(String key);

    /**
     * 设置请求规则
     * @return
     */
    void setRule();
    /**
     * 设置请求返回结果
     * @return
     */
    void setResponse();
    /**
     * 设置请求异常信息
     * @return
     */
    void setThrowable(Throwable throwable);
    /**
     * 设置上下文参数
     * @return
     */
    void setAttribute(String key,Object obj);

    /**
     * 获取Netty上下文
     *
     * @return
     */
    ChannelHandlerContext getNettyCtx();

    /**
     * 是否保持连接
     * @return
     */
    boolean isKeepAlive();
    /**
     * 释放资源
     */
    void releaseRequest();

    /**
     * 设置回调函数
     * @param consumer
     */
    void setCompletedCallBack(Consumer<IContext> consumer);

    /**
     * 设置回调函数
     * @param consumer
     */
    void invokeCompletedCallBack(Consumer<IContext> consumer);

}
