package cn.edu.neusoft.framework.model;

import java.lang.reflect.Method;

public class Handler {
	private Object target;
	private Method method;
	
	public Handler(Object target, Method method) {
		this.target = target;
		this.method = method;
	}
	public Object getTarget() {
		return target;
	}
	public Method getMethod() {
		return method;
	}
	@Override
	public String toString() {
		return "Handler [target=" + target.getClass().getName() + ", method=" + method.getName() + "]";
	}
	

}
