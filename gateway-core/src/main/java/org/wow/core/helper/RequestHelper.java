package org.wow.core.helper;

import com.alibaba.nacos.common.utils.CollectionUtils;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.*;
import io.netty.util.ReferenceCountUtil;
import org.apache.commons.lang3.StringUtils;
import org.wow.common.config.*;
import org.wow.common.constants.BasicConst;
import org.wow.common.constants.GatewayConst;
import org.wow.common.constants.GatewayProtocol;
import org.wow.common.enums.ResponseCode;
import org.wow.common.exception.ResponseException;
import org.wow.core.context.GatewayContext;
import org.wow.core.request.GatewayRequest;

import java.net.InetSocketAddress;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Date;
import java.util.List;


public class RequestHelper {

	public static GatewayContext doContext(FullHttpRequest request, ChannelHandlerContext ctx) {
		
		//	构建请求对象GatewayRequest
		GatewayRequest gateWayRequest = doRequest(request, ctx);
		
		//	根据请求对象里的uniqueId，获取资源服务信息(也就是服务定义信息)
		ServiceDefinition serviceDefinition = DynamicConfigManager.getInstance().getServiceDefinition(gateWayRequest.getUniqueId());
		
		//	根据请求对象获取服务定义对应的方法调用，然后获取对应的规则
		ServiceInvoker serviceInvoker = new HttpServiceInvoker();
		serviceInvoker.setInvokerPath(gateWayRequest.getPath());
		serviceInvoker.setTimeout(500);

		// 如果发现服务定义未启用 直接返回
		if(!serviceDefinition.isEnable()){
			FullHttpResponse httpResponse = ResponseHelper.getHttpResponse(ResponseCode.INTERNAL_ERROR);
			ctx.writeAndFlush(httpResponse)
					.addListener(ChannelFutureListener.CLOSE); // 释放资源后关闭
			ReferenceCountUtil.release(request);
		}

		// 根据请求对象获取规则
		Rule rule = getRule(gateWayRequest,serviceDefinition.getServiceId());

		
		//	构建我们而定GateWayContext对象
		GatewayContext gatewayContext = new GatewayContext(
				serviceDefinition.getProtocol(),
				ctx,
				HttpUtil.isKeepAlive(request),
				gateWayRequest,
				rule,0);


		//后续服务发现做完，这里都要改成动态的 -- 已经在负载均衡中实现
		//gatewayContext.getRequest().setModifyHost("127.0.0.1:8080");

		return gatewayContext;
	}


	/**
	 *构建Request请求对象
	 */
	private static GatewayRequest doRequest(FullHttpRequest fullHttpRequest, ChannelHandlerContext ctx) {
		
		HttpHeaders headers = fullHttpRequest.headers();
		//	从header头获取必须要传入的关键属性 uniqueId
		String uniqueId = headers.get(GatewayConst.UNIQUE_ID);
		
		String host = headers.get(HttpHeaderNames.HOST);
		HttpMethod method = fullHttpRequest.method();
		String uri = fullHttpRequest.uri();
		String clientIp = getClientIp(ctx, fullHttpRequest);
		String contentType = HttpUtil.getMimeType(fullHttpRequest) == null ? null : HttpUtil.getMimeType(fullHttpRequest).toString();
		Charset charset = HttpUtil.getCharset(fullHttpRequest, StandardCharsets.UTF_8);

		GatewayRequest gatewayRequest = new GatewayRequest(uniqueId,
				charset,
				clientIp,
				host, 
				uri, 
				method,
				contentType,
				headers,
				fullHttpRequest);
		
		return gatewayRequest;
	}
	
	/**
	 * 获取客户端ip
	 */
	private static String getClientIp(ChannelHandlerContext ctx, FullHttpRequest request) {
		String xForwardedValue = request.headers().get(BasicConst.HTTP_FORWARD_SEPARATOR);
		
		String clientIp = null;
		if(StringUtils.isNotEmpty(xForwardedValue)) {
			List<String> values = Arrays.asList(xForwardedValue.split(", "));
			if(values.size() >= 1 && StringUtils.isNotBlank(values.get(0))) {
				clientIp = values.get(0);
			}
		}
		if(clientIp == null) {
			InetSocketAddress inetSocketAddress = (InetSocketAddress)ctx.channel().remoteAddress();
			clientIp = inetSocketAddress.getAddress().getHostAddress();
		}
		return clientIp;
	}

	/**
	 * 获取Rule对象
	 * @param gateWayRequest
	 * @return
	 */
	private static Rule getRule(GatewayRequest gateWayRequest,String serviceId) {
		String key = serviceId + "." + gateWayRequest.getPath();
		Rule rule = DynamicConfigManager.getInstance().getRuleByPath(key);
		if(rule != null){
			if(TimeJudge(rule)){
				return rule;
			}
			throw new ResponseException(ResponseCode.PATH_NO_MATCHED);
		}
		// Rule Prefix
		rule = DynamicConfigManager.getInstance().getRuleByServiceId(serviceId)
				.stream().filter(r->gateWayRequest.getPath().startsWith(r.getPrefix()))
				.findAny().orElseThrow(()->new ResponseException(ResponseCode.PATH_NO_MATCHED));

		if(TimeJudge(rule)){
			return rule;
		}else{
			throw new ResponseException(ResponseCode.PATH_NO_MATCHED);
		}

	}
	private static Boolean TimeJudge(Rule rule){
		Rule.TimeConfig timeConfig = rule.getTimeConfig();
		if(timeConfig != null) {
			String pattern = timeConfig.getPattern();
			if (pattern.equals("After")) {
				LocalDateTime afterTime = LocalDateTime.parse(timeConfig.getAfter(), DateTimeFormatter.ISO_DATE_TIME);
				LocalDateTime currentDateTime = LocalDateTime.now();
				if(currentDateTime.isAfter(afterTime)){
					return true;
				}
				throw new ResponseException(ResponseCode.TIME_UNAVAILABLE);
			} else if (pattern.equals("Before")) {
				LocalDateTime beforeTime = LocalDateTime.parse(timeConfig.getBefore(), DateTimeFormatter.ISO_DATE_TIME);
				LocalDateTime currentDateTime = LocalDateTime.now();
				if(currentDateTime.isBefore(beforeTime)){
					return true;
				}
				throw new ResponseException(ResponseCode.TIME_UNAVAILABLE);
			} else if (pattern.equals("Between")) {
				LocalTime startTime = LocalTime.parse(timeConfig.getAfter());
				LocalTime endTime = LocalTime.parse(timeConfig.getBefore());
				LocalTime currentTime = LocalTime.now();
				if (isWithinTimeRange(startTime, endTime, currentTime)){
					return true;
				}
				throw new ResponseException(ResponseCode.TIME_UNAVAILABLE);
			}
		}

		return true;
	}
	public static boolean isWithinTimeRange(LocalTime startTime, LocalTime endTime, LocalTime currentTime) {
		return !currentTime.isBefore(startTime) && !currentTime.isAfter(endTime);
	}
	public static void main(String[] args) throws ParseException {
		String dateTimeString = "2023-10-17T08:30:00";

		try {
			// 将字符串解析为 LocalDateTime
			LocalTime startTime = LocalTime.parse("08:00");
			System.out.println(startTime);


		} catch (Exception e) {
			System.err.println("无法解析日期时间字符串: " + e.getMessage());
		}

	}
}
