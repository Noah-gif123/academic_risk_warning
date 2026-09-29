package cn.edu.neusoft.mall.dao.impl;

import cn.edu.neusoft.framework.db.JDBCTemplate;
import cn.edu.neusoft.framework.db.RowMapper;
import cn.edu.neusoft.mall.dao.LocationDao;
import cn.edu.neusoft.mall.entiy.Location;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

public class LocationDaoImpl implements LocationDao
{
    RowMapper rowMapper = new RowMapper() {
        @Override
        public Object mapRow(ResultSet rs, int rowNum) throws SQLException {

            Location location = new Location();
            location.setId(rs.getInt("id"));
            location.setLocation_name(rs.getNString("location_name"));
            location.setCreate_time(rs.getDate("create_time"));
            location.setDay(rs.getNString("day"));
            return location;
        }
    };
    @Override
    public List<Location> getLocationListById(String id)  {
        String sql = "select * from location where id = ?;";
        return JDBCTemplate.query(sql,rowMapper,id);
    }


}
