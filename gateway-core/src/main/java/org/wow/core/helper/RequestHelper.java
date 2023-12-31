package org.wow.core.helper;

import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.*;
import io.netty.util.ReferenceCountUtil;
import org.apache.commons.lang3.StringUtils;
import org.wow.common.config.*;
import org.wow.common.constants.BasicConst;
import org.wow.common.constants.GatewayConst;
import org.wow.common.enums.ResponseCode;
import org.wow.common.exception.ResponseException;
import org.wow.core.context.GatewayContext;
import org.wow.core.request.GatewayRequest;

import java.net.InetSocketAddress;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.net.URI;


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
		gateWayRequest.setRpcInterfaceName(serviceDefinition.getRpcInterfaceName());
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
//		// 获取请求URI
//		String uri = fullHttpRequest.uri();

		// 使用Java的URI类来解析URI
//		URI requestUri = new URI(uri);
//
//		// 获取查询参数
//		String query = requestUri.getQuery();
//
//		// 如果查询参数非空，您可以进一步解析它
//		if (query != null) {
//			// 将查询参数解析为键值对
//			Map<String, String> queryParams = parseQueryParameters(query);
//
//			// 访问特定参数的值
//			String paramValue = queryParams.get("paramName");
//		}

		// 从header头获取必须要传入的关键属性 uniqueId = serviceId + version
		String uniqueId = headers.get(GatewayConst.UNIQUE_ID);

		String rpcMethod = headers.get(GatewayConst.METHOD);


		// RPC 获取参数类型和值
		String test = headers.get(GatewayConst.PARAMETERTYPES);
		String[] parameterTypes = null;
		if (test != null) {
			List<String> idList = Arrays.asList(test.split(","));
			parameterTypes = idList.toArray(new String[0]);
		}

		test = headers.get(GatewayConst.ARGUMENTS);
		String[] arguments = null;
		if (test != null) {
			List<String> idList = Arrays.asList(test.split(","));
			arguments = idList.toArray(new String[0]);
		}


		String host = headers.get(HttpHeaderNames.HOST);




		HttpMethod method = fullHttpRequest.method();



		// 获取请求的URI
		String uri = fullHttpRequest.uri();

		// 获取客户端IP地址
		String clientIp = getClientIp(ctx, fullHttpRequest);





		// 获取请求的Content-Type
		String contentType = HttpUtil.getMimeType(fullHttpRequest) == null ? null : HttpUtil.getMimeType(fullHttpRequest).toString();

		// 获取字符编码
		Charset charset = HttpUtil.getCharset(fullHttpRequest, StandardCharsets.UTF_8);






		// 创建GatewayRequest对象，用于封装网关请求的相关信息
		GatewayRequest gatewayRequest = new GatewayRequest(uniqueId,
				charset,
				clientIp,
				host,
				uri,
				method,
				contentType,
				headers,
				fullHttpRequest,
				rpcMethod,
				parameterTypes,
				arguments);

		return gatewayRequest;
	}


	/**
	 * 获取客户端ip
	 */
	private static String getClientIp(ChannelHandlerContext ctx, FullHttpRequest request) {
		// X-Forwarded-For 是一个 HTTP 请求头部字段  一般不设置
		String xForwardedValue = request.headers().get(BasicConst.HTTP_FORWARD_SEPARATOR);
		
		String clientIp = null;
		if(StringUtils.isNotEmpty(xForwardedValue)) {
			List<String> values = Arrays.asList(xForwardedValue.split(", "));
			if(values.size() >= 1 && StringUtils.isNotBlank(values.get(0))) {
				clientIp = values.get(0);
			}
		}
		if(clientIp == null) {
			// 从底层的网络连接中获取客户端的 IP 地址
			InetSocketAddress inetSocketAddress = (InetSocketAddress)ctx.channel().remoteAddress();
			clientIp = inetSocketAddress.getAddress().getHostAddress();
		}
		return clientIp;
	}

	/**
	 * 获取Rule对象
	 * 匹配规则 服务 + 路径
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


		// 时间判断
		if(TimeJudge(rule)){
			return rule;
		}else{
			throw new ResponseException(ResponseCode.PATH_NO_MATCHED);
		}


	}
	private static Boolean TimeJudge(Rule rule){
		Rule.TimeConfig timeConfig = rule.getTimeConfig();
		if(timeConfig.getPattern() != null) {
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
//	public static Map<String, List<String>> parseQueryParameters(String query) {
//		Map<String, List<String>> queryParams = new HashMap<>();
//
//		if (query != null && !query.isEmpty()) {
//			String[] params = query.split("&");
//			for (String param : params) {
//				String[] keyValue = param.split("=");
//				if (keyValue.length == 2) {
//					String key = keyValue[0];
//					String value = keyValue[1];
//					queryParams.computeIfAbsent(key, k -> new ArrayList<>()).add(value);
//				}
//			}
//		}
//
//		return queryParams;
//	}
}

