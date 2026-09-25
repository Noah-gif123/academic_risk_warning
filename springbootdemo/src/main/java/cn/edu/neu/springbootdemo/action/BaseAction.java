package cn.edu.neu.springbootdemo.action;

import cn.edu.neu.springbootdemo.core.Constants;
import cn.edu.neu.springbootdemo.core.common.CommonBaseAction;
import cn.edu.neu.springbootdemo.model.User;


public class BaseAction extends CommonBaseAction{
	
	
	/* 获取登录用户ID */
	public int getLoginUserid() {
		User user = getLoginUser();
		if (user != null) {
			//return new Long(((BigDecimal) user.get("userid")).longValue());
			return user.getUserid();
		}
		return 0;
	}

	/* 获取登录用户名 */
	public String getLoginUsername() {
		User user = getLoginUser();
		if (user != null) {
			return (String) user.getUsername();
		}
		return null;
	}

	/* 获取登录用户对象 */
	public User getLoginUser() {
		return (User) getSession().getAttribute(Constants.LOGIN_USER);
	}
}
