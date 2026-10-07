package cn.edu.neu.springbootdemo.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import cn.edu.neu.springbootdemo.model.User;

@Mapper//使用@Mapper注解，表示这是操作数据库的mapper，SpringBoot自动扫描
public interface UserMapper {

	/**
	 * 登录验证：按用户名和密码查询用户，查到则返回该用户，否则返回 null
	 */
	User existsUser(User user);

	/**
	 * 判断用户名是否已被占用，返回同名用户数量（0 表示用户名可用）
	 * 对应指导书中 int existUser(User user)
	 */
	int countUser(User user);

	/**
	 * 新增用户
	 */
	void addUser(User user);

	/**
	 * 查询用户列表，参数为空时查询全部；带条件时做组合查询（用户名模糊、性别、生日区间）
	 */
	List<User> getUserList(User user);

	/**
	 * 统计满足组合查询条件的记录总数，用于分页
	 */
	int getUserListCount(User user);

	/**
	 * 按用户id查询单个用户（修改前回显数据用）
	 */
	User getUser(@Param("userid") String userid);

	/**
	 * 修改用户，返回影响行数
	 */
	int updateUser(User user);

	/**
	 * 删除用户，返回影响行数
	 * userids 非空时按批量删除（多个id用英文逗号分隔），否则按 userid 单条删除
	 */
	int deleteUser(User user);

}
