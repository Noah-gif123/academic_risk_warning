package com.example.academic_risk_warning.dto;

import lombok.Data;

/** 调整课时（目录一行） */
@Data
public class LessonUpdateRequest {
    private String title;
    private Integer sortNo;
    private String status;
}
