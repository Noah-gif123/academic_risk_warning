package cn.edu.neusoft.mall.dao.impl;

import cn.edu.neusoft.framework.db.JDBCTemplate;
import cn.edu.neusoft.framework.db.RowMapper;
import cn.edu.neusoft.mall.dao.LostTingDao;
import cn.edu.neusoft.mall.entiy.LostTing;
import java.sql.ResultSet;
import java.util.Arrays;
import java.util.List;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import static java.sql.DriverManager.getConnection;

public class LostTingDaoImpl implements LostTingDao {

    RowMapper rowMapper = new RowMapper() {
        @Override
        public Object mapRow(ResultSet rs, int rowNum) throws SQLException {
            LostTing lostTing = new LostTing();
            lostTing.setId(rs.getInt("id"));
            lostTing.setTitle(rs.getNString("title"));
            lostTing.setDescription(rs.getNString("description"));
            lostTing.setItem_type_id(rs.getInt("item_type_id"));
            lostTing.setUser_id(rs.getInt("user_id"));
            lostTing.setStatus(rs.getNString("status"));
            lostTing.setLocation_id(rs.getInt("location_id"));
            lostTing.setDate(rs.getDate("date"));
            lostTing.setContact_info(rs.getNString("contact_info"));
            lostTing.setCreate_time(rs.getDate("create_time"));
            lostTing.setUpdate_time(rs.getDate("update_time"));
            lostTing.setImage_path(rs.getNString("image_path"));
            lostTing.setDay(rs.getNString("day"));
            lostTing.setLocation(rs.getNString("location"));
            lostTing.setItemName(rs.getNString("itemName"));
            lostTing.setCategoryId(rs.getInt("categoryId"));
            lostTing.setUniqueFeatures(rs.getNString("uniqueFeatures"));
            lostTing.setPrivateContact(rs.getBoolean("privateContact"));
            lostTing.setVerifyIdentity(rs.getBoolean("verifyIdentity"));
            return lostTing;
        }
    };

    @Override
    public List<LostTing> getLostTingList() {
        String sql = "select * from item order by id desc limit  22;";
        return JDBCTemplate.query(sql, rowMapper, null);
    }

    @Override
    public List<LostTing> getLostTingListByName(String keyword) {
        if (keyword == null) {
            keyword = ""; // 处理keyword为null的情况
        }
        System.out.println("搜索关键词: " + keyword);
        String sql = "select * from item where title like ?";
        System.out.println("执行SQL: " + sql);
        System.out.println("参数: " + "%" + keyword + "%");
        return JDBCTemplate.query(sql, rowMapper, "%" + keyword + "%");
    }
    @Override
    public List<LostTing> getLostTingById(int id) {
        String sql = "SELECT * FROM item WHERE id = ?";
        return JDBCTemplate.query(sql, rowMapper, id);
    }
    @Override
    public int insertLostTing(LostTing lostTing) {
        // 移除 contactWay 字段和对应的占位符
        String sql = "INSERT INTO item (title, description, user_id, status, location_id, date, contact_info,create_time, update_time, image_path, day, location, itemName, categoryId, uniqueFeatures, privateContact, verifyIdentity) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
        Object[] params = {
                lostTing.getTitle(),
                lostTing.getDescription(),
                lostTing.getUser_id(),
                lostTing.getStatus(),
                lostTing.getLocation_id(),
                lostTing.getDate(),
                lostTing.getContact_info(),
                lostTing.getCreate_time(),
                lostTing.getUpdate_time(),
                lostTing.getImage_path(),
                lostTing.getDay(),
                lostTing.getLocation(),
                lostTing.getItemName(),
                lostTing.getCategoryId(),
                lostTing.getUniqueFeatures(),
                lostTing.isPrivateContact(),
                lostTing.isVerifyIdentity()
        };
        System.out.println("执行 SQL: " + sql);
        System.out.println("参数: " + Arrays.toString(params));
        return JDBCTemplate.update(sql, params);
    }
    @Override
    public int getLostCount() {
        String sql = "SELECT COUNT(*) FROM item WHERE status = '丢失'";
        Object[] params = {};
        try {
            return ((Number) JDBCTemplate.queryForObject(sql, (rs, rowNum) -> rs.getObject(1), params)).intValue();
        } catch (Exception e) {
            e.printStackTrace();
            return 0;
        }
    }
    @Override
    public int getFoundCount() {
        String sql = "SELECT COUNT(*) FROM item WHERE status = '已找回'";
        Object[] params = {};
        try {
            return ((Number) JDBCTemplate.queryForObject(sql, (rs, rowNum) -> rs.getObject(1), params)).intValue();
        } catch (Exception e) {
            e.printStackTrace();
            return 0;
        }
    }

    @Override
    public List<LostTing> getLostTingListByUserId(int userId) {
        String sql = "SELECT * FROM item WHERE user_id = ?";
        return JDBCTemplate.query(sql, rowMapper, userId);
    }
}
