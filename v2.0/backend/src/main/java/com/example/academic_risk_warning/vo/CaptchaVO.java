package com.example.academic_risk_warning.vo;

import lombok.Builder;
import lombok.Data;

/** 图形验证码：一次性 id + 图片（data URL）+ 有效期 */
@Data
@Builder
public class CaptchaVO {
    private String captchaId;
    private String image;
    private Integer expireSeconds;
}
