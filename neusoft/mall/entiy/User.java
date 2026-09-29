package cn.edu.neusoft.mall.entiy;

import java.util.Date;

public class User {
    private int id;
    private String username;
    private String password;
    private String email;

    private String phone;
    private String role;
    private Date create_time;
    private Date update_time;
    private String avatar;
    private String passwordSecret;

    public String getPasswordSecret() {
        return passwordSecret;
    }

    public void setPasswordSecret(String passwordSecret) {
        this.passwordSecret = passwordSecret;
    }

    public String getAvatar() {
        return avatar;
    }

    public void setAvatar(String avatar) {
        this.avatar = avatar;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public Date getCreate_time() {
        return create_time;
    }

    public void setCreate_time(Date create_time) {
        this.create_time = create_time;
    }

    public Date getUpdate_time() {
        return update_time;
    }
    private String passwordConfirm;
    public void setUpdate_time(Date update_time) {
        this.update_time = update_time;
    }
    public String getPasswordConfirm() {
        return passwordConfirm;
    }

    public void setPasswordConfirm(String passwordConfirm) {
        this.passwordConfirm = passwordConfirm;
    }

    @Override
    public String toString() {
        return "User [id=" + id + ", create_time=" + create_time + ", number=" + username + ", passwordSecret=" + passwordSecret
                + ", email=" + email + ",avatar=" + avatar + ", phone=" + phone + " ] ";
    }
}
