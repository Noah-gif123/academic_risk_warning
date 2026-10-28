package com.example.academic_risk_warning.vo.ai;

import lombok.Data;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * 行政班事实包：一名辅导员视角，跨课程看同一个班的学生。
 *
 * <p>与教学班报告的区别：教学班是"一门课 × 一个班"，行政班是"一个班 × 多门课"。
 * 所以这里的风险分布是按**选课记录**统计（一个学生多门课会有多条），
 * 另外单列"多门预警学生"与"集中风险课程"。</p>
 */
@Data
public class AdminClassFact {

    private Long adminClassId;
    private String classCode;
    private String className;
    private Integer gradeYear;
    private String majorName;
    private Long counselorId;
    private String counselorName;
    private Long termId;
    private String termName;

    /** 规模 */
    private Integer studentCount;
    private Integer courseCount;
    private Integer enrollmentCount;

    /** 风险分布（按选课记录） */
    private Integer redCount;
    private Integer yellowCount;
    private Integer greenCount;
    private Integer hardFailCount;
    /** 挂科风险率（红 / 选课数，百分比，保留 1 位） */
    private BigDecimal failRate;
    private BigDecimal avgScore;

    /** 集中风险课程 Top3 */
    private List<CourseRisk> courseRisks = new ArrayList<>();

    /** 多门预警学生（≥2 门红/黄） */
    private List<Student> multiRiskStudents = new ArrayList<>();

    /** 未覆盖/数据不足的选课数（无 risk_state） */
    private Integer unknownCount;
    private Integer activeAlertCount;

    public List<String> keyNumbers() {
        List<String> out = new ArrayList<>();
        add(out, studentCount);
        add(out, courseCount);
        add(out, enrollmentCount);
        add(out, redCount);
        add(out, yellowCount);
        add(out, greenCount);
        add(out, hardFailCount);
        add(out, failRate);
        add(out, avgScore);
        add(out, unknownCount);
        add(out, activeAlertCount);
        add(out, gradeYear);
        return out;
    }

    private void add(List<String> out, Object v) {
        if (v != null) {
            out.add(String.valueOf(v));
        }
    }

    @Data
    public static class CourseRisk {
        private Long courseId;
        private String courseName;
        private Integer red;
        private Integer yellow;
        private Integer total;
        private BigDecimal failRate;
    }

    @Data
    public static class Student {
        private Long studentId;
        private String studentNo;
        private String studentName;
        private Integer riskCount;
        private String riskCourses;
        private BigDecimal minScore;
    }
}
