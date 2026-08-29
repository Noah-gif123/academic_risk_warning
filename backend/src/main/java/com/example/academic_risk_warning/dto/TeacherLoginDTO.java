package com.example.academic_risk_warning.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 教师登录请求
 */
@Data
public class TeacherLoginDTO {

    @NotBlank(message = "工号不能为空")
    private String teacherNo;

    @NotBlank(message = "密码不能为空")
    private String password;
}
