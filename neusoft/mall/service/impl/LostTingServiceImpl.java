package cn.edu.neusoft.mall.service.impl;

import cn.edu.neusoft.framework.db.DBUtil;
import cn.edu.neusoft.mall.dao.LostTingDao;
import cn.edu.neusoft.mall.dao.impl.LostTingDaoImpl;
import cn.edu.neusoft.mall.entiy.LostTing;
import cn.edu.neusoft.mall.service.LostTingService;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static java.sql.DriverManager.getConnection;

public class LostTingServiceImpl implements LostTingService {

    LostTingDao lostTingDao = new LostTingDaoImpl();

    @Override
    public List<LostTing> getlostTingList() {
        List<LostTing> lostTingList = lostTingDao.getLostTingList();
        if (lostTingList.size() > 0) {
            List<LostTing> lostT = lostTingList.stream().map(lostTing -> {
                return lostTing;
            }).collect(Collectors.toList());
            return lostT;
        }
        return lostTingDao.getLostTingList();
    }

    @Override
    public List<LostTing> getLostTingListByName(String keyword) {
        List<LostTing> lostTingList = lostTingDao.getLostTingListByName(keyword);
        return lostTingList;
    }

    @Override
    public LostTing getById(int id) {
        List<LostTing> list = lostTingDao.getLostTingById(id);
        // 从列表中获取第一个元素（ID唯一时应为唯一结果）
        return list != null && !list.isEmpty() ? list.get(0) : null;
    }

    @Override
    public int insertLostTing(LostTing lostTing) {
        return lostTingDao.insertLostTing(lostTing);
    }
    @Override
    public Map<String, Object> publishItem(LostTing lostTing, Integer userId) {
        Map<String, Object> result = new HashMap<>();

        try {
            // 检查用户是否登录
            if (userId == null) {
                result.put("success", false);
                result.put("message", "请先登录");
                return result;
            }

            // 设置用户ID
            lostTing.setUser_id(userId);

            // 设置时间
            Date now = new Date();
            lostTing.setCreate_time(now);
            lostTing.setUpdate_time(now);

            System.out.println("Service 准备插入数据: " + lostTing);

            // 插入数据
            int insertResult = lostTingDao.insertLostTing(lostTing);

            if (insertResult > 0) {
                result.put("success", true);
                result.put("message", "发布成功");
            } else {
                result.put("success", false);
                result.put("message", "发布失败，请重试");
            }
        } catch (Exception e) {
            System.out.println("Service 异常: " + e.getMessage());
            result.put("success", false);
            result.put("message", "发布过程中发生错误：" + e.getMessage());
            e.printStackTrace();
        }

        return result;
    }

    @Override
    public double getRecoveryRate() {
        int lostCount = lostTingDao.getLostCount();
        int foundCount = lostTingDao.getFoundCount();
        if (lostCount == 0) {
            return 0;
        }
        return (double) foundCount / lostCount;
    }

    @Override
    public List<LostTing> getLostTingListByUserId(int userId) {
            return lostTingDao.getLostTingListByUserId(userId);
        }

    @Override
    public boolean updateStatus(int itemId, String status) {
        String sql = "UPDATE item SET status = ? WHERE id = ?";
        try (Connection conn = DBUtil.getConnection(); // 获取数据库连接
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, status);
            pstmt.setInt(2, itemId);
            int rows = pstmt.executeUpdate();
            return rows > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }


}

