package cn.edu.neu.springbootdemo.core.common;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.web.servlet.HandlerInterceptor;

public class CrossOriginInterceptor implements HandlerInterceptor {
	@Override
	public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
			throws Exception {
		// 回显请求来源，避免写死某个前端端口；带凭证时不能用通配符 *
		String origin = request.getHeader("Origin");
		if (origin != null && !origin.isEmpty()) {
			response.setHeader("Access-Control-Allow-Origin", origin);
			response.setHeader("Vary", "Origin");
		}
		response.setHeader("Access-Control-Allow-Methods", "GET, POST, OPTIONS, PUT, PATCH, DELETE");
		response.setHeader("Access-Control-Allow-Credentials", "true");
		// 允许浏览器发送的请求消息头
		response.setHeader("Access-Control-Allow-Headers", request.getHeader("Access-Control-Request-Headers"));
		// 设置响应数据格式
		response.setHeader("Content-Type", "application/json");
		// 如果是OPTIONS请求则结束
		if (HttpMethod.OPTIONS.toString().equals(request.getMethod())) {
			response.setStatus(HttpStatus.OK.value());
			//System.out.println("跨域配置requestURI:{}"+request.getRequestURI());
			//System.out.println("跨域配置method:{}"+request.getMethod());
		}
		return true;
	}
}
