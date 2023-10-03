package org.wow.core.netty.processor;

import org.wow.core.netty.HttpRequestWrapper;

public interface NettyProcessor {
    void process(HttpRequestWrapper wrapper);
}
