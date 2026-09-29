package cn.edu.neusoft.framework.model;

import java.util.HashMap;
import java.util.Map;

public class ModelAndView {
	private String view;
	private Map<String, Object> model;
	private boolean redirect;
	private String redirectUrl;

	// 添加无参构造函数
	public ModelAndView() {
		this.model = new HashMap<String, Object>();
		this.redirect = false;
	}

	public ModelAndView(String v) {
		this.view = v;
		this.model = new HashMap<String, Object>();
		this.redirect = false;
	}

	public ModelAndView(String redirectUrl, boolean isRedirect) {
		this.redirect = isRedirect;
		this.redirectUrl = redirectUrl;
		this.model = new HashMap<String, Object>();
	}

	public String getView() {
		return view;
	}

	public void setView(String view) {
		this.view = view;
	}

	public void addObject(String key, Object value) {
		this.model.put(key, value);
	}

	public Map<String, ?> getModel() {
		return model;
	}

	public boolean isRedirect() {
		return redirect;
	}

	public void setRedirect(boolean redirect) {
		this.redirect = redirect;
	}

	public String getRedirectUrl() {
		return redirectUrl;
	}

	public void setRedirectUrl(String redirectUrl) {
		this.redirect = true;
		this.redirectUrl = redirectUrl;
	}
}