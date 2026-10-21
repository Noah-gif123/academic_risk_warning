package com.example.academic_risk_warning.vo;

import lombok.Data;

import java.math.BigDecimal;

/** 一个学生在本教学班的考勤汇总（扣分不落库，按口径实时算） */
@Data
public class AttendanceStudentStatVO {
    private Long studentId;
    private String studentNo;
    private String studentName;
    private Integer presentCount;
    private Integer lateCount;
    private Integer absentCount;
    private Integer leaveCount;
    private Integer calledSessions;
    /** P = 2×缺课 + 1×迟到 + 1×max(0, 请假−3)，无上限 */
    private Integer deductPoints;
    /** N_max = floor(有效课次 / 3) */
    private Integer maxAbsent;
    private Integer remainingAbsent;
    private String thresholdHint;
    private BigDecimal attendanceRate;
}
