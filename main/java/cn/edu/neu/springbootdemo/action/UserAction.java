package cn.edu.neu.springbootdemo.action;


import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import cn.edu.neu.springbootdemo.core.Constants;
import cn.edu.neu.springbootdemo.model.User;
import cn.edu.neu.springbootdemo.service.UserService;

/**
 * 控制层：对外只暴露 /user/xxx 接口，跨域统一由 CrossOriginInterceptor 处理
 */
@RestController
@RequestMapping("/user")
public class UserAction {

	@Autowired
	private UserService userService;

	/**
	 * 用户登录
	 * 访问：/springbootdemo/user/login?username=admin&password=123
	 */
	@RequestMapping("/login")
	public Map<String, String> login(User user, HttpSession session) {
		Map<String, String> map = new HashMap<String, String>();
		User dbUser = userService.existsUser(user);
		if (dbUser != null) {
			// 登录成功，向session中存入用户信息
			session.setAttribute(Constants.LOGIN_USER, dbUser);
			map.put("login", "yes");
		} else {
			map.put("login", "no");
		}
		return map;
	}

	/**
	 * 退出登录：让服务端 session 失效
	 * 访问：/springbootdemo/user/logout
	 */
	@RequestMapping("/logout")
	public Map<String, String> logout(HttpSession session) {
		// 退出时销毁会话，避免只清前端缓存后仍能访问数据
		session.invalidate();
		Map<String, String> map = new HashMap<String, String>();
		map.put("result", "yes");
		map.put("msg", "已退出登录");
		return map;
	}

	/**
	 * 用户注册
	 * 访问：/springbootdemo/user/addUser?username=11&password=123
	 */
	@RequestMapping("/addUser")
	public Map<String, String> addUser(User user) {
		Map<String, String> map = new HashMap<String, String>();
		int result = userService.addUser(user);
		if (result == 0) {
			map.put("result", "yes");
			map.put("msg", "恭喜您，注册成功");
		} else if (result == 1) {
			map.put("result", "no");
			map.put("msg", "对不起，该用户名已存在，请更换");
		} else {
			map.put("result", "no");
			map.put("msg", "对不起，数据输入格式有误，注册失败");
		}
		return map;
	}

	/**
	 * 显示所有用户 / 按条件查询用户
	 * 访问：/springbootdemo/user/getUserList
	 *      /springbootdemo/user/getUserList?username=11
	 *      /springbootdemo/user/getUserList?username=11&gender=0
	 *      /springbootdemo/user/getUserList?birthdate1=2021-01-01&birthdate2=2021-12-31
	 * getAllUsers 为 getUserList 的别名，兼容前端不同写法
	 */
	@RequestMapping({ "/getUserList", "/getAllUsers" })
	public Map<String, Object> getUserList(User user) {
		Map<String, Object> map = new HashMap<String, Object>();
		map.put("userList", userService.getUserList(user));
		// 传入 pageSize 时额外返回分页信息（不传则与原来一致，返回全部记录）
		if (user.getPageSize() > 0) {
			int total = userService.getUserListCount(user);
			int pageSize = user.getPageSize();
			map.put("total", total);
			map.put("pageNo", user.getPageNo());
			map.put("pageSize", pageSize);
			map.put("totalPage", total % pageSize == 0 ? total / pageSize : total / pageSize + 1);
		}
		return map;
	}

	/**
	 * 按id查询单个用户，用于修改前的数据回显
	 * 访问：/springbootdemo/user/getUser?userid=1
	 */
	@RequestMapping("/getUser")
	public Map<String, User> getUser(String userid) {
		Map<String, User> map = new HashMap<String, User>();
		map.put("user", userService.getUser(userid));
		return map;
	}

	/**
	 * 修改用户
	 * 访问：/springbootdemo/user/updateUser?userid=1&password=111&gender=1
	 */
	@RequestMapping("/updateUser")
	public Map<String, String> updateUser(User user) {
		Map<String, String> map = new HashMap<String, String>();
		if (userService.updateUser(user)) {
			map.put("result", "yes");
			map.put("msg", "恭喜您，修改成功");
		} else {
			map.put("result", "no");
			map.put("msg", "对不起，数据输入格式有误，修改失败");
		}
		return map;
	}

	/**
	 * 删除用户，支持批量删除
	 * 访问：/springbootdemo/user/deleteUser?userid=1
	 *      /springbootdemo/user/deleteUser?userid=1&userid=2   （多值参数，批量删除）
	 *      /springbootdemo/user/deleteUser?userids=1,2,3       （逗号分隔，批量删除）
	 */
	@RequestMapping("/deleteUser")
	public Map<String, String> deleteUser(User user, HttpServletRequest request) {
		Map<String, String> map = new HashMap<String, String>();
		// 形如 userid=1&userid=2 的多值参数，拼接后按批量删除处理
		String[] useridArray = request.getParameterValues("userid");
		if (useridArray != null && useridArray.length > 1) {
			user.setUserids(String.join(",", useridArray));
		}
		if (userService.deleteUser(user)) {
			map.put("result", "yes");
			map.put("msg", "恭喜您，删除成功");
		} else {
			map.put("result", "no");
			map.put("msg", "对不起，删除失败");
		}
		return map;
	}

	/**
	 * 验证用户名是否可用
	 * 访问：/springbootdemo/user/checkUsername?username=admin
	 */
	@RequestMapping("/checkUsername")
	public Map<String, String> checkUsername(User user) {
		Map<String, String> map = new HashMap<String, String>();
		if (userService.checkUsername(user)) {
			map.put("result", "yes");
			map.put("msg", "");
		} else {
			map.put("result", "no");
			map.put("msg", "对不起，该用户名已存在，请更换");
		}
		return map;
	}

}
