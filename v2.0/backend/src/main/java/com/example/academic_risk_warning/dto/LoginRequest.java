package com.example.academic_risk_warning.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/** 登录请求：四端共用（账号为学号 / 工号 / 教务账号，默认密码 000000） */
@Data
public class LoginRequest {
    @NotBlank(message = "登录号不能为空")
    private String loginNo;

    @NotBlank(message = "密码不能为空")
    private String password;

    /** 图形验证码 id（GET /api/auth/captcha 获取） */
    private String captchaId;

    /** 用户输入的验证码 */
    private String captchaCode;
}
