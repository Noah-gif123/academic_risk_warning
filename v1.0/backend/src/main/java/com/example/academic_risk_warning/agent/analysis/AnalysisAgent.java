package com.example.academic_risk_warning.agent.analysis;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.academic_risk_warning.agent.core.AgentContext;
import com.example.academic_risk_warning.agent.core.BaseAgent;
import com.example.academic_risk_warning.agent.core.JsonUtils;
import com.example.academic_risk_warning.entity.*;
import com.example.academic_risk_warning.llm.LLMClient;
import com.example.academic_risk_warning.mapper.*;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 学习分析智能体：基于监测数据，用 LLM 深度分析问题根源和优先级排序
 */
@Component
public class AnalysisAgent extends BaseAgent<Map<String, Object>> {

    private final AlertRecordMapper alertRecordMapper;
    private final StudentWeakPointMapper weakPointMapper;
    private final KnowledgeMasteryMapper masteryMapper;
    private final AlertSnapshotMapper snapshotMapper;
    private final StudentMapper studentMapper;
    private final CourseKnowledgePointMapper kpMapper;
    private final AnalysisReportMapper analysisReportMapper;

    public AnalysisAgent(LLMClient llmClient,
                         AlertRecordMapper alertRecordMapper,
                         StudentWeakPointMapper weakPointMapper,
                         KnowledgeMasteryMapper masteryMapper,
                         AlertSnapshotMapper snapshotMapper,
                         StudentMapper studentMapper,
                         CourseKnowledgePointMapper kpMapper,
                         AnalysisReportMapper analysisReportMapper) {
        super(llmClient, "AnalysisAgent");
        this.alertRecordMapper = alertRecordMapper;
        this.weakPointMapper = weakPointMapper;
        this.masteryMapper = masteryMapper;
        this.snapshotMapper = snapshotMapper;
        this.studentMapper = studentMapper;
        this.kpMapper = kpMapper;
        this.analysisReportMapper = analysisReportMapper;
    }

    @Override
    public String getSystemPrompt() {
        return """
            你是一位资深的教育学分析专家，擅长从学习数据中发现深层问题模式。
            请基于学生的学情监测数据，做以下分析：
            1. 根本原因分析：为什么会出现这些学习问题？从知识基础、学习方法、学习动力、外部因素等角度分析
            2. 薄弱点归类：将薄弱知识点归类为核心基础弱/期中内容断层/期末复习不足等
            3. 补强优先级排序：哪些知识点最应该优先补强（考虑基础性和考试权重）
            4. 改善建议：给出具体、可操作的改善建议
            请使用简洁专业的中文输出。""";
    }

