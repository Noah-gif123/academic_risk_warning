package cn.edu.neu.springbootdemo.mapper;

import org.apache.ibatis.annotations.Mapper;
import cn.edu.neu.springbootdemo.model.User;

@Mapper//使用@Mapper注解，表示这是操作数据库的mapper，SpringBoot自动扫描
public interface UserMapper {
	User existsUser(User user);
}
