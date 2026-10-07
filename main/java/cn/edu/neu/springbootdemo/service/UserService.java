package cn.edu.neu.springbootdemo.service;

import java.util.List;

import cn.edu.neu.springbootdemo.model.User;

public interface UserService {

	/**
	 * 用户登录，登录成功返回用户信息，失败返回 null
	 */
	User existsUser(User user);

	/**
	 * 用户注册
	 * @return 0-注册成功；1-用户名已存在；2-数据输入有误，注册失败
	 */
	int addUser(User user);

	/**
	 * 查询全部用户
	 */
	List<User> getUserList();

	/**
	 * 按条件查询用户（用户名、性别、生日区间等组合查询）
	 */
	List<User> getUserList(User user);

	/**
	 * 统计满足组合查询条件的记录总数，用于分页
	 */
	int getUserListCount(User user);

	/**
	 * 按id查询单个用户，用于修改前的数据回显
	 */
	User getUser(String userid);

	/**
	 * 修改用户
	 */
	boolean updateUser(User user);

	/**
	 * 删除用户，userids 非空时为批量删除
	 */
	boolean deleteUser(User user);

	/**
	 * 校验用户名是否可用，可用返回 true
	 */
	boolean checkUsername(User user);

}
