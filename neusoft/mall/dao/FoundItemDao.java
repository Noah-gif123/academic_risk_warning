package cn.edu.neusoft.mall.dao;

import cn.edu.neusoft.mall.entiy.FoundItem;

import java.util.Date;
import java.util.List;

public interface FoundItemDao {
    List<FoundItem> getFoundItemsByDateRange(Date startDate, Date endDate);
    int getTotalFoundItems();
    int getFoundItemsByCategory(String category);
}
