package com.example.academic_risk_warning.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;

/**
 * 管理员表实体
 */
@Data
@TableName("admin")
public class Admin {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 管理员账号（登录用） */
    private String adminNo;

    /** 管理员姓名 */
    private String adminName;

    /** 登录密码（接口响应中不返回） */
    @JsonIgnore
    private String password;

    /** 手机号 */
    private String phone;
}
