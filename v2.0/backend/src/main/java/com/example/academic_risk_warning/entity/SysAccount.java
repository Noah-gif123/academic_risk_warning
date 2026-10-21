package com.example.academic_risk_warning.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * 统一账号表（四端登录）
 * <p>对应表：sys_account</p>
 */
@Data
@TableName("sys_account")
public class SysAccount {

    /** 账号ID */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 登录号（学号/工号/管理员号，全库唯一） */
    private String loginNo;

    /** 密码哈希（BCrypt） */
    private String passwordHash;

    /** 角色：STUDENT/TEACHER/COUNSELOR/ADMIN */
    private String role;

    /** 姓名（冗余展示用，可与档案表同步） */
    private String realName;

    /** 状态：ACTIVE正常/DISABLED停用/LOCKED锁定 */
    private String status;

    /** 当前登录令牌（单点登录；NULL 表示未登录） */
    private String token;

    /** 令牌过期时间 */
    private LocalDateTime tokenExpireTime;

    /** 最近改密时间 */
    private LocalDateTime pwdUpdateTime;

    /** 最近登录时间 */
    private LocalDateTime lastLoginTime;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
