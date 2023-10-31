package org.wow.core.request;

import com.google.common.collect.Lists;
import com.jayway.jsonpath.JsonPath;
import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.http.*;
import io.netty.handler.codec.http.cookie.Cookie;
import io.netty.handler.codec.http.cookie.ServerCookieDecoder;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;
import org.asynchttpclient.Request;
import org.asynchttpclient.RequestBuilder;
import org.wow.common.constants.BasicConst;

import java.nio.charset.Charset;
import java.util.*;

import static java.lang.System.currentTimeMillis;

/**
 * @program: api-gateway
 * @description:
 * @author: wow
 * @create: 2023-09-25 16:17
 **/

public class GatewayRequest implements IGatewayRequest{
/**
 *  首先我们需要来看一下，请求中不会变化的变量
 * 	首先我们需要给我们的服务顶一个唯一的服务ID，一般是ServiceID：Version
 *	对于一个FullHttpRequest: 在header里面必须要有该属性：uniqueId
 */



    /**
     * 服务唯一ID 包含版本信息
     */
    @Getter
    private final String uniqueId;

    @Getter
    private final String rpcMethod;

    /**
     * 进入网关开始时间
     */
    @Getter
    private final long beginTime;


    /**
     * 字符集
     */
    @Getter
    private final Charset charset;

    /**
     * 客户端的IP 主要用于做流控，黑白名单
     */
    @Getter
    private final String clientIp;

    /**
     * 服务端的主机名
     */
    @Getter
    private final String host;

    /**
     * 服务端对额请求路径 /XXX/XX/XX
     */
    @Getter
    private final String path;
    /**
     * 统一资源标识符 /XXX/XX/XX?attr1=1&attr2=2
     */
    @Getter
    private final String uri;
    /**
     * 请求方式 Post/Get/Put
     */
    @Getter
    private final HttpMethod method;

    /**
     * 请求格式
     */
    @Getter
    private final String contentType;

    /**
     * 请求头
     */
    @Getter
    private final HttpHeaders headers;

    /**
     * 参数解析器
     */
    @Getter
    private final QueryStringDecoder queryStringDecoder;

    /**
     * fullHttpRequest
     */
    @Getter
    private final FullHttpRequest fullHttpRequest;

    /**
     * 请求体
     */
    @Getter
    private String body;

    /**
     * 请求对象里面的cookie
     */
    @Getter
    private Map<String, Cookie> cookieMap;


    /**
     * Post请求参数结合
     */
    @Getter
    private Map<String, List<String>> postParameters;

    @Getter
    private String[] parameterTypes;

    @Getter
    private String[] arguments;


    /***************** IGatewayRequest:可修改的请求变量 	**********************/

    /**
     * 可修改的Scheme，默认为Http://
     */
    private String modifyScheme;

    /**
     * 可修改的主机名
     */
    private String modifyHost;

    /**
     * 可修改的请求路径
     */
    private String modifyPath;
    @Setter
    @Getter
    private long userId;


    @Setter
    @Getter
    private String rpcInterfaceName;

    /**
     * 构建下游请求时的http构建器
     */
    private final RequestBuilder requestBuilder;

