package org.wow.core.filter;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.wow.common.config.Rule;
import org.wow.common.config.ServiceInstance;
import org.wow.common.constants.FilterConst;
import org.wow.common.constants.GatewayConst;
import org.wow.core.context.GatewayContext;
import org.wow.core.filter.router.RouterFilter;

import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * @program: api-gateway
 * @description: 过滤器工厂实现类
 * @author: wow
 * @create: 2023-10-02 09:32
 **/
@Slf4j
public class GatewayFilterChainFactory implements FilterFactory {

    private static class SingletonInstance{
        private static final GatewayFilterChainFactory INSTANCE = new GatewayFilterChainFactory();
    }

    private Cache<String,GatewayFilterChain> chainCache = Caffeine.newBuilder().recordStats().expireAfterWrite(10, TimeUnit.SECONDS).build();

    public static GatewayFilterChainFactory getInstance(){
        return SingletonInstance.INSTANCE;
    }

    public Map<String, Filter> processorFilterIdMap = new LinkedHashMap<>();

    private GatewayFilterChainFactory(){
        // 初始化所有filter
        ServiceLoader<Filter> serviceLoader = ServiceLoader.load(Filter.class);
        serviceLoader.stream().forEach(filterProvider -> {
            /**
             * 当调用 filterProvider.get() 时，会实例化相应的 Filter 实现类，并执行其构造函数。
             * 这意味着只有在您迭代 ServiceLoader 中的元素并调用 get() 方法时，才会初始化实际的 Filter 实例。
             */
            Filter filter = filterProvider.get();
            FilterAspect annotation = filter.getClass().getAnnotation(FilterAspect.class);
            if(annotation != null){
                log.info("load filter success:{},{},{},{}",filter.getClass(),
                        annotation.id(),annotation.name(),annotation.order());
                //添加到过滤器集合中去
                String filterId = annotation.id();
                if(StringUtils.isEmpty(filterId)){
                    filterId = filterId.getClass().getName();
                }
                processorFilterIdMap.put(filterId,filter);
            }
        });
    }
    @Override
    public GatewayFilterChain buildFilterChain(GatewayContext ctx) throws Exception{
        return chainCache.get(ctx.getRule().getId(),k->doBuildFilterChain(ctx.getRule()));
    }

    public GatewayFilterChain doBuildFilterChain(Rule rule)  {
        GatewayFilterChain chain = new GatewayFilterChain();
        List<Filter> filters = new ArrayList<>();

        // 添加灰度测试
        filters.add(getFilterInfo(FilterConst.GRAY_FILTER_ID));
        // 添加监控
        filters.add(getFilterInfo(FilterConst.MONITOR_FILTER_ID));
        filters.add(getFilterInfo(FilterConst.MONITOR_END_FILTER_ID));
        // 添加mock
        filters.add(getFilterInfo(FilterConst.MOCK_FILTER_ID));

        if(rule != null){
            Set<Rule.FilterConfig>  filterConfigs = rule.getFilterConfigs();
            Iterator iterator = filterConfigs.iterator();
            Rule.FilterConfig filterConfig;
            while(iterator.hasNext()){
                filterConfig = (Rule.FilterConfig)iterator.next();
                if(filterConfig == null){
                    continue;
                }
                String filterId = filterConfig.getId();
                if(StringUtils.isNotEmpty(filterId) && getFilterInfo(filterId) != null){
                    Filter filter = getFilterInfo(filterId);
                    filters.add(filter);
                }
            }
        }
        // 添加路由过滤器
        filters.add(new RouterFilter());

        // 排序 添加到链表中
        filters.sort(Comparator.comparing(Filter::getOrder));
        chain.addFilterList(filters);
        return chain;
    }

    @Override
    public Filter getFilterInfo(String filterId) {

        return processorFilterIdMap.get(filterId);
    }
}
