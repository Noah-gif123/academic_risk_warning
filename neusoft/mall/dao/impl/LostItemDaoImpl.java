package cn.edu.neusoft.mall.dao.impl;

import cn.edu.neusoft.framework.db.RowMapper;
import cn.edu.neusoft.mall.dao.LostItemDao;
import cn.edu.neusoft.mall.entiy.DailyStatistics;
import cn.edu.neusoft.mall.entiy.LostItem;

import java.sql.*;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

public class LostItemDaoImpl implements LostItemDao {

    private static final String DB_URL = "jdbc:mysql://localhost:3306/lost_thing?serverTimezone=Asia/Shanghai&useUnicode=true&characterEncoding=utf8";
    private static final String DB_USER = "root";
    private static final String DB_PASSWORD = "Dike221811!!@@";
    RowMapper rowMapper = new RowMapper() {
        @Override
        public Object mapRow(ResultSet rs, int rowNum) throws SQLException {
            LostItem lostItem = new LostItem();
            lostItem.setId(rs.getInt("id"));
            lostItem.setLostDate(rs.getDate("lost_date"));
            lostItem.setCategory(rs.getNString("category"));
            lostItem.setStatus(rs.getNString("status"));
            lostItem.setUser_id(rs.getInt("user_id"));
            return lostItem;
        }
    };
    @Override
    public List<LostItem> getLostItemsByDateRange(Date startDate, Date endDate) {
        List<LostItem> lostItems = new ArrayList<>();
        String sql = "SELECT * FROM lost_items WHERE lost_date BETWEEN ? AND ?";
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setDate(1, new java.sql.Date(startDate.getTime()));
            pstmt.setDate(2, new java.sql.Date(endDate.getTime()));
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                LostItem lostItem = new LostItem();
                lostItem.setId(rs.getInt("id"));
                lostItem.setLostDate(rs.getDate("lost_date"));
                lostItem.setCategory(rs.getString("category"));
                lostItems.add(lostItem);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return lostItems;
    }

    @Override
    public int getTotalLostItems() {
        int total = 0;
        String sql = "SELECT COUNT(*) FROM lost_items";
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
    public int getLostItemsByCategory(String category) {

            int count = 0;
            String sql = "SELECT COUNT(*) FROM lost_items WHERE category = ?";
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

    @Override
    public List<DailyStatistics> getDailyStatistics(Date startDate, Date endDate) {
         List<DailyStatistics> statisticsList = new ArrayList<>();

        // 计算两个月前的日期
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.MONTH, -2);
        Date calcStartDate = calendar.getTime();
        Date calcEndDate = new Date();
        String sql = "SELECT " +
                "l.lost_date AS date, " +
                "COUNT(l.id) AS lost_count, " +
                "SUM(CASE WHEN f.found_count IS NOT NULL THEN f.found_count ELSE 0 END) AS found_count, " +
                "SUM(CASE WHEN f.found_count IS NOT NULL THEN f.found_count ELSE 0 END) / COUNT(l.id) AS recovery_rate, " +
                "SUM(CASE WHEN f.found_count IS NOT NULL THEN f.found_count ELSE 0 END) / COUNT(l.id) - COALESCE(prev.recovery_rate, 0) AS trend " +
                "FROM lost_items l " +
                "LEFT JOIN ( " +
                "    SELECT lost_item_id, DATE(found_date) AS found_date, COUNT(*) AS found_count " +
                "    FROM found_items " +
                "    WHERE found_date BETWEEN ? AND ? " +
                "    GROUP BY lost_item_id, DATE(found_date) " +
                ") f ON l.id = f.lost_item_id AND DATE(l.lost_date) = f.found_date " +
                "LEFT JOIN ( " +
                "    SELECT " +
                "        DATE(l1.lost_date) AS date, " +
                "        SUM(CASE WHEN f1.found_count IS NOT NULL THEN f1.found_count ELSE 0 END) / COUNT(l1.id) AS recovery_rate " +
                "    FROM lost_items l1 " +
                "    LEFT JOIN ( " +
                "        SELECT lost_item_id, DATE(found_date) AS found_date, COUNT(*) AS found_count " +
                "        FROM found_items " +
                "        WHERE found_date BETWEEN ? AND ? " +
                "        GROUP BY lost_item_id, DATE(found_date) " +
                "    ) f1 ON l1.id = f1.lost_item_id AND DATE(l1.lost_date) = f1.found_date " +
                "    GROUP BY DATE(l1.lost_date) " +
                ") prev ON DATE(l.lost_date) = DATE(DATE_ADD(prev.date, INTERVAL 1 DAY)) " +
                "WHERE l.lost_date BETWEEN ? AND ? " +
                // 关键修改点：GROUP BY 子句补充 l.lost_date 相关字段，保证 SELECT 里非聚合字段都能对应上
                "GROUP BY DATE(l.lost_date), prev.recovery_rate, l.lost_date " +
                "ORDER BY DATE(l.lost_date) DESC " +
                "LIMIT 5";

        // 使用 try-with-resources 自动关闭资源
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            // 设置6个日期参数
            Timestamp startTime = new Timestamp(startDate.getTime());
            Timestamp endTime = new Timestamp(endDate.getTime());

            for (int i = 1; i <= 6; i++) {
                pstmt.setTimestamp(i, i % 2 == 0 ? endTime : startTime);
            }

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    DailyStatistics stats = new DailyStatistics();
                    stats.setDate(rs.getDate("date"));
                    stats.setLostCount(rs.getInt("lost_count"));
                    stats.setFoundCount(rs.getInt("found_count"));
                    stats.setRecoveryRate(rs.getDouble("recovery_rate"));
                    stats.setTrend(rs.getDouble("trend"));
                    statisticsList.add(stats);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            // 记录日志或抛出自定义异常
        }

        return statisticsList;
    }
}

