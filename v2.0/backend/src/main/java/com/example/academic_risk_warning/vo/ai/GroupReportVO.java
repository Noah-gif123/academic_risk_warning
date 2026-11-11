package com.example.academic_risk_warning.vo.ai;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** 群体报告展示对象（教学班 / 行政班共用） */
@Data
public class GroupReportVO {

    private Long id;
    private String scope;
    private Long targetId;
    private Long teachingClassId;
    private Long adminClassId;
    private String title;
    private String summary;
    private List<Section> sections = new ArrayList<>();
    private List<Map<String, Object>> actions = new ArrayList<>();
    /** DIFY / LLM / TEMPLATE */
    private String source;
    private String createTime;

    /** Q2：报告生成后班级数据又被重判过 → 结论过期，前端应提示重新生成 */
    private Boolean stale = false;
    /** 过期原因（整句，前端直接展示） */
    private String staleReason;

    /** 数字对账：正文里抽到的关键数字与事实包的比对结果 */
    private Integer checksTotal = 0;
    private Integer checksPassed = 0;
    private List<Map<String, Object>> checks = new ArrayList<>();

    /** 报告点名的学生（前端做成可点人员条，跳学生详情） */
    private List<Map<String, Object>> persons = new ArrayList<>();

    /** 生成该报告的 Agent 运行记录；历史报告可能为空 */
    private Long runId;
    private String runStatus;
    private List<RunStep> runSteps = new ArrayList<>();

    @Data
    public static class RunStep {
        private Integer stepNo;
        private String name;
        private String stepRole;
        private String toolName;
        private String validationStatus;
        private String gateStatus;
        private Integer durationMs;
        private String error;
        private String createTime;
    }

    @Data
    public static class Section {
        private String title;
        private String text;
    }
}
