package com.example.academic_risk_warning.vo;

import lombok.Builder;
import lombok.Data;

/**
 * 管理员登录响应
 */
@Data
@Builder
public class AdminLoginVO {
    private Long adminId;
    private String adminNo;
    private String adminName;
    private String token;
}
