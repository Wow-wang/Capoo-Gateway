package org.wow.common.config;

import lombok.Data;
import lombok.Getter;

import java.io.Serializable;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * @program: api-gateway
 * @description: 规则对象
 * @author: wow
 * @create: 2023-09-25 22:06
 **/
@Data
public class Rule implements Comparable<Rule>, Serializable {

    /**
     * 全局唯一规则ID
     */

    private String id;

    /**
     * 规则名称
     */
    private String name;

    /**
     * 规则对应的协议
     */
    private String protocol;

    /**
     * 后端服务ID
     */
    private String serviceId;

    /**
     * 请求前缀
     */
    private String prefix;

    /**
     * 路径集合
     */
    public List<String> paths;

    /**
     * 规则优先级 对应场景 一条路径对应多条规则 然后只执行一条规则的情况
     */
    @Getter
    private Integer order;

    /**
     * 规则优先级
     */
    private Set<FilterConfig> filterConfigs = new HashSet<>();

    private RetryConfig retryConfig = new RetryConfig();

    public RetryConfig getRetryConfig() {
        return retryConfig;
    }

    public void setRetryConfig(RetryConfig retryConfig) {
        this.retryConfig = retryConfig;
    }

    public Rule(){

    }

    public Rule(String id, String name, String protocol, String serviceId, String prefix, List<String> paths, Integer order, Set<FilterConfig> filterConfigs) {
        this.id = id;
        this.name = name;
        this.protocol = protocol;
        this.serviceId = serviceId;
        this.prefix = prefix;
        this.paths = paths;
        this.order = order;
        this.filterConfigs = filterConfigs;
    }

    public Rule(String id, String name, String protocol, Integer order, Set<FilterConfig> filterConfigs) {
        super();
        this.id = id;
        this.name = name;
        this.protocol = protocol;
        this.order = order;
        this.filterConfigs = filterConfigs;
    }

    public static class FilterConfig{
        /**
         * 规则配置ID
         */
        private String id;
        /**
         * 配置信息
         */
        private String config;

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getConfig() {
            return config;
        }

        public void setConfig(String config) {
            this.config = config;
        }

        @Override
        public boolean equals(Object o){
            if(this == o){
                return true;
            }
            if(o==null || getClass()!=o.getClass()){
                return false;
            }
            FilterConfig that = (FilterConfig) o;
            return id.equals(that.id);
        }

        @Override
        public int hashCode(){
            return  Objects.hash(id);
        }
    }

    private static class RetryConfig{
        //重试次数
        private int times;

        public int getTimes() {
            return times;
        }

        public void setTimes(int times) {
            this.times = times;
        }

    }

    /**
     * 向规则里面提供一些新增配置的方法
     * @param filterConfig
     * @return
     */
    public boolean addFilterConfig(FilterConfig filterConfig){
        return filterConfigs.add(filterConfig);
    }

    /**
     * 通过指定id获取指定的配置信息
     * @param id
     * @return
     */
    public FilterConfig getFilterConfig(String id){
        for(FilterConfig filterConfig : filterConfigs){
            if(filterConfig.getId().equalsIgnoreCase(id)){
                return filterConfig;
            }
        }
        return null;
    }

    /**
     * 通过传入的FilterID判断配置信息是否存在
     * @param id
     * @return
     */
    public boolean hashId(String id){
        for(FilterConfig filterConfig : filterConfigs){
            if(filterConfig.getId().equalsIgnoreCase(id)){
                return true;
            }
        }
        return false;
    }




    @Override
    public int compareTo(Rule o) {
        int orderCompare = Integer.compare(getOrder(),o.getOrder());
        if(orderCompare == 0){
            return getId().compareTo(o.getId());
        }
        return orderCompare;
    }

    @Override
    public boolean equals(Object o){
        if(this == o){
            return true;
        }
        if(o==null || getClass()!=o.getClass()){
            return false;
        }
        Rule that = (Rule) o;
        return id.equals(that.id);
    }

    @Override
    public int hashCode(){
        return Objects.hash(id);
    }

}
