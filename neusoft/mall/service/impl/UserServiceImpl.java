package cn.edu.neusoft.mall.service.impl;

import cn.edu.neusoft.framework.exception.ServiceException;
import cn.edu.neusoft.framework.util.MD5Util;
import cn.edu.neusoft.mall.dao.UserDao;
import cn.edu.neusoft.mall.dao.impl.UserDaoImpl;
import cn.edu.neusoft.mall.entiy.User;
import cn.edu.neusoft.mall.service.UserService;
import cn.edu.neusoft.mall.until.Constant;

import javax.servlet.http.HttpSession;
import java.util.Date;
import java.util.UUID;

public class UserServiceImpl implements UserService {
    UserDao userDao = new UserDaoImpl();

    @Override
    public User login(User user) {
        User tmp = userDao.gerUserByNumber(user.getUsername());
        if (tmp == null) {
            throw new ServiceException("用户不存在");
        } else {
            if (MD5Util.md5(user.getPassword()).equals(tmp.getPasswordSecret())) {
                return tmp;
            } else {
                throw new ServiceException("密码错误,请重新输入");
            }
        }

    }

    @Override
    public boolean existUser(String userName) {
        User user=userDao.selectUserByName(userName);
        if(user!=null){
            return true;
        }else{
            return false;
        }
    }

    @Override
    public int register(User user) {
        // 必填字段
        user.setCreate_time(new Date());
        user.setPasswordSecret(MD5Util.md5(user.getPassword()));

        user.setCreate_time(new Date());
        user.setPasswordSecret(MD5Util.md5(user.getPassword()));

        // 选填字段默认值
        user.setEmail(user.getEmail() != null ? user.getEmail() : "default@example.com");
        user.setAvatar(user.getAvatar() != null ? user.getAvatar() : "/res/user/avatar/2415e800-e7f7-45f5-9a02-7675988e0e34.jpeg");
        user.setPhone(user.getPhone() != null ? user.getPhone() : "19824327051");

        return userDao.insert(user);

    }

    @Override
    public User getCurrentUser(HttpSession session) {
        return (User)session.getAttribute(Constant.LOGIN_USER);
    }
}
