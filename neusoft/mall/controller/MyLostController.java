package cn.edu.neusoft.mall.controller;

import cn.edu.neusoft.framework.annotations.Controller;
import cn.edu.neusoft.framework.annotations.RequestMapping;
import cn.edu.neusoft.framework.model.ModelAndView;
import cn.edu.neusoft.mall.entiy.LostTing;
import cn.edu.neusoft.mall.entiy.User;
import cn.edu.neusoft.mall.service.LostTingService;
import cn.edu.neusoft.mall.service.impl.LostTingServiceImpl;
import cn.edu.neusoft.mall.until.Constant;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.util.List;

@Controller
public class MyLostController {
    LostTingService lostTingService = new LostTingServiceImpl();

    @RequestMapping("/myLost")
    public ModelAndView myLost(HttpServletRequest request) {
        ModelAndView mv = new ModelAndView("myLost");

        HttpSession session = request.getSession(false);
        if (session != null) {
            Object userObj = session.getAttribute(Constant.LOGIN_USER);
            if (userObj != null && userObj instanceof User) {
                User userInfo = (User) userObj;
                int userId = userInfo.getId();

                List<LostTing> lostTingList = lostTingService.getLostTingListByUserId(userId);
                mv.addObject("lostTingList", lostTingList);
            }
        }

        return mv;
    }
}