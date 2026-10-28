package com.example.academic_risk_warning.vo.ai;

import lombok.Data;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * 教学班（授课班）事实包：一门课、一个班、一名教师。
 *
 * <p>口径全部来自已有表：risk_state（红黄绿）、score_result（总分与考勤）、
 * submission_answer（分组/题型正确率）、score_item（手工登分项）、alert_record（预警事件）。
 * **不引入知识点**：薄弱项用课程分组（question.group_name）与题型表达。</p>
 */
@Data
public class ClassFact {

    private Long teachingClassId;
    private String classCode;
    private String className;
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

    /** 人数与风险分布 */
    private Integer studentCount;
    private Integer redCount;
    private Integer yellowCount;
    private Integer greenCount;
    private Integer unknownCount;
    private Integer hardFailCount;
    private BigDecimal avgScore;
    private BigDecimal minScore;
    private BigDecimal maxScore;

    /** 考勤 */
    private Integer absentOverLimitCount;
    private BigDecimal avgAbsent;
    private Integer maxAbsent;
    private Integer lateTotal;

    /** 共性难点：分组（group_name）与题型 */
    private List<Dim> weakGroups = new ArrayList<>();
    private List<Dim> weakTypes = new ArrayList<>();

    /** 手工登分项的低分项 */
    private List<Item> weakItems = new ArrayList<>();

    /** 未登分项数量（成绩还没齐） */
    private Integer ungradedItemCount;

    /** 预警事件 */
    private Integer activeAlertCount;

    /** 重点关注名单（红灯优先，最多 10 人） */
    private List<Student> watchList = new ArrayList<>();

    /** 关键数字摊平，供"数字白名单"回验 */
    public List<String> keyNumbers() {
        List<String> out = new ArrayList<>();
        add(out, studentCount);
        add(out, redCount);
        add(out, yellowCount);
        add(out, greenCount);
        add(out, hardFailCount);
        add(out, unknownCount);
        add(out, avgScore);
        add(out, minScore);
        add(out, maxScore);
        add(out, absentOverLimitCount);
        add(out, avgAbsent);
        add(out, maxAbsent);
        add(out, lateTotal);
        add(out, ungradedItemCount);
        add(out, activeAlertCount);
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
    public static class Item {
        private String name;
        private BigDecimal avgScore;
        private Integer sampleCount;
    }

    @Data
    public static class Student {
        private Long studentId;
        private String studentNo;
        private String studentName;
        private String riskLevel;
        private BigDecimal totalScore;
        private Integer absentCount;
        private Integer allowedAbsent;
        private String reason;
    }
}