    @Override
    public String buildUserPrompt(AgentContext ctx) {
        Long studentId = ctx.getStudentId();
        Long courseId = ctx.getCourseId();
        Student student = studentMapper.selectById(studentId);
        String studentName = student != null ? student.getStudentName() : "未知";

        // 从上游 MonitorAgent 注入的数据
        Map<String, Object> monitor = ctx.getMonitorResult();
        StringBuilder sb = new StringBuilder();

        sb.append("学生：").append(studentName).append("（ID=").append(studentId).append("）\n");
        sb.append("课程ID：").append(courseId != null ? courseId : "全部").append("\n\n");

        if (monitor != null && !monitor.isEmpty()) {
            sb.append("=== 上游监测结果 ===\n");
            sb.append("整体摘要：").append(monitor.getOrDefault("summary", "无")).append("\n");
            Object anomalies = monitor.get("anomalyFlags");
            if (anomalies != null) {
                sb.append("异常标记：").append(anomalies.toString()).append("\n");
            }
            sb.append("风险维度：").append(monitor.getOrDefault("riskDimensions", "无")).append("\n");
            sb.append("整体风险等级：").append(monitor.getOrDefault("overallLevel", "未知")).append("\n\n");
        }

        // 1. 薄弱知识点详情
        List<StudentWeakPoint> weakPoints = queryWeakPoints(studentId, courseId);
        sb.append("=== 薄弱知识点（共").append(weakPoints.size()).append("个） ===\n");
        Map<Long, String> kpNameMap = loadKpNames(weakPoints);
        Map<Long, String> kpDescMap = loadKpDescs(weakPoints);
        weakPoints.stream().limit(15).forEach(wp -> {
            String name = kpNameMap.getOrDefault(wp.getKpId(), "未知");
            String desc = kpDescMap.getOrDefault(wp.getKpId(), "");
            sb.append("- ").append(name)
              .append(" | 错误率=").append(wp.getErrorRate()).append("%")
              .append(" | 错误次数=").append(wp.getErrorCount());
            if (!desc.isBlank()) sb.append(" | 描述=").append(desc);
            sb.append("\n");
        });
        sb.append("\n");

        // 2. 知识掌握概况
        KnowledgeMastery mastery = queryMastery(studentId, courseId);
        if (mastery != null) {
            sb.append("=== 知识掌握概况 ===\n");
            sb.append("总做题数=").append(nvl(mastery.getTotalQuestion()))
              .append("，错误数=").append(nvl(mastery.getErrorCount()))
              .append("，总知识点数=").append(nvl(mastery.getTotalKnowledge()))
              .append("，薄弱点数=").append(nvl(mastery.getWeakKnowledgeCount()))
              .append("，基础题/正确=").append(nvl(mastery.getBasicTotal()))
              .append("/").append(nvl(mastery.getBasicCorrect())).append("\n\n");
        }

        // 3. 历史风险趋势
        List<AlertSnapshot> snapshots = querySnapshots(studentId, courseId);
        sb.append("=== 风险趋势 ===\n");
        if (snapshots.isEmpty()) {
            sb.append("暂无历史数据。\n");
        } else {
            sb.append(String.format("%-12s %-8s %-8s %s\n", "日期", "风险分", "等级", "预测成绩"));
            snapshots.forEach(s -> {
                sb.append(String.format("%-12s %-8s %-8s %s\n",
                        s.getSnapshotDate(),
                        nvl(s.getRiskScore()),
                        s.getAlertLevel(),
                        nvl(s.getPredictedScore())));
            });
            // 计算趋势方向
            if (snapshots.size() >= 2) {
                double first = snapshots.get(snapshots.size() - 1).getRiskScore() != null
                        ? snapshots.get(snapshots.size() - 1).getRiskScore().doubleValue() : 0;
                double last = snapshots.get(0).getRiskScore() != null
                        ? snapshots.get(0).getRiskScore().doubleValue() : 0;
                String trend = last > first ? "恶化" : (last < first ? "改善" : "稳定");
                sb.append("趋势方向：").append(trend).append("\n");
            }
        }

        sb.append("\n请按照以下 JSON 格式输出分析结果：\n");
        sb.append("{\n");
        sb.append("  \"weaknessCategory\": \"核心基础弱|知识断层|学习方法问题|动力不足|多因素\",\n");
        sb.append("  \"rootCause\": \"根源分析（300字以内）\",\n");
        sb.append("  \"prioritySequence\": [\n");
        sb.append("    { \"kpName\": \"知识点名\", \"reason\": \"优先原因\", \"urgency\": \"HIGH|MEDIUM|LOW\" }\n");
        sb.append("  ],\n");
        sb.append("  \"improvementSuggestions\": [\"建议1\", \"建议2\", \"建议3\"],\n");
        sb.append("  \"riskForecast\": \"如果不干预，预测接下来会发生什么（100字以内）\",\n");
        sb.append("  \"estimatedRecoveryWeeks\": 预计恢复正常需要周数\n");
        sb.append("}");
        return sb.toString();
    }

