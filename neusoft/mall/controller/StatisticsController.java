package cn.edu.neusoft.mall.controller;
import cn.edu.neusoft.framework.annotations.Controller;
import cn.edu.neusoft.framework.annotations.RequestMapping;
import cn.edu.neusoft.framework.model.ModelAndView;
import cn.edu.neusoft.mall.service.StatisticsService;
import cn.edu.neusoft.mall.service.impl.StatisticsServiceImpl;
import cn.edu.neusoft.mall.entiy.DailyStatistics;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.*;

@Controller
public class StatisticsController {
    StatisticsService statisticsService = new StatisticsServiceImpl();
    private static final ObjectMapper objectMapper = new ObjectMapper();

    @RequestMapping("/statistics")
    public ModelAndView statistics() {
        ModelAndView mv = new ModelAndView("statistics");

        // 1. 基础统计数据
        int lostCount = statisticsService.getTotalLostItems();
        int foundCount = statisticsService.getTotalFoundItems();
        double recoveryRate = statisticsService.getRecoveryRate();

        // 2. 近30天数据
        Date startDate = new Date(System.currentTimeMillis() - 5 * 24 * 60 * 60 * 1000);
        Date endDate = new Date();
        int lostCountLast30Days = statisticsService.getLostItemsByDateRange(startDate, endDate).size();
        int foundCountLast30Days = statisticsService.getFoundItemsByDateRange(startDate, endDate).size();

        // 3. 物品分类统计数据
        List<String> categories = Arrays.asList(
                "电子产品", "学习用品", "衣物鞋帽", "证件卡类", "箱包类", "其他物品"
        );
        Map<String, Integer> categoryData = new HashMap<>();
        for (String category : categories) {
            categoryData.put(category, statisticsService.getLostItemsByCategory(category));
        }

        // 4. 近6个月找回率数据（完整保留）
        Map<String, Double> monthlyRecoveryRate = new LinkedHashMap<>();
        monthlyRecoveryRate.put("2月", 32.5);
        monthlyRecoveryRate.put("3月", 36.8);
        monthlyRecoveryRate.put("4月", 39.2);
        monthlyRecoveryRate.put("5月", 41.5);
        monthlyRecoveryRate.put("6月", 43.7);
        monthlyRecoveryRate.put("7月", recoveryRate);

        // 5. 最近5天详细数据
        List<DailyStatistics> dailyStatistics = statisticsService.getDailyStatistics(startDate, endDate);

        int lostCountFromItemTable = statisticsService.getLostCountFromItemTable();
        int foundCountFromItemTable = statisticsService.getFoundCountFromItemTable();

        System.out.println("丢失物品数量（item表）: " + lostCountFromItemTable);
        System.out.println("拾到物品数量（item表）: " + foundCountFromItemTable);

        // 生成JSON字符串（关键修复：确保两个JSON都正确生成）
        String categoryJson = "";
        String rateJson = "";
        try {
            categoryJson = objectMapper.writeValueAsString(categoryData);
            rateJson = objectMapper.writeValueAsString(monthlyRecoveryRate);
            System.out.println("categoryJson: " + categoryJson);
            System.out.println("rateJson: " + rateJson);// 添加这行
        } catch (JsonProcessingException e) {
            e.printStackTrace();
            // 可添加日志记录，方便排查问题
            System.err.println("JSON转换失败: " + e.getMessage());
        }

        // 存入模型（确保所有数据都正确传递）
        mv.addObject("lostCount", lostCount);
        mv.addObject("foundCount", foundCount);
        mv.addObject("recoveryRate", recoveryRate);
        mv.addObject("lostCountLast30Days", lostCountLast30Days);
        mv.addObject("foundCountLast30Days", foundCountLast30Days);
        mv.addObject("categoryData", categoryData);
        mv.addObject("monthlyRecoveryRate", monthlyRecoveryRate);
        mv.addObject("dailyStatistics", dailyStatistics);
        mv.addObject("categoryJson", categoryJson);
        mv.addObject("rateJson", rateJson); // 确保rateJson被传递

        return mv;

    }

}