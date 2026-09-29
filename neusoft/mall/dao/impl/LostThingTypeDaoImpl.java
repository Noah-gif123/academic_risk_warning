package cn.edu.neusoft.mall.dao.impl;

import cn.edu.neusoft.framework.db.JDBCTemplate;
import cn.edu.neusoft.framework.db.RowMapper;
import cn.edu.neusoft.mall.dao.LostTingTypeDao;
import cn.edu.neusoft.mall.entiy.LostTingType;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

public class LostThingTypeDaoImpl implements LostTingTypeDao {
    RowMapper rowMapper = new RowMapper() {
        @Override
        public Object mapRow(ResultSet rs, int rowNum) throws SQLException {
            LostTingType lostTingType = new LostTingType();
            lostTingType.setItem_id(rs.getInt("item_id"));
            lostTingType.setType_name(rs.getNString("type_name"));
            lostTingType.setCreate_time(rs.getDate("create_time"));
            lostTingType.setDay(rs.getNString("day"));
            return lostTingType;
        }
    };
    @Override
    public List<LostTingType> getLostTingTypeList(String item_id) {
        String sql = "select * from item_type where item_id = ?";
        return JDBCTemplate.query(sql,rowMapper,item_id);
    }
}
