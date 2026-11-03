package com.example.academic_risk_warning.vo.ai;

import lombok.Data;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * 课程（开课）事实包：一名教师名下、同一门课在本学期的**全部教学班**。
 *
 * <p>三份群体报告的口径差异：</p>
 * <ul>
 *   <li>教学班（CLASS）：一门课 × 一个班 —— 看本班薄弱分组与重点关注名单；</li>
 *   <li>行政班（ADMIN_CLASS）：一个班 × 多门课 —— 看集中风险课程与多门预警学生；</li>
 *   <li>课程（OFFERING）：一门课 × 多个班 —— 看<b>班与班的横向对比</b>与共同薄弱项。</li>
 * </ul>
 *
 * <p>数字全部由 {@link com.example.academic_risk_warning.service.ai.GroupFactBuilder}
 * 从各教学班的现有事实包聚合而来（人数加权），模型只负责写结论。</p>
 */
@Data
public class OfferingFact {

    /** 课程本身 */
    private Long offeringId;
    private Long courseId;
    private String courseName;
    private String courseType;
    private Integer credit;
    private Long termId;
    private String termName;
    private Long teacherId;
    private String teacherName;
    private Integer plannedWeeks;
    private Integer passScore;

    /** 规模：教学班数与合计人数（以选课记录计） */
    private Integer classCount;
    private Integer studentCount;

    /** 风险分布（按选课记录汇总） */
    private Integer redCount;
    private Integer yellowCount;
    private Integer greenCount;
    private Integer unknownCount;
    private Integer hardFailCount;
    /** 红灯占选课记录的比例（百分比，保留 1 位） */
    private BigDecimal failRate;
    private BigDecimal avgScore;
    private BigDecimal minScore;
    private BigDecimal maxScore;

    /** 考勤与事件 */
    private Integer absentOverLimitCount;
    private Integer activeAlertCount;
    private Integer ungradedItemCount;

    /** 全课程共性难点（各教学班按题量加权） */
    private List<Dim> weakGroups = new ArrayList<>();
    private List<Dim> weakTypes = new ArrayList<>();

    /** 教学班横向对比（红黄灯多的排前面） */
    private List<ClassRow> classes = new ArrayList<>();

    /** 重点关注名单（跨班取红灯优先，最多 10 人） */
    private List<Student> watchList = new ArrayList<>();

    /** 关键数字摊平，供"数字白名单"回验 */
    public List<String> keyNumbers() {
        List<String> out = new ArrayList<>();
        add(out, classCount);
        add(out, studentCount);
        add(out, redCount);
        add(out, yellowCount);
        add(out, greenCount);
        add(out, unknownCount);
        add(out, hardFailCount);
        add(out, failRate);
        add(out, avgScore);
        add(out, minScore);
        add(out, maxScore);
        add(out, absentOverLimitCount);
        add(out, activeAlertCount);
        add(out, ungradedItemCount);
        add(out, passScore);
        add(out, credit);
        add(out, plannedWeeks);
        return out;
    }

    private void add(List<String> out, Object v) {
        if (v != null) {
            out.add(String.valueOf(v));
        }
    }

    @Data
    public static class Dim {
        private String name;
        private BigDecimal rate;
        private Integer questionCount;
    }

    @Data
    public static class ClassRow {
        private Long teachingClassId;
        private String classCode;
        private String className;
        private Integer studentCount;
        private Integer red;
        private Integer yellow;
        private Integer green;
        private Integer hardFail;
        private BigDecimal avgScore;
        private BigDecimal minScore;
        private BigDecimal maxScore;
        private Integer absentOverLimitCount;
        private Integer activeAlertCount;
    }

    @Data
    public static class Student {
        private Long teachingClassId;
        private String className;
        private String studentName;
        private String riskLevel;
        private BigDecimal totalScore;
        private String reason;
    }
}
