package org.wow.core.disruptor;

import afu.org.checkerframework.checker.oigj.qual.O;
import com.google.common.base.Preconditions;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.lmax.disruptor.*;
import com.lmax.disruptor.dsl.ProducerType;
import org.apache.commons.lang.ObjectUtils;
import org.wow.core.disruptor.EventListener;
import org.wow.core.disruptor.ParallelQueue;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;



/**
 * @program: api-gateway
 * @description: 基于Disruptor实现的多生产者多消费者无锁队列处理类
 * @author: wow
 * @create: 2023-10-07 10:51
 **/

public class ParallelQueueHandler<E> implements ParallelQueue<E> {
    private RingBuffer<Holder> ringBuffer;

    // 用户处理从环形缓冲区中读取的事件数据
    private EventListener<E> eventListener;

    // 工作线程池 从环形缓冲区读取事件数据
    private WorkerPool<Holder> workerPool;

    // 线程池执行任务
    private ExecutorService executorService;

    //事件翻译器 转化为E
    private EventTranslatorOneArg<Holder,E> eventTranslator;

    /**
     * 使用建造者模式初始化超多参数的构造器
     * @param builder
     */
    public ParallelQueueHandler(Builder<E> builder) {
        this.executorService = Executors.newFixedThreadPool(builder.threads,
                new ThreadFactoryBuilder().setNameFormat("ParallelQueueHandler"+builder.namePrefix+"-pool-%d").build());
        this.eventListener = builder.listener;
        this.eventTranslator = new HolderEventTranslator();

        //创建RingBuffer
        RingBuffer<Holder> ringBuffer = RingBuffer.create(builder.producerType,
                new HolderEventFactory(), builder.bufferSize, builder.waitStrategy);

        //创建RingBuffer 创建屏障
        SequenceBarrier sequenceBarrier = ringBuffer.newBarrier();

        //创建多个消费者组
        WorkHandler<Holder>[] workerHandlers = new WorkHandler[builder.threads];
        for(int i = 0; i < workerHandlers.length; i++){
            workerHandlers[i] = new HolderWorkHandler();
        }

        //创建多消费者线程池
        WorkerPool<Holder> workerPool = new WorkerPool<>(ringBuffer,
                sequenceBarrier,
                new HolderExceptionHandler(),
                workerHandlers);

        //设置多消费者的Sequence序号 主要用于统计消费进度
        ringBuffer.addGatingSequences(workerPool.getWorkerSequences());
        this.workerPool = workerPool;


    }

    @Override
    public void add(E event) {
        final RingBuffer<Holder> holderRing = ringBuffer;
        if(holderRing == null){
            process(this.eventListener,new IllegalStateException("ParallelQueueHandler is close"),event);
        }
        try{
            ringBuffer.publishEvent(this.eventTranslator,event);
        }catch (NullPointerException e){
            process(this.eventListener,new IllegalStateException("ParallelQueueHandler is close"),event);
        }
    }

    @Override
    public void add(E... events) {
        final RingBuffer<Holder> holderRing = ringBuffer;
        if(holderRing == null){
            process(this.eventListener,new IllegalStateException("ParallelQueueHandler is close"),events);
        }
        try{
            ringBuffer.publishEvents(this.eventTranslator,events);
        }catch (NullPointerException e){
            process(this.eventListener,new IllegalStateException("ParallelQueueHandler is close"),events);
        }
    }

    @Override
    public boolean tryAdd(E event) {
        final RingBuffer<Holder> holderRing = ringBuffer;
        if(holderRing == null){
            return false;
        }
        try{
            return ringBuffer.tryPublishEvent(this.eventTranslator,event);
        }catch(NullPointerException e){
            return false;
        }
    }

    @Override
    public boolean tryAdd(E... events) {
        final RingBuffer<Holder> holderRing = ringBuffer;
        if(holderRing == null){
            return false;
        }
        try{
            return ringBuffer.tryPublishEvents(this.eventTranslator,events);
        }catch(NullPointerException e){
            return false;
        }
    }

