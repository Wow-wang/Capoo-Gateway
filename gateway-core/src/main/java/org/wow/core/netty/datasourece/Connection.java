package org.wow.core.netty.datasourece;


public interface Connection {

    Object execute(String method, String[] parameterTypes, String[] parameterNames, Object[] args);

}