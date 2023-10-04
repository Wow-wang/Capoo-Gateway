package org.wow.core.filter.Auth;

import io.jsonwebtoken.Jwt;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.impl.DefaultClaims;
import io.netty.handler.codec.http.cookie.Cookie;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.wow.common.enums.ResponseCode;
import org.wow.common.exception.ResponseException;
import org.wow.core.context.GatewayContext;
import org.wow.core.filter.Filter;
import org.wow.core.filter.FilterAspect;

import static org.wow.common.constants.FilterConst.*;

/**
 * @program: api-gateway
 * @description:
 * @author: wow
 * @create: 2023-10-04 11:24
 **/
@Slf4j
@FilterAspect(id= USER_AUTH_FILTER_ID,
        name = USER_AUTH_FILTER_NAME,
        order =USER_AUTH_FILTER_ORDER )
public class UserAuthFilter implements Filter {

    private static final String SECRET_KEY ="faewifheafewhefsfjkds";
    private static final String COOKIE_NAME = "user-jwt";
    @Override
    public void doFilter(GatewayContext ctx) throws Exception {
//        // 检查用户是否需要用户鉴权
//        if(ctx.getRule().getFilterConfig(USER_AUTH_FILTER_ID) == null){
//            return;
//        }
        String token = ctx.getRequest().getCookie(COOKIE_NAME).value();
        if(StringUtils.isBlank(token)){
            throw new ResponseException(ResponseCode.UNAUTHORIZED);
        }

        try {
            // 解析用户id
            long userId = parseUserId(token);
            // 把用户id传给下游
            ctx.getRequest().setUserId(userId);
        } catch (Exception e) {
            throw new ResponseException(ResponseCode.UNAUTHORIZED);
        }
    }

    private long parseUserId(String token) {
        Jwt jwt = Jwts.parser().setSigningKey(SECRET_KEY).parse(token);

        // 在 DefaultClaims 对象中，使用 getSubject() 方法来获取主题声明的值
        return Long.parseLong(((DefaultClaims)jwt.getBody()).getSubject());
    }
}
