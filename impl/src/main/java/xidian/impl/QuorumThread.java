/*
 * Copyright © 2017 zhi and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package xidian.impl;

import java.io.IOException;
import java.util.Map;

import org.apache.http.HttpEntity;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.DefaultHttpRequestRetryHandler;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonParser;

public class QuorumThread implements Runnable{

	private String ip;
	private String inputJson;
	private Map<String, String> map;
	private JsonParser jsonParser;
	private Logger LOG;
	public QuorumThread(String ip, Map<String, String> map, String inputJson) {
		super();
		this.inputJson = inputJson;
		this.jsonParser = new JsonParser();
		this.ip = ip;
		this.map = map;
		this.LOG = LoggerFactory.getLogger(QuorumThread.class);
	}
	
	private RequestConfig getRequestConfig() {
		RequestConfig requestConfig = RequestConfig.custom().setSocketTimeout(50000).setConnectTimeout(50000)
				.setConnectionRequestTimeout(50000).build();
		return requestConfig;
	}
	/**
	 * 创建一个httpclient
	 * @return
	 */
	private CloseableHttpClient getHttpClient() {
		
		// 配置同时支持 HTTP 和 HTPPS
		Registry<ConnectionSocketFactory> socketFactoryRegistry = RegistryBuilder.<ConnectionSocketFactory>create()
				.register("http", PlainConnectionSocketFactory.getSocketFactory()).build();
		PoolingHttpClientConnectionManager pool;
		pool = new PoolingHttpClientConnectionManager(socketFactoryRegistry);
		// 将最大连接数增加到200，实际项目最好从配置文件中读取这个值
		pool.setMaxTotal(200);
		// 设置最大路由
		pool.setDefaultMaxPerRoute(2);
		RequestConfig requestConfig = getRequestConfig();
		
		CloseableHttpClient httpClient = HttpClients.custom()
				// 设置连接池管理
				.setConnectionManager(pool)
				// 设置请求配置
				.setDefaultRequestConfig(requestConfig)
				// 设置重试次数
				.setRetryHandler(new DefaultHttpRequestRetryHandler(0, false)).build();
		return httpClient;
	}

	private String sendHttpPost(HttpPost httpPost) {

		CloseableHttpClient httpClient = null;
		CloseableHttpResponse response = null;
		// 响应内容
		String responseContent = null;
		try {
			// 创建默认的httpClient实例.
			httpClient = getHttpClient();
			// 配置请求信息
			httpPost.setConfig(getRequestConfig());
			// 执行请求
			response = httpClient.execute(httpPost);
			// 得到响应实例
			HttpEntity entity = response.getEntity();
			responseContent = EntityUtils.toString(entity, "utf-8");
			EntityUtils.consume(entity);

		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			try {
				// 释放资源
				if (response != null) {
					response.close();
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return responseContent;
	}
	@Override
	public void run() {
		
		// 每一个线程都新建一个client来进行访问
		HttpPost httpPost = new HttpPost("http://" + ip + ":" + "8181" + "/restconf/operations/byzantine:receive-message");// 创建httpPost

		try {
			// 设置参数
			if (inputJson != null && inputJson.trim().length() > 0) {
				StringEntity stringEntity = new StringEntity(inputJson, "UTF-8");
				stringEntity.setContentType("application/yang.data+json");
				httpPost.setEntity(stringEntity);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		String result =  sendHttpPost(httpPost);
		
		
//		String result = HttpUtils.sendHttpPostJson(
//				HttpUtils.getBasicURL(this.ip, 8181, "/restconf/operations/byzantine:receive-message"), this.inputJson);
		String jugement = null;
		try {
			jugement = jsonParser.parse(result).getAsJsonObject().get("output").getAsJsonObject().get("result")
					.getAsString();
		} catch (Exception json) {
			jugement = "error";
			LOG.info("Get Message from:" + ip + " error");
		}
		map.put(this.ip, jugement);
	}

}
