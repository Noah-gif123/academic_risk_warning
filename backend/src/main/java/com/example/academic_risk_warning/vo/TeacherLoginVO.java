package com.example.academic_risk_warning.vo;

import lombok.Builder;
import lombok.Data;

/**
 * 教师登录响应
 */
@Data
@Builder
public class TeacherLoginVO {

    private Long teacherId;
    private String teacherNo;
    private String teacherName;
    /** 后续请求放在 Header: Authorization: Bearer {token} */
    private String token;
}
