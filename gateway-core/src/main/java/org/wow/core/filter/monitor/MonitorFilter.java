package org.wow.core.filter.monitor;

import io.micrometer.core.instrument.Timer;
import lombok.extern.slf4j.Slf4j;
import org.wow.core.context.GatewayContext;
import org.wow.core.filter.Filter;
import org.wow.core.filter.FilterAspect;

import static org.wow.common.constants.FilterConst.*;

/**
 * @program: api-gateway
 * @description:
 * @author: wow
 * @create: 2023-10-04 21:46
 **/
@Slf4j
@FilterAspect(id = MONITOR_FILTER_ID,
        name = MONITOR_FILTER_NAME,
        order = MONITOR_FILTER_ORDER)
public class MonitorFilter implements Filter {
    @Override
    public void doFilter(GatewayContext ctx) throws Exception {
        // 开始计时
        ctx.setTimerSample(Timer.start());
    }
}
