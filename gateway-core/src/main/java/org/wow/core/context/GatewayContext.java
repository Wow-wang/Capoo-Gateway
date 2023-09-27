package org.wow.core.context;

import io.netty.channel.ChannelHandlerContext;
import io.netty.util.ReferenceCountUtil;
import org.wow.core.config.Rule;
import org.wow.common.utils.AssertUtil;
import org.wow.core.request.GatewayRequest;
import org.wow.core.response.GatewayResponse;

/**
 * 网关上下文类
 */
public class GatewayContext extends BasicContext{
    public GatewayRequest request;

    public GatewayResponse response;

    public Rule rule;

    public GatewayContext(String protocal, ChannelHandlerContext nettyCtx, boolean keepAlive, GatewayRequest request, Rule rule) {
        super(protocal, nettyCtx, keepAlive);
        this.request = request;
        this.rule = rule;
    }


    public static class Builder{
        private String protocal;
        private ChannelHandlerContext nettyCtx;
        private GatewayRequest request;
        private Rule rule;
        private boolean keepAlive;

        public Builder(){

        }

        public Builder setProtocal(String protocal) {
            this.protocal = protocal;
            return this;
        }

        public Builder setNettyCtx(ChannelHandlerContext nettyCtx) {
            this.nettyCtx = nettyCtx;
            return this;
        }

        public Builder setRequest(GatewayRequest request) {
            this.request = request;
            return this;
        }

        public Builder setRule(Rule rule) {
            this.rule = rule;
            return this;
        }

        public Builder setKeepAlive(boolean keepAlive) {
            this.keepAlive = keepAlive;
            return this;
        }

        public GatewayContext build(){
            AssertUtil.notNull(protocal,"protocal 不能为空");
            AssertUtil.notNull(nettyCtx,"nettyCtx 不能为空");
            AssertUtil.notNull(rule,"rule 不能为空");
            AssertUtil.notNull(request,"request 不能为空");

            return new GatewayContext(protocal,nettyCtx,keepAlive,request,rule);
        }
    }

    // 获取上下文

    /**
     *获取必要的上下文参数
     */
    public <T> T getRequireAttribute(String key){
        T value = getAttribute(key);
        AssertUtil.notNull(value," 缺乏必要参数");
        return value;
    }

    /**
     *获取指定Key的上下文参数 如果没有返回默认值
     */
    public <T> T getRequireAttribute(String key,T defaultValue){
        return (T) attributes.getOrDefault(key,defaultValue);
    }

    /**
     *获取指定的过滤器信息
     */
    public Rule.FilterConfig getFilterConfig(String filterId){
        return rule.getFilterConfig(filterId);
    }

    /**
     *获取服务ID
     */
    public String getUniqueId(){
        return request.getUniqueId();
    }

    /**
     * 重写父类释放资源 真正释放
     */
    public void releaseRequest(){
        if(requestReleased.compareAndSet(false,true)){
            ReferenceCountUtil.release(request.getFullHttpRequest());
        }
    }

    /**
     * 获取原始请求对象的方法
     * @return
     */
    public GatewayRequest getOriginRequest(){
        return request;
    }

    @Override
    public GatewayRequest getRequest() {
        return request;
    }

    public void setRequest(GatewayRequest request) {
        this.request = request;
    }

    @Override
    public GatewayResponse getResponse() {
        return response;
    }

    public void setResponse(Object response) {
        this.response = (GatewayResponse) response;
    }

    @Override
    public Rule getRule() {
        return rule;
    }

    public void setRule(Rule rule) {
        this.rule = rule;
    }
}
