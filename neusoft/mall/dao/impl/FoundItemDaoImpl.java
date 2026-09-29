package cn.edu.neusoft.mall.dao.impl;

import cn.edu.neusoft.mall.dao.FoundItemDao;
import cn.edu.neusoft.mall.entiy.FoundItem;

import java.sql.*;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class FoundItemDaoImpl implements FoundItemDao {
    private static final String DB_URL = "jdbc:mysql://localhost:3306/lost_thing?serverTimezone=Asia/Shanghai&useUnicode=true&characterEncoding=utf8";
    private static final String DB_USER = "root";
    private static final String DB_PASSWORD = "Dike221811!!@@";
    @Override
    public List<FoundItem> getFoundItemsByDateRange(Date startDate, Date endDate) {
        List<FoundItem> foundItems = new ArrayList<>();
        String sql = "SELECT * FROM found_items WHERE found_date BETWEEN ? AND ?";
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setDate(1, new java.sql.Date(startDate.getTime()));
            pstmt.setDate(2, new java.sql.Date(endDate.getTime()));
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                FoundItem foundItem = new FoundItem();
                foundItem.setId(rs.getInt("id"));
                foundItem.setLostItemId(rs.getInt("lost_item_id"));
                foundItem.setFoundDate(rs.getDate("found_date"));
                foundItems.add(foundItem);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return foundItems;
    }

    @Override
    public int getTotalFoundItems() {
        int total = 0;
        String sql = "SELECT COUNT(*) FROM found_items";
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {
            if (rs.next()) {
                total = rs.getInt(1);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return total;
    }

    @Override
    public int getFoundItemsByCategory(String category) {
        int count = 0;
        String sql = "SELECT COUNT(*) FROM found_items fi " +
                "JOIN lost_items li ON fi.lost_item_id = li.id " +
                "WHERE li.category = ?";
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, category);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                count = rs.getInt(1);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return count;
    }
}
