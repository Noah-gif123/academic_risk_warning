package com.example.academic_risk_warning.dto;

import lombok.Data;

/** 作废资源文件（逻辑删除，保留引用痕迹） */
@Data
public class ResourceVoidRequest {
    private String reason;
}
