package cn.edu.neusoft.mall.service.impl;

import cn.edu.neusoft.framework.db.PageInfo;
import cn.edu.neusoft.mall.dao.AddressDao;
import cn.edu.neusoft.mall.dao.impl.AddressDaoImpl;
import cn.edu.neusoft.mall.entiy.Address;
import cn.edu.neusoft.mall.service.AddressService;

public class AddressServiceImpl implements AddressService {
    AddressDao addressDao = new AddressDaoImpl();
    @Override
    public PageInfo<Address> getAddressList(Integer page, String user) {
        return addressDao.selectList(page, user);
    }

    @Override
    public int insertAddress(Address address) {
        return addressDao.insert(address);
    }


    @Override
    public int modifyAddress(Address address) {
        return addressDao.modifyAddress(address);
    }

    @Override
    public int deleteAddress(String id) {
        return addressDao.deleteById(id);
    }
}
