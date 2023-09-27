package org.wow.core.request;



import org.asynchttpclient.Request;
import org.asynchttpclient.cookie.Cookie;


/**
 * @program: api-gateway
 * @description: 提供可修改的Request参数操作接口
 * @author: wow
 * @create: 2023-09-25 15:51
 **/

public interface IGatewayRequest {

    /**
     * 第一个我能想到的是目标服务的地址，我们需要修改，为什么呢
     * 因为我们一开始拿到的是一个后端服务的域名，这时候我们就需要通过注册中心，
     * 在通过自己的负载均衡算法，拿到真正服务的IP地址，然后替换掉其中的host，我们就先定义一些这个接口
     * @param host
     */
    void setModifyHost(String host);

    /**
     * 获取目标服务主机
     * 有修改就会有获取，我们写一个获取方法
     */
    String getModifyHost();

    /**
     * 有了目标地址，我们还需要组装最后的请求，我们同样需要这样一个方法
     * @param path
     */
    void setModifyPath(String path);

    /**
     * 获取目标服务路径
     *
     */
    String getModifyPath();

    /**
     * 添加请求头信息
     * @param name
     * @param value
     */
    void addHeader(CharSequence name, String value);

    /**
     * 设置请求头信息
     * @param name
     * @param value
     */
    void setHeader(CharSequence name, String value);

    /**
     * 添加Get请求参数
     * @param name
     * @param value
     */
    void addQueryParam(String name, String value);

    /**
     * 添加表单请求参数
     * @param name
     * @param value
     */
    void addFormParam(String name,String value);

    /**
     * 添加或者替换Cookie
     *
     */
    void addOrReplaceCookie(Cookie cookie);



    /**
     * 设置超时时间
     * @param RequestTimeout
     */
    void setRequestTimeout(int RequestTimeout);

    /**
     * 获取最终的请求路径 包含请求参数 Http://localhost:8081/api/admin?name=111
     * @param
     */
    String getFinalUrl();
    /**
     * 构造最终的请求对象
     * @return
     */
    Request build();
}
