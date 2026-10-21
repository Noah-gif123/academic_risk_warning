package com.example.academic_risk_warning.dto;

import lombok.Data;

/** 两次处理无效后上报教务 / 终止干预 */
@Data
public class ReferRequest {
    private String note;
    private String reason;
}
