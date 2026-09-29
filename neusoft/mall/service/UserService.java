package cn.edu.neusoft.mall.service;


import cn.edu.neusoft.mall.entiy.User;

import javax.servlet.http.HttpSession;

public interface UserService {
    User login(User user);
    public boolean existUser(String userName);
    public int register(User user);
    User getCurrentUser(HttpSession session);
}
