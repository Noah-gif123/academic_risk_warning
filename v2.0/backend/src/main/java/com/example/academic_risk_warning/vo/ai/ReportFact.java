package com.example.academic_risk_warning.vo.ai;

import lombok.Data;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * 报告事实包（FactPack）：报告里出现的一切数字都来自这里，模型只负责"说人话"。
 *
 * <p>维度一律用系统已有数据：成绩构成项、试卷与周次、题目分组名（group_name）、题型、
 * 考勤、项目贡献、预警结论 —— 不引入知识点。</p>
 */
@Data
public class ReportFact {

    private Long enrollmentId;
    private Long studentId;
    private String studentNo;
    private String studentName;
    private Long teachingClassId;
    private String className;
    private Long offeringId;
    private Long courseId;
    private String courseName;
    private String courseType;
    private Integer credit;
    private Long termId;
    private String termName;
    private String teacherName;
    private Integer plannedWeeks;
    private Integer effectiveSessions;
    private Integer passScore;

    /** 总分与班级对比 */
    private BigDecimal totalScore;
    private BigDecimal weightedScore;
    private BigDecimal deductionPoints;
    private BigDecimal classAvg;
    private Integer classRank;
    private Integer classSize;

    /** 成绩构成分项 */
    private List<Item> items = new ArrayList<>();

    /** 分组维度（question.group_name） */
    private List<Dim> groups = new ArrayList<>();

    /** 题型维度（question.q_type） */
    private List<Dim> types = new ArrayList<>();

    /** 周次趋势 */
    private List<Trend> weeks = new ArrayList<>();

    /** 考勤 */
    private Integer absentCount;
    private Integer lateCount;
    private Integer leaveCount;
    private Integer allowedAbsent;
    private Integer remainingAbsent;

    /** 项目 */
    private List<Project> projects = new ArrayList<>();

    /** 结论 */
    private String riskLevel;
    private String confidenceLevel;
    private String outputMode;
    private String failReason;
    private BigDecimal finalExamNeeded;
    private Boolean hardFail;
    private Boolean nearFail;

    /** 预警事件 */
    private String alertLevel;
    private String alertReason;
    private String alertStatus;
    private String teacherReply;
    private Boolean appealPending;

    /** 一次性把关键数字摊平，供"数字白名单"与前端展示使用 */
    public List<String> keyNumbers() {
        List<String> out = new ArrayList<>();
        add(out, totalScore);
        add(out, weightedScore);
        add(out, deductionPoints);
        add(out, classAvg);
        add(out, finalExamNeeded);
        add(out, passScore);
        add(out, credit);
        add(out, plannedWeeks);
        add(out, effectiveSessions);
        add(out, classRank);
        add(out, classSize);
        add(out, absentCount);
        add(out, lateCount);
        add(out, leaveCount);
        add(out, allowedAbsent);
        add(out, remainingAbsent);
        return out;
    }

    private void add(List<String> out, Object v) {
        if (v != null) {
            out.add(String.valueOf(v));
        }
    }

    /** 成绩构成分项 */
    @Data
    public static class Item {
        private String name;
        private String path;
        private BigDecimal score;
        private BigDecimal weight;
        private BigDecimal rate;
        private Boolean missing;
    }

    /** 通用维度（分组 / 题型） */
    @Data
    public static class Dim {
        private String name;
        private BigDecimal myRate;
        private BigDecimal classRate;
        private BigDecimal gap;
        private Integer questionCount;
    }

    /** 周次趋势 */
    @Data
    public static class Trend {
        private Integer week;
        private BigDecimal myRate;
        private BigDecimal classRate;
    }

    /** 项目 */
    @Data
    public static class Project {
        private String projectName;
        private BigDecimal teamScore;
        private BigDecimal ratio;
        private String submitStatus;
    }
}