    @Override
    protected Map<String, Object> doExecute(AgentContext ctx) {
        Long studentId = ctx.getStudentId();

        // 1. 调用 LLM 进行深度分析
        String llmOutput = callLLM(ctx);
        Map<String, Object> llmResult = JsonUtils.parse(llmOutput);
        if (llmResult.containsKey("raw")) {
            llmResult = Map.of("rootCause", llmOutput);
        }

        // 2. 持久化分析报告
        AnalysisReport report = new AnalysisReport();
        report.setStudentId(studentId);
        report.setCourseId(ctx.getCourseId());
        report.setAnalysisDate(LocalDateTime.now());
        report.setWeaknessCategory(
                Objects.toString(llmResult.getOrDefault("weaknessCategory", ""), ""));
        report.setRootCause(
                Objects.toString(llmResult.getOrDefault("rootCause", ""), ""));
        report.setPrioritySequence(JsonUtils.toJsonString(llmResult.get("prioritySequence")));
        report.setCreateTime(LocalDateTime.now());
        analysisReportMapper.insert(report);

        // 3. 构建返回（用于传递给下一个 Agent）
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("reportId", report.getId());
        result.put("studentId", studentId);
        result.put("courseId", ctx.getCourseId());
        result.put("weaknessCategory", report.getWeaknessCategory());
        result.put("rootCause", report.getRootCause());
        result.put("prioritySequence", llmResult.get("prioritySequence"));
        result.put("improvementSuggestions", llmResult.get("improvementSuggestions"));
        result.put("riskForecast", llmResult.getOrDefault("riskForecast", ""));
        result.put("estimatedRecoveryWeeks", llmResult.getOrDefault("estimatedRecoveryWeeks", 0));
        result.put("rawOutput", llmOutput);
        return result;
    }

    // ===== 数据查询 =====

    private List<StudentWeakPoint> queryWeakPoints(Long studentId, Long courseId) {
        var wrapper = new LambdaQueryWrapper<StudentWeakPoint>()
                .eq(StudentWeakPoint::getStudentId, studentId)
                .orderByDesc(StudentWeakPoint::getErrorRate);
        if (courseId != null) wrapper.eq(StudentWeakPoint::getCourseId, courseId);
        return weakPointMapper.selectList(wrapper);
    }

    private KnowledgeMastery queryMastery(Long studentId, Long courseId) {
        return masteryMapper.selectOne(
                new LambdaQueryWrapper<KnowledgeMastery>()
                        .eq(KnowledgeMastery::getStudentId, studentId)
                        .eq(courseId != null, KnowledgeMastery::getCourseId, courseId)
                        .last("LIMIT 1"));
    }

    private List<AlertSnapshot> querySnapshots(Long studentId, Long courseId) {
        var wrapper = new LambdaQueryWrapper<AlertSnapshot>()
                .eq(AlertSnapshot::getStudentId, studentId)
                .orderByAsc(AlertSnapshot::getSnapshotDate);
        if (courseId != null) wrapper.eq(AlertSnapshot::getCourseId, courseId);
        return snapshotMapper.selectList(wrapper);
    }

    private Map<Long, String> loadKpNames(List<StudentWeakPoint> weakPoints) {
        Set<Long> kpIds = weakPoints.stream().map(StudentWeakPoint::getKpId).collect(Collectors.toSet());
        if (kpIds.isEmpty()) return Collections.emptyMap();
        return kpMapper.selectList(
                new LambdaQueryWrapper<CourseKnowledgePoint>().in(CourseKnowledgePoint::getId, kpIds))
                .stream().collect(Collectors.toMap(CourseKnowledgePoint::getId, CourseKnowledgePoint::getName));
    }

    private Map<Long, String> loadKpDescs(List<StudentWeakPoint> weakPoints) {
        Set<Long> kpIds = weakPoints.stream().map(StudentWeakPoint::getKpId).collect(Collectors.toSet());
        if (kpIds.isEmpty()) return Collections.emptyMap();
        return kpMapper.selectList(
                new LambdaQueryWrapper<CourseKnowledgePoint>()
                        .in(CourseKnowledgePoint::getId, kpIds)
                        .isNotNull(CourseKnowledgePoint::getDescription))
                .stream().filter(kp -> kp.getDescription() != null)
                .collect(Collectors.toMap(CourseKnowledgePoint::getId, CourseKnowledgePoint::getDescription));
    }

    private String nvl(Object v) {
        return v != null ? v.toString() : "N/A";
    }

    @Override
    protected void validate(AgentContext ctx) {
        super.validate(ctx);
        if (ctx.getMonitorResult() == null || ctx.getMonitorResult().isEmpty()) {
            log.info("[AnalysisAgent] 上游监测数据为空，将从数据库重新采集");
        }
    }
}
