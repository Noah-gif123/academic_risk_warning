package com.example.academic_risk_warning.vo.ai;

import lombok.Data;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** 学生端「学习评价报告」展示对象 */
@Data
public class StudentReportVO {

    private Long id;
    private Long enrollmentId;
    private Long teachingClassId;
    private Long offeringId;
    private String courseName;
    private String className;
    private String teacherName;
    private String termName;
    private String reportType;
    private String scope;
    private String riskLevel;
    private BigDecimal totalScore;
    private String summary;
    private List<Section> sections = new ArrayList<>();
    private List<Map<String, Object>> actions = new ArrayList<>();
    /** DIFY / TEMPLATE，前端展示"AI 生成"或"规则生成" */
    private String source;
    private Boolean read;
    private Boolean useful;
    private String createTime;

    @Data
    public static class Section {
        private String title;
        private String text;
    }
}
