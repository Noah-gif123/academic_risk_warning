package com.example.academic_risk_warning.vo;

import lombok.Data;

import java.util.List;

/** 生成结果摘要 */
@Data
public class SyntheticResultVO {
    private String termCode;
    private String termName;
    private Long termId;
    private Integer students;
    private Integer courses;
    private Integer offerings;
    private Integer teachingClasses;
    private Integer enrollments;
    private Integer sessions;
    private Integer attendanceRecords;
    private Integer papers;
    private Integer submissions;
    private Integer scoreItems;
    private Integer scoreResults;
    private Integer teacherStyles;
    private Long elapsedMs;
    private List<String> notes;
}
