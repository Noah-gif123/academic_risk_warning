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

    /** 本次详情按哪个学期取的（默认当前学期）；null 表示未指定且系统也没有当前学期 */
    private Long termId;
    private String termName;
    /** 本学期课程数，等于 courses.size()，给前端直接展示用 */
    private Integer courseCount;

    private List<CourseRow> courses;
    private List<InterventionRow> interventions;

    @Data
    public static class CourseRow {
        private Long teachingClassId;
        private String courseName;
        private String teacherName;
        /** 课程所属学期：与导员端看板口径一致，避免"同一页两个数字对不上" */
        private Long termId;
        private String termName;
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
