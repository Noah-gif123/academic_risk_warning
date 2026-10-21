package com.example.academic_risk_warning.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/** 导员按行政班发布「教师风格判定」问卷 */
@Data
public class QuestionnaireSaveRequest {
    @NotNull(message = "行政班不能为空")
    private Long adminClassId;
    private String title;
    private Long offeringId;
    private Long courseId;
}