    /**
     * 构造器
     * @param uniqueId
     * @param charset
     * @param clientIp
     * @param host
     * @param uri
     * @param method
     * @param contentType
     * @param httpHeaders
     * @param fullHttpRequest
     */
    public GatewayRequest(String uniqueId,
                          Charset charset,
                          String clientIp, String host,
                          String uri,
                          HttpMethod method, String contentType,
                          HttpHeaders httpHeaders,
                          FullHttpRequest fullHttpRequest, String rpcMethod,String[] parameterTypes,String[] arguments) {
        this.uniqueId = uniqueId;
        this.rpcMethod = rpcMethod;
        this.beginTime = currentTimeMillis();
        this.charset = charset;
        this.clientIp = clientIp;
        this.host = host;
        this.uri = uri;
        this.method = method;
        this.contentType = contentType;
        this.headers = httpHeaders;
        this.queryStringDecoder = new QueryStringDecoder(uri,charset);
        this.fullHttpRequest = fullHttpRequest;
        this.requestBuilder = new RequestBuilder();
        this.path = queryStringDecoder.path();
        this.parameterTypes = parameterTypes;
        this.arguments = arguments;


        this.modifyHost = host;
        this.modifyPath = path;
        this.modifyScheme = BasicConst.HTTP_PREFIX_SEPARATOR;
        this.requestBuilder.setMethod(getMethod().name());
        this.requestBuilder.setHeaders(getHeaders());
        this.requestBuilder.setQueryParams(queryStringDecoder.parameters());

        ByteBuf contentBuffer = fullHttpRequest.content();
        if(Objects.nonNull(contentBuffer)){
            this.requestBuilder.setBody(contentBuffer.nioBuffer());
        }
    }

    /**
     * 获取请求体
     * @return body
     */
    public String getBody(){
        if(StringUtils.isEmpty(body)){
            body = fullHttpRequest.content().toString(charset);
        }
        return body;
    }

    public Cookie getCookie(String name){
        if(cookieMap == null){
            cookieMap = new HashMap<String, Cookie>();
            String cookieStr = getHeaders().get(HttpHeaderNames.COOKIE);
            Set<Cookie> cookies = ServerCookieDecoder.STRICT.decode(cookieStr);
            for(Cookie cookie : cookies){
                cookieMap.put(name,cookie);
            }
        }
        return cookieMap.get(name);
    }

    public List<String> getQueryParametersMultiple(String name){
        String body = getBody();
        if(isFormPost()){
            if(postParameters == null){
                QueryStringDecoder paramDecoder = new QueryStringDecoder(body,false);
                postParameters = paramDecoder.parameters();
            }
            if(postParameters == null || postParameters.isEmpty()){
                return null;
            }else{
                return postParameters.get(name);
            }
        }else if(isJsonPost()){
            return Lists.newArrayList(JsonPath.read(body,name).toString());
        }
        return null;
    }

    public boolean isFormPost(){
        return HttpMethod.POST.equals(method)&&(
                contentType.startsWith(HttpHeaderValues.FORM_DATA.toString())||
                contentType.startsWith(HttpHeaderValues.APPLICATION_X_WWW_FORM_URLENCODED.toString())
         );
    }
    public boolean isJsonPost(){
        return HttpMethod.POST.equals(method)&&(
                contentType.startsWith(HttpHeaderValues.APPLICATION_JSON.toString()));
    }




    @Override
    public void setModifyHost(String host) {
        this.modifyHost = host;
    }

    @Override
    public String getModifyHost() {

        return modifyHost;
    }

    @Override
    public void setModifyPath(String path) {
        this.modifyPath = path;
    }

    @Override
    public String getModifyPath() {
        return path;
    }

    @Override
    public void addHeader(CharSequence name, String value) {
        requestBuilder.addHeader(name,value);
    }

    @Override
    public void setHeader(CharSequence name, String value) {
        requestBuilder.setHeader(name,value);
    }

    @Override
    public void addQueryParam(String name, String value) {
        requestBuilder.addQueryParam(name,value);
    }

    @Override
    public void addFormParam(String name, String value) {
        requestBuilder.addFormParam(name, value);
    }

    @Override
    public void addOrReplaceCookie(org.asynchttpclient.cookie.Cookie cookie) {
        requestBuilder.addOrReplaceCookie(cookie);
    }


    @Override
    public void setRequestTimeout(int RequestTimeout) {
        requestBuilder.setRequestTimeout(RequestTimeout);
    }

    @Override
    public String getFinalUrl() {
        return modifyScheme+modifyHost+modifyPath;
    }


    @Override
    public Request build() {
        requestBuilder.setUrl(getFinalUrl());
        // 传给下游服务
        requestBuilder.addHeader("userId",String.valueOf(userId));
        return requestBuilder.build();
    }
}
