package org.wow.gateway.client.core;

/**
 * @program: api-gateway
 * @description:
 * @author: wow
 * @create: 2023-09-29 21:51
 **/

public enum ApiProtocol {
    HTTP("http","http协议"),
    DUBBO("dubbo","dubbo协议");
    private String code;
    private String desc;

    ApiProtocol(String code, String desc){
        this.code = code;
        this.desc = desc;
    }

    public String getCode(){
        return code;
    }


}
