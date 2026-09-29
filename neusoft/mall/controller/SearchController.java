package cn.edu.neusoft.mall.controller;

import cn.edu.neusoft.framework.annotations.Controller;
import cn.edu.neusoft.framework.annotations.RequestMapping;
import cn.edu.neusoft.framework.annotations.RequestParam;
import cn.edu.neusoft.framework.model.ModelAndView;
import cn.edu.neusoft.mall.entiy.LostTing;
import cn.edu.neusoft.mall.service.LostTingService;
import cn.edu.neusoft.mall.service.impl.LostTingServiceImpl;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

@Controller
public class SearchController {

    LostTingService lostTingService = new LostTingServiceImpl();

    @RequestMapping("/search")
    public ModelAndView search(HttpServletRequest request) {
        System.out.println("########## 进入 search 方法 ##########");
        String keyword = request.getParameter("keyword");
        System.out.println("搜索关键词: " + keyword);

        List<LostTing> lostTingList = lostTingService.getLostTingListByName(keyword);
        System.out.println("查询结果数量: " + (lostTingList != null ? lostTingList.size() : 0));

        ModelAndView mv = new ModelAndView("search");
        mv.addObject("lostTingList", lostTingList);
        mv.addObject("keyword", keyword);
        return mv;
    }
}