    @Override
    public void start() {
        this.ringBuffer = workerPool.start(executorService);
    }

    @Override
    public void shutDown() {
        RingBuffer<Holder> holder = ringBuffer;
        ringBuffer = null;
        if(holder == null){
            return;
        }
        if(workerPool != null){
            workerPool.drainAndHalt();
        }
        if(executorService != null){
            executorService.shutdown();
        }

    }

    @Override
    public boolean isShutDown() {
        return ringBuffer == null;
    }

    private static <E> void process(EventListener<E> listener, Throwable e, E event){
        listener.onException(e,-1,event);
    }
    private static <E> void process(EventListener<E> listener,Throwable e, E... events){
        for(E event : events){
            process(listener,e,event);
        }
    }




    public static class Builder<E>{
        private ProducerType producerType = ProducerType.MULTI;
        private int bufferSize = 1024 * 16;
        private int threads = 1;
        private String namePrefix = "";

        /**
         * WaitStrategy 是用来定义生产者与消费者之间等待的策略
         * BlockingWaitStrategy：阻塞等待策略
         */
        private WaitStrategy waitStrategy = new BlockingWaitStrategy();
        private EventListener<E> listener;

        public Builder<E> setProducerType(ProducerType producerType) {
            /**
             * Preconditions.checkNotNull 方法的作用是在方法执行之前检查传入的参数是否为 null。
             * 如果参数为 null，则该方法会抛出 NullPointerException 异常，以便在出现错误的情况下提前检测并处理。
             */
            Preconditions.checkNotNull(producerType);
            this.producerType = producerType;
            return this;
        }

        public Builder<E> setBufferSize(int bufferSize) {
            // bufferSize 必须是 2 的幂次方
            Preconditions.checkArgument(Integer.bitCount(bufferSize) == 1);
            this.bufferSize = bufferSize;
            return this;
        }

        public Builder<E> setThreads(int threads) {
            Preconditions.checkArgument(threads>0);
            this.threads = threads;
            return this;
        }

        public Builder<E> setNamePrefix(String namePrefix) {
            Preconditions.checkNotNull(namePrefix);
            this.namePrefix = namePrefix;
            return this;
        }

        public Builder<E> setWaitStrategy(WaitStrategy waitStrategy) {
            Preconditions.checkNotNull(waitStrategy);
            this.waitStrategy = waitStrategy;
            return this;
        }

        public Builder<E> setListener(EventListener<E> listener) {
            Preconditions.checkNotNull(listener);
            this.listener = listener;
            return this;
        }

        public ParallelQueueHandler<E> build(){
            return new ParallelQueueHandler<>(this);
        }
    }

    public class Holder{
        private E event;
        public void setEvent(E event){
            this.event = event;
        }

        @Override
        public String toString() {
            return "Holder{" +
                    "event=" + event +
                    '}';
        }


    }

    private class HolderExceptionHandler implements  ExceptionHandler<Holder>{

        @Override
        public void handleEventException(Throwable throwable, long l, Holder event) {
            Holder holder = (Holder) event;
            try{
                eventListener.onException(throwable,l,holder.event);
            }catch(Exception e){

            }finally {
                holder.setEvent(null);
            }


        }

        @Override
        public void handleOnStartException(Throwable throwable) {
            throw new UnsupportedOperationException(throwable);
        }

        @Override
        public void handleOnShutdownException(Throwable throwable) {
            throw new UnsupportedOperationException(throwable);
        }
    }



    private class HolderWorkHandler implements WorkHandler<Holder>{
        @Override
        public void onEvent(Holder holder) throws Exception{
            eventListener.onEvent(holder.event);
            holder.setEvent(null);
        }
    }



    private class HolderEventFactory implements EventFactory<Holder>{
        @Override
        public Holder newInstance(){
            return new Holder();
        }
    }

    private class HolderEventTranslator implements EventTranslatorOneArg<Holder,E>{

        @Override
        public void translateTo(Holder holder, long l, E e) {
            holder.setEvent(e);
        }
    }
}
