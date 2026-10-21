package com.example.academic_risk_warning.vo;

import lombok.Data;

import java.time.LocalDateTime;

/** 入班申请（学生端看自己的、教师端看本班的） */
@Data
public class JoinRequestVO {
    private Long id;
    private Long teachingClassId;
    private String courseName;
    private String className;
    private String teacherName;
    private Long studentId;
    private String studentNo;
    private String studentName;
    private String inviteCode;
    private String status;
    private String statusText;
    private LocalDateTime applyTime;
    private LocalDateTime reviewTime;
    private String reviewNote;
    private Boolean notifyAdmin;
}
