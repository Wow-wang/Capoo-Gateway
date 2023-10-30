package org.wow.core.netty.datasourece;

import java.util.Map;


public interface IGenericReference {

    SessionResult $invoke(Map<String, Object> params);

}