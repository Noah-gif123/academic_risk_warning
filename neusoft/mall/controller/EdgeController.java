package cn.edu.neusoft.mall.controller;

import cn.edu.neusoft.framework.annotations.Controller;
import cn.edu.neusoft.framework.annotations.RequestMapping;
import cn.edu.neusoft.framework.annotations.RequestParam;
import cn.edu.neusoft.framework.db.PageInfo;
import cn.edu.neusoft.framework.enums.RequestMethod;
import cn.edu.neusoft.framework.exception.ServiceException;
import cn.edu.neusoft.framework.model.ModelAndView;

import cn.edu.neusoft.mall.entiy.User;
import cn.edu.neusoft.mall.service.UserService;
import cn.edu.neusoft.mall.service.impl.UserServiceImpl;
import cn.edu.neusoft.mall.until.Constant;

import javax.servlet.http.HttpSession;
import java.util.UUID;

@Controller
@RequestMapping("/user")
public class EdgeController {
    private UserService userService = new UserServiceImpl();

    @RequestMapping(value = "/toLogin", methods = {RequestMethod.GET})
    public ModelAndView login() {
        return new ModelAndView("edge/login");
    }

    @RequestMapping(value = "/login", methods = {RequestMethod.POST})
    public String login(User user, HttpSession session) {
        User userInfo = userService.login(user);
        if (userInfo != null) {
            session.setAttribute(Constant.LOGIN_USER, userInfo);
            return "redirect:../index.action";
        } else {
            return "redirect:../toLogin.action";
        }
    }

    @RequestMapping(value = "/toRegister", methods = {RequestMethod.GET})
    public ModelAndView register() {
        return new ModelAndView("edge/register");
    }

    @RequestMapping(value = "/registerComplete", methods = {RequestMethod.POST}) // 添加请求方法限制
    public ModelAndView registerComplete(User user) {
        ModelAndView mv = new ModelAndView("edge/register");

        try {
            // 1. 检查用户名是否存在 (逻辑优化)
            if (userService.existUser(user.getUsername())) {
                throw new ServiceException("用户名已存在");
            }

            // 2. 验证密码匹配
            if (!user.getPassword().equals(user.getPasswordConfirm())) {
                throw new ServiceException("两次密码不一致");
            }

            System.out.println("尝试注册用户: " + user.getUsername());

            // 3. 使用事务确保数据一致性
            int result = userService.register(user);

            System.out.println("注册结果: " + result);

            // 4. 改进结果判断逻辑
            if (result > 0) {
                return new ModelAndView("edge/register-complete");
            } else {
                throw new ServiceException("注册失败，请重试");
            }
        } catch (ServiceException e) {
            // 5. 记录错误日志而不是打印堆栈跟踪
            System.err.println("注册异常: " + e.getMessage());
            mv.addObject("error", e.getMessage());
            return mv;
        } catch (Exception e) {
            // 6. 记录系统错误
            System.err.println("系统错误: " + e.getMessage());
            e.printStackTrace();
            mv.addObject("error", "系统错误，请联系管理员");
            return mv;
        }
    }
}