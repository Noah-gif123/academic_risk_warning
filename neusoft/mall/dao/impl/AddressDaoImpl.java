package cn.edu.neusoft.mall.dao.impl;

import cn.edu.neusoft.framework.db.JDBCTemplate;
import cn.edu.neusoft.framework.db.PageInfo;
import cn.edu.neusoft.framework.db.RowMapper;
import cn.edu.neusoft.mall.dao.AddressDao;
import cn.edu.neusoft.mall.entiy.Address;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;

public class AddressDaoImpl implements AddressDao {
    private RowMapper<Address> rowMapper=new RowMapper<Address>() {
        @Override
        public Address mapRow(ResultSet rs, int rowNum) throws SQLException {
            Address address=new Address();
            address.setId(rs.getNString("id"));
            address.setUserId(rs.getNString("user_id"));
            address.setProvince(rs.getString("province"));
            address.setCity(rs.getString("city"));
            address.setDistrict(rs.getString("district"));
            address.setDetailAddress(rs.getNString("detail_address"));
            return address;
        }
    };
    @Override
    public PageInfo<Address> selectList(Integer page, String user) {
        try {
            String sql="select id,user_id,province,city,district,detail_address from address where user_id = ?";
            return JDBCTemplate.getPage(sql, rowMapper, new Object[] {user}, page);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return new PageInfo<Address>();
    }

    @Override
    public int insert(Address address) {
        String sql = "INSERT INTO address (id, user_id, province, " +
                "city, district, detail_address ) VALUES ( ?, ?, ?, ?, ?, ? )";
        try {
            return  JDBCTemplate.update(sql, UUID.randomUUID().toString(),address.getUserId(),
                    address.getProvince(),address.getCity(),address.getDistrict(),
                    address.getDetailAddress());
        } catch (Exception e){
            e.printStackTrace();
        }
        return 0;
    }

    @Override
    public int modifyAddress(Address address) {
        String sql="update address set province=?,city=?,district=?,detail_address=? where id=?";
        return JDBCTemplate.update(sql,new Object[]{address.getProvince(),address.getCity(), address.getDistrict(),address.getDetailAddress(),address.getId()});
    }

    @Override
    public int deleteById(String id) {
        String sql = "DELETE FROM address WHERE id = ?";
        System.out.println(sql);
        return JDBCTemplate.update(sql, id);
    }

}
