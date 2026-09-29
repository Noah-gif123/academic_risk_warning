package cn.edu.neusoft.framework.db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DBUtil {
    private static final String URL = "jdbc:mysql://192.168.56.102:63306/lost_thing?serverTimezone=Asia/Shanghai&useUnicode=true&characterEncoding=utf8";
    private static final String USER = "test";
    private static final String PASSWORD = "123456";

    // 静态代码块：确保驱动只加载一次
    static {
        try {
            // 加载 MySQL 8.0+ 驱动类
            Class.forName("com.mysql.cj.jdbc.Driver");
            System.out.println("MySQL JDBC 驱动加载成功");
        } catch (ClassNotFoundException e) {
            System.err.println("驱动加载失败：请检查 mysql-connector-java 依赖是否正确");
            e.printStackTrace();
        }
    }

    // 获取数据库连接
    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }

    // 测试连接（可选）
    public static void main(String[] args) {
        try (Connection conn = getConnection()) {
            System.out.println("数据库连接成功！");
        } catch (SQLException e) {
            System.err.println("连接失败：" + e.getMessage());
            e.printStackTrace();
        }
    }
}
