package com.example.academic_risk_warning.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/** 导员按行政班发布问卷（评教 / 满意度 / 心理健康 / 毕业生去向 / 学情调查） */
@Data
public class QuestionnaireSaveRequest {
    @NotNull(message = "行政班不能为空")
    private Long adminClassId;
    private String title;
    private Long offeringId;
    private Long courseId;
    /**
     * 问卷类型：TEACHER_STYLE（默认）/ SATISFACTION / MENTAL_HEALTH /
     * GRADUATE_SURVEY / NATIONAL_SURVEY，见 common.QuestionnaireTypes
     */
    private String questionnaireType;
}
