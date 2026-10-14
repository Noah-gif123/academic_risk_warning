package com.example.academic_risk_warning.vo;

import lombok.Data;

@Data
public class HistoryRiskVO {

    private Long id;
    private Long courseId;
    private String courseName;
    /** 年级：大一/大二/大三 */
    private String gradeLevel;
    private String lastTermFailed;
    private String studyStable;

    /** 历史风险汇总描述，如"无挂科，学习稳定" */
    private String summary;
}
