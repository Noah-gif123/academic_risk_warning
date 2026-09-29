package cn.edu.neusoft.mall.service.impl;

import cn.edu.neusoft.mall.dao.FoundItemDao;
import cn.edu.neusoft.mall.dao.LostItemDao;
import cn.edu.neusoft.mall.dao.LostTingDao;
import cn.edu.neusoft.mall.dao.impl.FoundItemDaoImpl;
import cn.edu.neusoft.mall.dao.impl.LostItemDaoImpl;
import cn.edu.neusoft.mall.dao.impl.LostTingDaoImpl;
import cn.edu.neusoft.mall.entiy.DailyStatistics;
import cn.edu.neusoft.mall.entiy.FoundItem;
import cn.edu.neusoft.mall.entiy.LostItem;
import cn.edu.neusoft.mall.entiy.LostTing;
import cn.edu.neusoft.mall.service.StatisticsService;

import java.util.Date;
import java.util.List;

public class StatisticsServiceImpl implements StatisticsService {
    private LostItemDao lostItemDao = new LostItemDaoImpl();
    private FoundItemDao foundItemDao = new FoundItemDaoImpl();
    private LostTingDao lostTingDao = new LostTingDaoImpl();

    @Override
    public int getTotalLostItems() {
        return lostItemDao.getTotalLostItems();
    }

    @Override
    public int getTotalFoundItems() {
        return foundItemDao.getTotalFoundItems();
    }

    @Override
    public double getRecoveryRate() {
        int lostCount = getTotalLostItems();
        int foundCount = getTotalFoundItems();
        if (lostCount == 0) {
            return 0.0;
        }
        return (double) foundCount / lostCount * 100;
    }

    @Override
    public List<LostItem> getLostItemsByDateRange(Date startDate, Date endDate) {
        return lostItemDao.getLostItemsByDateRange(startDate, endDate);
    }

    @Override
    public List<FoundItem> getFoundItemsByDateRange(Date startDate, Date endDate) {
        return foundItemDao.getFoundItemsByDateRange(startDate, endDate);
    }

    @Override
    public int getLostItemsByCategory(String category) {
        return lostItemDao.getLostItemsByCategory(category);
    }

    @Override
    public int getFoundItemsByCategory(String category) {
        return foundItemDao.getFoundItemsByCategory(category);
    }

    @Override
    public List<DailyStatistics> getDailyStatistics(Date startDate, Date endDate) {
        return lostItemDao.getDailyStatistics(startDate, endDate);
    }

    @Override
    public int getLostCountFromItemTable() {
        return lostTingDao.getLostCount();
    }

    @Override
    public int getFoundCountFromItemTable() {
        return lostTingDao.getFoundCount();
    }
}
