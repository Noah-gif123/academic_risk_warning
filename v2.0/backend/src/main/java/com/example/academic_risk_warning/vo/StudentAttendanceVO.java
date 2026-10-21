package com.example.academic_risk_warning.vo;

import lombok.Data;

import java.time.LocalDate;
import java.util.List;

/** 学生端考勤：明细 + 汇总；按口径**不显示剩余可缺次数**，只在临界时提示「再缺一次即挂」 */
@Data
public class StudentAttendanceVO {
    private Long teachingClassId;
    private String courseName;
    private String teacherName;
    private Integer totalSessions;
    private Integer calledSessions;
    private Integer presentCount;
    private Integer lateCount;
    private Integer absentCount;
    private Integer leaveCount;
    private Integer deductPoints;
    private Integer maxAbsent;
    private String thresholdHint;
    private List<Row> records;

    @Data
    public static class Row {
        private Long id;
        private Integer weekNo;
        private Integer sessionNo;
        private LocalDate sessionDate;
        private String status;
        private String leaveType;
        private String phase;
    }
}
