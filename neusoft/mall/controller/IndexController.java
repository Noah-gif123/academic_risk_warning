package cn.edu.neusoft.mall.controller;

import cn.edu.neusoft.framework.annotations.Controller;
import cn.edu.neusoft.framework.annotations.RequestMapping;
import cn.edu.neusoft.framework.model.ModelAndView;

import cn.edu.neusoft.mall.entiy.LostTing;
import cn.edu.neusoft.mall.service.LostTingService;
import cn.edu.neusoft.mall.service.impl.LostTingServiceImpl;

import java.util.List;

@Controller
public class IndexController {
    LostTingService lostTingService = new LostTingServiceImpl();

    @RequestMapping("/index")
    public ModelAndView mallIndex() {
        System.out.println("【IndexController】处理首页请求");
        ModelAndView mv=new ModelAndView("index");
        List<LostTing> lostTingList=lostTingService.getlostTingList();
        mv.addObject("title", "失物招领系统首页");
        mv.addObject("lostTingList",lostTingList);
        lostTingService.getlostTingList();
        return  mv;
    }


    }


