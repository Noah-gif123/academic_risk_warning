package cn.edu.neusoft.mall.controller;


import cn.edu.neusoft.framework.annotations.Controller;
import cn.edu.neusoft.framework.annotations.RequestMapping;
import cn.edu.neusoft.framework.model.ModelAndView;
import cn.edu.neusoft.mall.entiy.LostTing;
import cn.edu.neusoft.mall.service.LostTingService;
import cn.edu.neusoft.mall.service.impl.LostTingServiceImpl;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

@Controller
public class AllThingsController {
    LostTingService lostTingService = new LostTingServiceImpl();

    @RequestMapping("/allThings")
    public ModelAndView allThings(HttpServletRequest request){
        ModelAndView mv = new ModelAndView("allThings");
        List<LostTing> lostTingList=lostTingService.getlostTingList();
        mv.addObject("lostTingList",lostTingList);
        lostTingService.getlostTingList();
        return mv;
    }
}
