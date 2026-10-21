package com.example.academic_risk_warning.vo;

import lombok.Data;

/** 教学班视图 */
@Data
public class TeachingClassVO {
    private Long id;
    private Long offeringId;
    private Long teacherId;
    private String teacherName;
    private String teacherNo;
    private String classCode;
    private String className;
    private Integer capacity;
    private Integer isRetakeClass;
    private String inviteCode;
    private Integer joinReviewEnabled;
    private String status;
    private Integer studentCount;
    private Integer sessionCount;
    private String adminClasses;
}
