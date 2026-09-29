package cn.edu.neusoft.mall.controller;

import cn.edu.neusoft.framework.annotations.Controller;
import cn.edu.neusoft.framework.annotations.RequestMapping;
import cn.edu.neusoft.framework.model.ModelAndView;
import cn.edu.neusoft.mall.entiy.LostTing;
import cn.edu.neusoft.mall.service.LostTingService;
import cn.edu.neusoft.mall.service.impl.LostTingServiceImpl;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.List;

@Controller
public class FavoriteController {
    LostTingService lostTingService = new LostTingServiceImpl();

    @RequestMapping("/favorites")
    public ModelAndView getFavourite(HttpServletRequest request) {



        List<LostTing> lostTingList=lostTingService.getlostTingList();


        ModelAndView mv = new ModelAndView("favorites");
        String paramNames = request.getParameter("favoriteIds");




        if (!paramNames.isEmpty()) {
            ArrayList lostInputList = new ArrayList();

            String[] paramNamesArr = paramNames.split(",");

            for (int i = 0; i < paramNamesArr.length; i++) {
                lostInputList.add(paramNamesArr[i]);
            }



            for (LostTing lost: lostTingList ) {
                String id = String.valueOf(lost.getId());
                boolean exists = lostInputList.contains(id);

                if (exists) {
                    lost.setFavorite(true);
                }
            }
        }

    mv.addObject("lostTingList",lostTingList);
        lostTingService.getlostTingList();
        return mv;
    }
}