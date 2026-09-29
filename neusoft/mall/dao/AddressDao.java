package cn.edu.neusoft.mall.dao;

import cn.edu.neusoft.framework.db.PageInfo;
import cn.edu.neusoft.mall.entiy.Address;

public interface AddressDao {
    PageInfo<Address> selectList(Integer page, String user);
    int insert(Address address);
    int deleteById(String id);
    int modifyAddress(Address address);
}
