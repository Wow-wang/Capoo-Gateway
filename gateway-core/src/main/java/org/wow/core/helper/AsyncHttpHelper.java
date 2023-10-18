package org.wow.core.helper;

import org.asynchttpclient.*;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * 异步的http辅助类
 */
public class AsyncHttpHelper {

	/**
	 * 静态内部类
	 */
	private static final class SingletonHolder {
		private static final AsyncHttpHelper INSTANCE = new AsyncHttpHelper();
	}
	
	private AsyncHttpHelper() {
		
	}
	
	public static AsyncHttpHelper getInstance() {
		return SingletonHolder.INSTANCE;
	}
	
	private AsyncHttpClient asyncHttpClient;
	
	public void initialized(AsyncHttpClient asyncHttpClient) {
		this.asyncHttpClient = asyncHttpClient;
	}

	/**
	 * ListenableFuture 允许您附加监听器（Listener），以便在操作完成时执行回调操作
	 * 收到结果
	 * @param request
	 * @return
	 */
	public CompletableFuture<Response> executeRequest(Request request) {
		/**
		 * Execute an HTTP request
		 *
		 * ListenableFuture 给异步任务添加监听
		 */
		ListenableFuture<Response> future = asyncHttpClient.executeRequest(request);
		return future.toCompletableFuture();
		// 设置超时时间 return future.toCompletableFuture().orTimeout(5,TimeUnit.SECONDS);
	}


	
	public <T> CompletableFuture<T> executeRequest(Request request, AsyncHandler<T> handler) {
		ListenableFuture<T> future = asyncHttpClient.executeRequest(request, handler);
		return future.toCompletableFuture();
	}
	
}
