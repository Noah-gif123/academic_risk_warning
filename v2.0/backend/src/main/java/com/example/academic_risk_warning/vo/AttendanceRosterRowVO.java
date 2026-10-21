package com.example.academic_risk_warning.vo;

import lombok.Data;

/** 点名单一行：学生 + 当前状态（未点名时默认出勤） */
@Data
public class AttendanceRosterRowVO {
    private Long enrollmentId;
    private Long studentId;
    private String studentNo;
    private String studentName;
    private String status;
    private String leaveType;
    private Long recordId;
    private Boolean archived;
    private Integer absentTotal;
    private String thresholdHint;
}
