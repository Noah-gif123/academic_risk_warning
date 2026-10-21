package com.example.academic_risk_warning.vo;

import lombok.Data;

import java.math.BigDecimal;

/** 点名覆盖率（教务督办用）：低于 1/3 判低置信度并催办 */
@Data
public class CoverageVO {
    private Long teachingClassId;
    private String classCode;
    private String className;
    private String courseName;
    private String teacherName;
    private Integer effectiveSessions;
    private Integer passedSessions;
    private Integer calledSessions;
    private BigDecimal coverageRate;
    private Boolean lowCoverage;
    private Integer archivePending;
}
