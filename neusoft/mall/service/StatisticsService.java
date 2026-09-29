package cn.edu.neusoft.mall.service;

import cn.edu.neusoft.mall.entiy.DailyStatistics;
import cn.edu.neusoft.mall.entiy.FoundItem;
import cn.edu.neusoft.mall.entiy.LostItem;

import java.util.Date;
import java.util.List;

public interface StatisticsService {
    int getTotalLostItems();
    int getTotalFoundItems();
    double getRecoveryRate();
    List<LostItem> getLostItemsByDateRange(Date startDate, Date endDate);
    List<FoundItem> getFoundItemsByDateRange(Date startDate, Date endDate);
    int getLostItemsByCategory(String category);
    int getFoundItemsByCategory(String category);
    List<DailyStatistics> getDailyStatistics(Date startDate, Date endDate);
    int getLostCountFromItemTable();
    int getFoundCountFromItemTable();

}
