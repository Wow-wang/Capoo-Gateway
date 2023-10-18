package org.wow.core.response;

import com.fasterxml.jackson.databind.node.ObjectNode;
import io.netty.handler.codec.http.*;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.asynchttpclient.Response;
import org.wow.common.enums.ResponseCode;
import org.wow.common.utils.JSONUtil;
import org.wow.core.request.GatewayRequest;

/**
 * @program: api-gateway
 * @description:
 * @author: wow
 * @create: 2023-09-25 21:52
 **/

@Data
public class GatewayResponse {
    /**
     * 响应头
     */
    private HttpHeaders responseHeaders = new DefaultHttpHeaders();

    /**
     * 额外的响应头信息
     */
    private HttpHeaders extraResponseHeaders = new DefaultHttpHeaders();

    /**
     * 响应内容
     */
    private String content;

    /**
     * 返回相应状态码
     */
    private HttpResponseStatus httpResponseStatus;

    /**
     * 异步返回对象
     */
    private Response futureResponse;

    public GatewayResponse(){

    }

    /**
     * 设置响应头信息
     */
    public void putHeader(CharSequence key,CharSequence val){
        responseHeaders.add(key,val);
    }

    /**
     * 构建异步响应对象
     * @param futureResponse
     * @return
     */
    public static GatewayResponse buildGatewayResponse(Response futureResponse){
        GatewayResponse response = new GatewayResponse();
        response.setFutureResponse(futureResponse);
        response.setHttpResponseStatus(HttpResponseStatus.valueOf(futureResponse.getStatusCode()));
        return response;
    }

    /**
     * 返回一个Json类型的响应信息 失败时使用
     */
    public static GatewayResponse buildGatewayResponse(ResponseCode code, Object... arg){
        ObjectNode objectNode = JSONUtil.createObjectNode();
        objectNode.put(JSONUtil.STATUS,code.getStatus().code());
        objectNode.put(JSONUtil.CODE,code.getCode());
        objectNode.put(JSONUtil.MESSAGE,code.getMessage());


        GatewayResponse response = new GatewayResponse();
        response.setHttpResponseStatus(code.getStatus());
        response.putHeader(HttpHeaderNames.CONTENT_TYPE,HttpHeaderValues.APPLICATION_JSON+";charset=utf-8");
        response.setContent(JSONUtil.toJSONString(objectNode));
        return response;
    }
    /**
     * 返回一个Json类型的响应信息 成功时使用
     */
    public static GatewayResponse buildGatewayResponse(Object data){
        ObjectNode objectNode = JSONUtil.createObjectNode();
        objectNode.put(JSONUtil.STATUS,ResponseCode.SUCCESS.getStatus().code());
        objectNode.put(JSONUtil.CODE,ResponseCode.SUCCESS.getCode());
        objectNode.putPOJO(JSONUtil.DATA,data);

        GatewayResponse response = new GatewayResponse();
        response.setHttpResponseStatus(ResponseCode.SUCCESS.getStatus());
        response.putHeader(HttpHeaderNames.CONTENT_TYPE,HttpHeaderValues.APPLICATION_JSON+";charset=utf-8");
        response.setContent(JSONUtil.toJSONString(objectNode));
        return response;
    }

}
