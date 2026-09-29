package cn.edu.neusoft.mall.controller;

import cn.edu.neusoft.framework.annotations.Controller;
import cn.edu.neusoft.framework.annotations.RequestMapping;
import cn.edu.neusoft.framework.enums.RequestMethod;
import cn.edu.neusoft.framework.model.ModelAndView;
import cn.edu.neusoft.mall.entiy.LostTing;; // 假设你有这个表单实体类
import cn.edu.neusoft.mall.service.LostTingService;
import cn.edu.neusoft.mall.service.impl.LostTingServiceImpl;

import javax.servlet.http.HttpServletRequest;

@Controller
@RequestMapping("/apply")
public class ApplyController {
    LostTingService lostTingService = new LostTingServiceImpl();

    // 处理申请认领页面的跳转
    @RequestMapping(value = "/applyClaim",  methods = {RequestMethod.GET})
    public ModelAndView applyClaim(HttpServletRequest request) {
        String itemId = request.getParameter("id");
        if (itemId == null || itemId.isEmpty()) {
            return new ModelAndView("redirect:/allThings.action");
        }

        LostTing item = lostTingService.getById(Integer.parseInt(itemId));
        if (item == null) {
            return new ModelAndView("redirect:/allThings.action");
        }

        ModelAndView mv = new ModelAndView("apply/applyClaim");
        mv.addObject("item", item);
        return mv;
    }

    // 新增：处理表单提交（数据匹配逻辑）
    @RequestMapping(value = "/processClaim",  methods = {RequestMethod.POST})
    public ModelAndView processClaim(HttpServletRequest request) {
        // 获取表单提交的数据
        String itemId = request.getParameter("id");
        String title = request.getParameter("title");
        String location = request.getParameter("location");
        String description = request.getParameter("description");

        // 获取物品原始信息
        LostTing item = lostTingService.getById(Integer.parseInt(itemId));
        if (item == null) {
            return new ModelAndView("redirect:/allThings.action");
        }

        // 数据匹配逻辑（根据实际业务调整匹配规则）
        boolean isMatch = verifyClaimInfo(item, title, location, description);

        // 根据匹配结果跳转不同页面
        if (isMatch) {
            // 匹配成功，传递物品信息到成功页面
            boolean statusUpdated = lostTingService.updateStatus(item.getId(), "已找回");
            ModelAndView successMv = new ModelAndView("apply/claimSuccess");
            successMv.addObject("item", item);
            return successMv;
        } else {
            // 匹配失败，传递物品信息到失败页面
            ModelAndView failMv = new ModelAndView("apply/claimFailure");
            failMv.addObject("item", item);
            return failMv;
        }
    }

    // 数据匹配验证方法
    private boolean verifyClaimInfo(LostTing item, String title, String location, String description) {
        // 这里是核心匹配逻辑，根据实际需求调整
        // 示例规则：物品名称完全匹配，且描述有50%以上相似度
        boolean titleMatch = item.getTitle().equals(title);

        // 简单的描述匹配度检查
        int matchCount = 0;
        String[] descKeywords = description.split(" ");
        for (String keyword : descKeywords) {
            if (item.getDescription().contains(keyword)) {
                matchCount++;
            }
        }
        double matchRate = (double) matchCount / descKeywords.length;

        // 地点部分匹配
        boolean locationMatch = item.getLocation().contains(location) || location.contains(item.getLocation());

        // 返回综合匹配结果
        return titleMatch && locationMatch && matchRate > 0.5;
    }
}