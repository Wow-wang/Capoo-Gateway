package com.wow.gatewaydiverse.nacos;


import com.alibaba.cloud.nacos.NacosDiscoveryProperties;
import com.wow.gatewaydiverse.ApiProperties;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;

import java.net.SocketException;



@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class ApiNacosDiscoveryProperties extends NacosDiscoveryProperties {
    private final ApiProperties apiProperties;

    @Override
    public void init() throws SocketException {
        super.init();

        String ip = apiProperties.getIp();
        if (StringUtils.isNotBlank(ip)) {
            this.setIp(ip);
        }

        Integer port = apiProperties.getPort();
        this.setPort(port);
    }
}
