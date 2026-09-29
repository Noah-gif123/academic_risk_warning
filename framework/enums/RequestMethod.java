package cn.edu.neusoft.framework.enums;

public enum RequestMethod {
	GET("get"), HEAD("head"), POST("post"),
	PUT("put"), PATCH("patch"), DELETE("delete"),
	OPTIONS("options");
	private final String method;

	private RequestMethod(String method) {
		this.method = method;
	}
	public String getMethod() {
		return this.method;
	}
}
