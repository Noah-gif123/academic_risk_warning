package com.example.academic_risk_warning.security;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** 登录用户（来自令牌）：角色 + 归属，用于限定数据范围 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoginUser {
    /** 账号ID（sys_account.id） */
    private Long accountId;
    /** 登录号（学号 / 工号 / 教务账号） */
    private String loginNo;
    /** 角色：STUDENT / TEACHER / COUNSELOR / ADMIN */
    private String role;
    /** 归属ID：student.id / teacher.id / counselor.id / admin.id */
    private Long refId;
    /** 姓名 */
    private String realName;
}
