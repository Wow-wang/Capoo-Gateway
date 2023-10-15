package org.wow.core.filter.mock;

import lombok.extern.slf4j.Slf4j;
import org.wow.common.config.Rule;
import org.wow.common.utils.JSONUtil;
import org.wow.core.context.GatewayContext;
import org.wow.core.filter.Filter;
import org.wow.core.filter.FilterAspect;
import org.wow.core.helper.ResponseHelper;
import org.wow.core.response.GatewayResponse;

import java.util.Map;

import static org.wow.common.constants.FilterConst.*;

/**
 * @program: api-gateway
 * @description:
 * @author: wow
 * @create: 2023-10-05 12:12
 **/

@Slf4j
@FilterAspect(id = MOCK_FILTER_ID,
        name = MOCK_FILTER_NAME,
        order = MOCK_FILTER_ORDER)
public class MockFilter implements Filter {
    @Override
    public void doFilter(GatewayContext ctx) throws Exception {
        Rule.FilterConfig config = ctx.getRule().getFilterConfig(MOCK_FILTER_ID);
        // 没有mock配置
        if (config == null) {
            return;
        }

        Map<String, String> map = JSONUtil.parse(config.getConfig(), Map.class);
        String value = map.get(ctx.getRequest().getMethod().name() + " " + ctx.getRequest().getPath());
        if (value != null) {
            // 设置返回值 直接写回
            ctx.setResponse(GatewayResponse.buildGatewayResponse(value));
            ctx.written();
            ResponseHelper.writeResponse(ctx);
            log.info("mock {} {} {}", ctx.getRequest().getMethod(), ctx.getRequest().getPath(), value);
            ctx.terminated();
        }
    }
}

