package cn.edu.neusoft.mall.dao;

import cn.edu.neusoft.mall.entiy.LostTing;
import java.util.List;

public interface LostTingDao {

    public List<LostTing> getLostTingList();
    public List<LostTing> getLostTingListByName(String keyword);
    List<LostTing> getLostTingById(int id);
    public int insertLostTing(LostTing lostTing);
     int getLostCount();
     int getFoundCount();
    List<LostTing> getLostTingListByUserId(int userId);

}