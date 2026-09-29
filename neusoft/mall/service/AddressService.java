package cn.edu.neusoft.mall.service;

import cn.edu.neusoft.framework.db.PageInfo;
import cn.edu.neusoft.mall.entiy.Address;

public interface AddressService {
    public PageInfo<Address> getAddressList(Integer page, String user);
    public int insertAddress(Address address);
    public int deleteAddress(String id);
    int modifyAddress(Address address);
}
