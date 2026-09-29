package cn.edu.neusoft.mall.dao;

import cn.edu.neusoft.mall.entiy.DailyStatistics;
import cn.edu.neusoft.mall.entiy.LostItem;

import java.util.Date;
import java.util.List;

public interface LostItemDao {
    List<LostItem> getLostItemsByDateRange(Date startDate, Date endDate);
    int getTotalLostItems();
    int getLostItemsByCategory(String category);
    List<DailyStatistics> getDailyStatistics(Date startDate, Date endDate);
}
