package cn.edu.neu.springbootdemo.core.common;

import java.io.PrintWriter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

import cn.edu.neu.springbootdemo.core.Constants;
import cn.edu.neu.springbootdemo.core.util.QueryUtil;
import cn.edu.neu.springbootdemo.model.User;


/**
* 
* @ClassName: LoginInterceptor
* @Description: 登录拦截器，用于阻止未登录用户访问系统
* @author liubingyue 
* @date 2018年7月11日 
*
*/
public class LoginInterceptor implements HandlerInterceptor {

	@Override
	public void afterCompletion(HttpServletRequest arg0, HttpServletResponse arg1, Object arg2, Exception arg3)
			throws Exception {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void postHandle(HttpServletRequest arg0, HttpServletResponse arg1, Object arg2, ModelAndView arg3)
			throws Exception {
		// TODO Auto-generated method stub
		
	}

	@Override
	public boolean preHandle(HttpServletRequest arg0, HttpServletResponse arg1, Object arg2) throws Exception {
		// TODO Auto-generated method stub	
		// 如果是OPTIONS请求则结束
		if (HttpMethod.OPTIONS.toString().equals(arg0.getMethod())) {
			arg1.setStatus(HttpStatus.OK.value());
			System.out.println("跨域配置requestURI:{}"+arg0.getRequestURI());
			System.out.println("跨域配置method:{}"+arg0.getMethod());
			return true;
		}
		if (arg2 instanceof HandlerMethod && ((HandlerMethod) arg2).getBean() instanceof Anonymous  ) {
			return true;
		}
		else{
			User user=(User)arg0.getSession().getAttribute(
					Constants.LOGIN_USER);
			if (user != null) {		
				return true;
			}		
		} 
		System.out.println("----logininterceptor:"+arg0.getHeader("Referer")+","+QueryUtil.getRequestURL(arg0));
		arg0.getSession().setAttribute(Constants.ORIGINAL_URL,
					QueryUtil.getRequestURL(arg0));
		String redirUrl=QueryUtil.getRequestURL(arg0);
		//System.out.println("--------x-request-with:"+arg0.getHeader("x-requested-with"));		
		System.out.println("redirUrl:"+redirUrl);
		arg1.setContentType("text/json;charset=UTF-8");
		arg1.setStatus(401);
		PrintWriter out=arg1.getWriter();
		out.print("{\"login\":\""+Constants.LOGIN_PROMPT+"\",");
		out.print("\"redirUrl\":\""+redirUrl+"\"}");
		out.flush();
		out.close(); 
		
		return false;
	}

}
