package com.example.academic_risk_warning.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;

/**
 * 教师表实体
 */
@Data
@TableName("teacher")
public class Teacher {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 工号 */
    private String teacherNo;

    /** 登录密码（接口响应中不返回） */
    @JsonIgnore
    private String password;

    /** 姓名 */
    private String teacherName;

    /** 手机号 */
    private String phone;

    /** 邮箱 */
    private String email;
}
