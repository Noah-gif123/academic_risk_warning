package com.example.academic_risk_warning.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 学生端 · 我的学情预警（第二阶段第 4 步）。
 *
 * <p>口径约束：学生端<b>只给结论与建议</b>，不给"剩余可缺次数"这类可被钻空子的具体额度，
 * 也不展示教师的处置记录（内部工作记录）。</p>
 */
@Data
public class StudentWarningVO {

    private String studentNo;
    private String studentName;
    private String termName;
    private LocalDateTime lastEvalTime;
    private String engineVersion;

    /** 各等级门数与待核实申诉数 */
    private Summary summary;

    /** 每门课一条（没有预警的课也会出现，等级为 GREEN） */
    private List<Item> items;

    /** 总体建议（跨课程，如"两门课同时预警"） */
    private List<String> advice;

    /** 判定口径说明（学生可见的解释） */
    private List<String> guidelines;

    @Data
    public static class Summary {
        private int total;
        private int red;
        private int orange;
        private int yellow;
        private int green;
        /** 已提交申诉、等老师核实的门数 */
        private int appealed;
    }

    @Data
    public static class Item {
        private Long enrollmentId;
        private Long offeringId;
        private Long teachingClassId;
        /** 有预警才有 alertId，用于查看详情与申诉 */
        private Long alertId;
        private String courseName;
        private String courseType;
        private String classCode;
        private String teacherName;

        private String level;
        private BigDecimal riskScore;
        private BigDecimal confidence;
        private String confidenceLevel;
        private String outputMode;
        private String activeChannel;
        private String triggerRule;
        /** 一句话结论（学生可读） */
        private String conclusion;
        /** 判定依据（原始描述） */
        private String reason;

        private Integer absentCount;
        private Integer lateCount;
        private Integer leaveCount;
        private Integer allowedAbsent;
        private Boolean hardFail;
        private Boolean finalUnreachable;
        private BigDecimal finalExamNeeded;
        private BigDecimal channelBScore;
        private Integer channelBRank;
        private Integer channelBClassSize;

        private Integer effectiveSessions;
        private BigDecimal totalScore;
        private BigDecimal passScore;
        private BigDecimal finalRatio;

        private String status;
        private String studentResponse;
        private LocalDateTime studentResponseTime;
        /** 教师对申诉的回复（学生端可见） */
        private String teacherReply;
        private LocalDateTime teacherReplyTime;
        private LocalDateTime lastEvalTime;

        /** 这门课的具体建议 */
        private List<String> advice;
    }

    /** 详情：课程结论 + 时间线（只含学生该看的节点）+ 建议与口径 */
    @Data
    public static class Detail {
        private Item item;
        private List<TimelineItem> timeline;
        private List<String> advice;
        private List<String> guidelines;
    }

    @Data
    public static class TimelineItem {
        private String eventType;
        private String label;
        private LocalDateTime eventTime;
    }
}
