package com.example.academic_risk_warning.vo;

import lombok.Builder;
import lombok.Data;

/** 登录返回：令牌 + 角色 + 归属 + 是否首次登录 */
@Data
@Builder
public class LoginVO {
    private String token;
    private String role;
    private String loginNo;
    private String realName;
    private Long refId;
    private boolean firstLogin;
}
