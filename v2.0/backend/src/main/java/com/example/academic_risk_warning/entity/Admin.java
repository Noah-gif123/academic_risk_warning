package com.example.academic_risk_warning.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * 教务/管理员表
 * <p>对应表：admin</p>
 */
@Data
@TableName("admin")
public class Admin {

    /** 管理员ID */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 账号ID */
    private Long accountId;

    /** 管理员号（登录号） */
    private String adminNo;

    /** 姓名 */
    private String adminName;

    /** 类型：ACADEMIC教务/SYS系统管理员 */
    private String adminType;

    /** 所属部门（教务处等） */
    private String deptName;

    private String phone;

    private String email;

    private String status;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
