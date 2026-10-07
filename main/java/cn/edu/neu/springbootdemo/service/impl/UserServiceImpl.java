package cn.edu.neu.springbootdemo.service.impl;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import cn.edu.neu.springbootdemo.mapper.UserMapper;
import cn.edu.neu.springbootdemo.model.User;
import cn.edu.neu.springbootdemo.service.UserService;


@Service //使用@Service注解，SpringBoot自动扫描
public class UserServiceImpl implements UserService{

	@Autowired
	private UserMapper userMapper;

	@Override
	public User existsUser(User user) {
		return userMapper.existsUser(user);
	}

	@Override
	public int addUser(User user) {
		// 用户名已存在，返回 1
		if (userMapper.countUser(user) > 0) {
			return 1;
		}
		try {
			userMapper.addUser(user);
			// 注册成功，返回 0
			return 0;
		} catch (Exception e) {
			e.printStackTrace();
			// 数据输入格式有误，注册失败，返回 2
			return 2;
		}
	}

	@Override
	public List<User> getUserList() {
		return userMapper.getUserList(new User());
	}

	@Override
	public List<User> getUserList(User user) {
		return userMapper.getUserList(user);
	}

	@Override
	public int getUserListCount(User user) {
		return userMapper.getUserListCount(user);
	}

	@Override
	public User getUser(String userid) {
		return userMapper.getUser(userid);
	}

	@Override
	public boolean updateUser(User user) {
		return userMapper.updateUser(user) > 0;
	}

	@Override
	public boolean deleteUser(User user) {
		return userMapper.deleteUser(user) > 0;
	}

	@Override
	public boolean checkUsername(User user) {
		return userMapper.countUser(user) == 0;
	}

}
