package com.example.academic_risk_warning.vo;

import lombok.Data;

import java.time.LocalDate;

/** 点名用的课次行 */
@Data
public class AttendanceSessionVO {
    private Long sessionId;
    private Integer weekNo;
    private Integer sessionNo;
    private LocalDate sessionDate;
    private String sessionType;
    private String status;
    private String rollCallStatus;
    private Integer rollCallCount;
    private Integer totalStudents;
    private Integer presentCount;
    private Integer lateCount;
    private Integer absentCount;
    private Integer leaveCount;
    private Boolean archived;
    private String phase;
}
