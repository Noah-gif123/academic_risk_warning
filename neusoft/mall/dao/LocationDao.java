package cn.edu.neusoft.mall.dao;

import cn.edu.neusoft.mall.entiy.Location;

import java.util.List;

public interface LocationDao {
    public List<Location> getLocationListById(String id);
}
