package com.example.academic_risk_warning.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 学生登录响应
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StudentLoginVO {

    private Long studentId;
    private String studentNo;
    private String studentName;
    private String grade;
    private String className;
    /** 后续请求放在 Header: Authorization: Bearer {token} */
    private String token;
}
