package cn.edu.neusoft.mall.dao;

import cn.edu.neusoft.mall.entiy.User;

public interface UserDao {
        User gerUserByNumber(String Number);
        User selectUserByName(String userName);
        int insert(User row);
    }

