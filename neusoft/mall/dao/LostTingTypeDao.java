package cn.edu.neusoft.mall.dao;

import cn.edu.neusoft.mall.entiy.LostTingType;

import java.util.List;

public interface LostTingTypeDao {
    public List<LostTingType> getLostTingTypeList(String itemId);
}
