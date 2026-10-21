package com.example.academic_risk_warning.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/** 学生单科详情 + 处理记录 */
@Data
public class CounselorStudentDetailVO {
    private Long studentId;
    private String studentNo;
    private String studentName;
    private String majorName;
    private String adminClassName;
    private Integer enrollYear;
    private String phone;
    private List<CourseRow> courses;
    private List<InterventionRow> interventions;

    @Data
    public static class CourseRow {
        private Long teachingClassId;
        private String courseName;
        private String teacherName;
        private Integer effectiveSessions;
        private Integer allowedAbsent;
        private Integer absentCount;
        private Integer lateCount;
        private Integer leaveCount;
        private Integer deductPoints;
        private BigDecimal weightedScore;
        private BigDecimal totalScore;
        private Integer passScore;
        private Boolean hardFail;
        private Boolean fail;
        private String failReasonText;
        private String scoreStatus;
        private String riskLevel;
        private String riskHint;
    }

    @Data
    public static class InterventionRow {
        private Long id;
        private String actionType;
        private String actionTypeText;
        private String content;
        private String result;
        private String effective;
        private String effectiveText;
        private String operatorRole;
        private String referStatus;
        private String referNote;
        private String adminFeedback;
        private LocalDateTime createTime;
    }
}
