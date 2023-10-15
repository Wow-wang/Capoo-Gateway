package org.wow.core.filter.gray;

import lombok.extern.slf4j.Slf4j;
import org.wow.core.context.GatewayContext;
import org.wow.core.filter.Filter;
import org.wow.core.filter.FilterAspect;

import static org.wow.common.constants.FilterConst.*;

/**
 * @program: api-gateway
 * @description:
 * @author: wow
 * @create: 2023-10-04 16:57
 **/

@FilterAspect(id = GRAY_FILTER_ID,name = GRAY_FILTER_NAME,
        order = GRAY_FILTER_ORDER)
@Slf4j
public class GrayFilter implements Filter {
    @Override
    public void doFilter(GatewayContext ctx) throws Exception {
        // 测试灰度功能时候使用
        String gray = ctx.getRequest().getHeaders().get("gray");
        if("true".equals(gray)){
            ctx.setGray(true);
        }
        String clientIp = ctx.getRequest().getClientIp();
        int res = clientIp.hashCode() & (1024 - 1);// 等于与对1024取模
        log.info("本次灰度中奖了嘛:{},{}", res,gray == null ? "无" : "中奖！" );
        if(res == 1){
            // 1024分之一的概率设置为1
            ctx.setGray(true);
        }
    }
}
