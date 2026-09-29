package cn.edu.neusoft.mall.dao.impl;

import cn.edu.neusoft.framework.db.JDBCTemplate;
import cn.edu.neusoft.framework.db.RowMapper;
import cn.edu.neusoft.mall.dao.UserDao;
import cn.edu.neusoft.mall.entiy.User;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;
import java.util.List;

public class UserDaoImpl implements UserDao {
    RowMapper rowMapper = new RowMapper() {
        @Override
        public Object mapRow(ResultSet rs, int rowNum) throws SQLException {
            User user = new User();
            user.setId(rs.getInt("id"));
            user.setUsername(rs.getNString("username"));
            user.setPassword(rs.getNString("password_secret"));
            user.setEmail(rs.getNString("email"));
            user.setPhone(rs.getNString("phone"));
            user.setRole(rs.getNString("role"));
            user.setCreate_time(rs.getDate("create_time"));
            user.setUpdate_time(rs.getDate("update_time"));
            user.setPasswordSecret(rs.getNString("password_secret"));
            user.setAvatar(rs.getNString("avatar"));
            return user;
        }
    };

    @Override
    public User gerUserByNumber(String Number) {
        String sql = "select * from user where username=?";
        return JDBCTemplate.query(sql, rowMapper, Number).size()>0? (User) JDBCTemplate.query(sql, rowMapper, Number).get(0):null;
    }

    @Override
    public User selectUserByName(String userName) {
        String sql = "select * from user where username=?";
        List<User> users = JDBCTemplate.query(sql, rowMapper, new String[]{userName});
        if (users.size() > 0) {
            return users.get(0);
        } else {
            return null;
        }
    }

    @Override
    public int insert(User row) {
        String sql = "INSERT INTO user(id, create_time, username, password_secret, phone, email, avatar) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?)";

        return JDBCTemplate.update(
                sql,
                row.getId(),
                row.getCreate_time(),
                row.getUsername(),
                row.getPasswordSecret(),
                row.getPhone(),
                row.getEmail(),
                row.getAvatar()
        );
    }
    }

