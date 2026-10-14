package com.example.academic_risk_warning.agent.monitor;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.academic_risk_warning.agent.core.AgentContext;
import com.example.academic_risk_warning.agent.core.BaseAgent;
import com.example.academic_risk_warning.agent.core.JsonUtils;
import com.example.academic_risk_warning.entity.*;
import com.example.academic_risk_warning.llm.LLMClient;
import com.example.academic_risk_warning.mapper.*;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 学情监测智能体：采集多维数据 + LLM 异常识别 + 生成监测报告
 */
@Component
public class MonitorAgent extends BaseAgent<Map<String, Object>> {

    /** 数据采集线程池（并行查询5个维度数据） */
    private static final ExecutorService DATA_EXECUTOR = Executors.newFixedThreadPool(5, r -> {
        Thread t = new Thread(r, "monitor-data-");
        t.setDaemon(true);
        return t;
    });

    /**
     * 并行采集的多维数据容器
     */
    public static class MonitorData {
        List<AlertRecord> alerts;
        List<StudentWeakPoint> weakPoints;
        List<KnowledgeMastery> masteries;
        List<ExerciseRecommendation> recentRecommendations;
        List<AlertSnapshot> snapshots;
        Map<Long, String> kpNameMap;
    }

    private final AlertRecordMapper alertRecordMapper;
    private final StudentWeakPointMapper weakPointMapper;
    private final KnowledgeMasteryMapper masteryMapper;
    private final ExerciseRecommendationMapper recommendMapper;
    private final AlertSnapshotMapper snapshotMapper;
    private final StudentMapper studentMapper;
    private final CourseKnowledgePointMapper kpMapper;
    private final MonitorReportMapper monitorReportMapper;

    public MonitorAgent(LLMClient llmClient,
                        AlertRecordMapper alertRecordMapper,
                        StudentWeakPointMapper weakPointMapper,
                        KnowledgeMasteryMapper masteryMapper,
                        ExerciseRecommendationMapper recommendMapper,
                        AlertSnapshotMapper snapshotMapper,
                        StudentMapper studentMapper,
                        CourseKnowledgePointMapper kpMapper,
                        MonitorReportMapper monitorReportMapper) {
        super(llmClient, "MonitorAgent");
        this.alertRecordMapper = alertRecordMapper;
        this.weakPointMapper = weakPointMapper;
        this.masteryMapper = masteryMapper;
        this.recommendMapper = recommendMapper;
        this.snapshotMapper = snapshotMapper;
        this.studentMapper = studentMapper;
        this.kpMapper = kpMapper;
        this.monitorReportMapper = monitorReportMapper;
    }

    @Override
    public String getSystemPrompt() {
        return """
            你是一位专业的学情监测分析师，负责识别学生的异常学习行为和风险信号。
            请基于提供的多维学习数据，做到以下几点：
            1. 识别最突出的风险信号（学业成绩、作业、出勤、知识点等维度）
            2. 标记异常变化趋势（与历史数据对比）
            3. 给每个风险维度打分（1-100，越高越危险）
            4. 给出整体风险评级（GREEN/YELLOW/ORANGE/RED）
            请使用简洁中文输出。""";
    }

    @Override
    public String buildUserPrompt(AgentContext ctx) {
        Long studentId = ctx.getStudentId();
        Student student = studentMapper.selectById(studentId);
        String studentName = student != null ? student.getStudentName() : "未知";

        // 优先从并行采集缓存读取，避免重复查询DB
        MonitorData data = ctx.getAttribute("MONITOR_DATA");
        if (data == null) {
            return buildUserPromptLegacy(ctx, studentName, studentId);
        }

        StringBuilder sb = new StringBuilder();
        sb.append("学生：").append(studentName).append("（ID=").append(studentId).append("）\n\n");

        // 1. 最新预警记录
        sb.append("=== 最新预警记录 ===\n");
        if (data.alerts.isEmpty()) {
            sb.append("暂无预警记录。\n");
        } else {
            AlertRecord latest = data.alerts.get(0);
            sb.append("预警等级：").append(latest.getAlertLevel()).append("\n");
            sb.append("预警类型：").append(latest.getAlertType()).append("\n");
            sb.append("综合风险分：").append(latest.getRiskScore()).append("\n");
            sb.append("预测成绩：").append(latest.getPredictedScore()).append("\n");
            sb.append("状态：").append(latest.getStatus()).append("\n");
            sb.append("分维度风险：学业=").append(nvl(latest.getAcademicRiskScore()))
              .append("，作业=").append(nvl(latest.getHomeworkRiskScore()))
              .append("，出勤=").append(nvl(latest.getAttendanceRiskScore()))
              .append("，知识=").append(nvl(latest.getKnowledgeRiskScore()))
              .append("，历史=").append(nvl(latest.getHistoryRiskScore())).append("\n");
        }
        sb.append("\n");

        // 2. 薄弱知识点
        sb.append("=== 薄弱知识点（共").append(data.weakPoints.size()).append("个） ===\n");
        data.weakPoints.stream().limit(10).forEach(wp -> {
            sb.append("- ").append(data.kpNameMap.getOrDefault(wp.getKpId(), "未知"))
              .append(" 错误率=").append(wp.getErrorRate()).append("%\n");
        });
        if (data.weakPoints.size() > 10) sb.append("...（仅显示前10个）\n");
        sb.append("\n");

        // 3. 知识掌握
        sb.append("=== 知识掌握情况 ===\n");
        if (data.masteries.isEmpty()) {
            sb.append("暂无掌握情况数据。\n");
        } else {
            data.masteries.forEach(m -> {
                sb.append("课程ID=").append(m.getCourseId())
                  .append("，总题数=").append(nvl(m.getTotalQuestion()))
                  .append("，错误数=").append(nvl(m.getErrorCount()))
                  .append("，薄弱点数=").append(nvl(m.getWeakKnowledgeCount()))
                  .append("\n");
            });
        }
        sb.append("\n");

        // 4. 练习题完成情况
        sb.append("=== 练习题完成情况（近30天） ===\n");
        if (data.recentRecommendations.isEmpty()) {
            sb.append("暂无练习记录。\n");
        } else {
            long completed = data.recentRecommendations.stream()
                    .filter(r -> "GRADED".equals(r.getStatus())).count();
            long pending = data.recentRecommendations.stream()
                    .filter(r -> "PENDING".equals(r.getStatus())).count();
            double avgScore = data.recentRecommendations.stream()
                    .filter(r -> r.getScore() != null)
                    .mapToInt(ExerciseRecommendation::getScore).average().orElse(0);
            sb.append("推荐总数=").append(data.recentRecommendations.size())
              .append("，已完成=").append(completed)
              .append("，待完成=").append(pending)
              .append("，平均分=").append(String.format("%.1f", avgScore)).append("\n");
        }
        sb.append("\n");

        // 5. 历史快照趋势
        sb.append("=== 风险趋势（近5次快照） ===\n");
        if (data.snapshots.isEmpty()) {
            sb.append("暂无历史快照。\n");
        } else {
            data.snapshots.stream().limit(5).forEach(s -> {
                sb.append("日期=").append(s.getSnapshotDate())
                  .append(" 风险分=").append(nvl(s.getRiskScore()))
                  .append(" 等级=").append(s.getAlertLevel())
                  .append(" 预测=").append(nvl(s.getPredictedScore())).append("\n");
            });
        }

        sb.append("\n请分析该学生的学习异常情况，按以下 JSON 格式输出：\n");
        sb.append("{\n");
        sb.append("  \"summary\": \"整体摘要（200字以内）\",\n");
        sb.append("  \"riskDimensions\": {\n");
        sb.append("    \"academic\": { \"score\": 0-100, \"level\": \"GREEN|YELLOW|ORANGE|RED\", \"detail\": \"说明\" },\n");
        sb.append("    \"homework\": { \"score\": 0-100, \"level\": \"GREEN|YELLOW|ORANGE|RED\", \"detail\": \"说明\" },\n");
        sb.append("    \"attendance\": { \"score\": 0-100, \"level\": \"GREEN|YELLOW|ORANGE|RED\", \"detail\": \"说明\" },\n");
        sb.append("    \"knowledge\": { \"score\": 0-100, \"level\": \"GREEN|YELLOW|ORANGE|RED\", \"detail\": \"说明\" },\n");
        sb.append("    \"exercise\": { \"score\": 0-100, \"level\": \"GREEN|YELLOW|ORANGE|RED\", \"detail\": \"说明\" },\n");
        sb.append("    \"trend\": { \"score\": 0-100, \"level\": \"GREEN|YELLOW|ORANGE|RED\", \"detail\": \"说明\" }\n");
        sb.append("  },\n");
        sb.append("  \"anomalyFlags\": [\"异常1\", \"异常2\"],\n");
        sb.append("  \"overallLevel\": \"GREEN|YELLOW|ORANGE|RED\",\n");
        sb.append("  \"topWeakPoints\": [\"知识点A\", \"知识点B\"]\n");
        sb.append("}");
        return sb.toString();
    }

    /**
     * 兼容旧调用路径：串行构建 User Prompt（无缓存时使用）
     */
    private String buildUserPromptLegacy(AgentContext ctx, String studentName, Long studentId) {
        StringBuilder sb = new StringBuilder();
        sb.append("学生：").append(studentName).append("（ID=").append(studentId).append("）\n\n");

        List<AlertRecord> alerts = queryAlerts(studentId);
        sb.append("=== 最新预警记录 ===\n");
        if (alerts.isEmpty()) {
            sb.append("暂无预警记录。\n");
        } else {
            AlertRecord latest = alerts.get(0);
            sb.append("预警等级：").append(latest.getAlertLevel()).append("\n");
            sb.append("预警类型：").append(latest.getAlertType()).append("\n");
            sb.append("综合风险分：").append(latest.getRiskScore()).append("\n");
            sb.append("预测成绩：").append(latest.getPredictedScore()).append("\n");
            sb.append("状态：").append(latest.getStatus()).append("\n");
            sb.append("分维度风险：学业=").append(nvl(latest.getAcademicRiskScore()))
              .append("，作业=").append(nvl(latest.getHomeworkRiskScore()))
              .append("，出勤=").append(nvl(latest.getAttendanceRiskScore()))
              .append("，知识=").append(nvl(latest.getKnowledgeRiskScore()))
              .append("，历史=").append(nvl(latest.getHistoryRiskScore())).append("\n");
        }
        sb.append("\n");

        List<StudentWeakPoint> weakPoints = queryWeakPoints(studentId, ctx.getCourseId());
        Map<Long, String> kpNameMap = loadKpNames(weakPoints);
        sb.append("=== 薄弱知识点（共").append(weakPoints.size()).append("个） ===\n");
        weakPoints.stream().limit(10).forEach(wp -> {
            sb.append("- ").append(kpNameMap.getOrDefault(wp.getKpId(), "未知"))
              .append(" 错误率=").append(wp.getErrorRate()).append("%\n");
        });
        if (weakPoints.size() > 10) sb.append("...（仅显示前10个）\n");
        sb.append("\n");

        List<KnowledgeMastery> masteries = queryMasteries(studentId, ctx.getCourseId());
        sb.append("=== 知识掌握情况 ===\n");
        if (masteries.isEmpty()) {
            sb.append("暂无掌握情况数据。\n");
        } else {
            masteries.forEach(m -> {
                sb.append("课程ID=").append(m.getCourseId())
                  .append("，总题数=").append(nvl(m.getTotalQuestion()))
                  .append("，错误数=").append(nvl(m.getErrorCount()))
                  .append("，薄弱点数=").append(nvl(m.getWeakKnowledgeCount()))
                  .append("\n");
            });
        }
        sb.append("\n");

        List<ExerciseRecommendation> recs = queryRecentRecommendations(studentId);
        sb.append("=== 练习题完成情况（近30天） ===\n");
        if (recs.isEmpty()) {
            sb.append("暂无练习记录。\n");
        } else {
            long completed = recs.stream().filter(r -> "GRADED".equals(r.getStatus())).count();
            long pending = recs.stream().filter(r -> "PENDING".equals(r.getStatus())).count();
            double avgScore = recs.stream()
                    .filter(r -> r.getScore() != null)
                    .mapToInt(ExerciseRecommendation::getScore).average().orElse(0);
            sb.append("推荐总数=").append(recs.size())
              .append("，已完成=").append(completed)
              .append("，待完成=").append(pending)
              .append("，平均分=").append(String.format("%.1f", avgScore)).append("\n");
        }
        sb.append("\n");

        List<AlertSnapshot> snapshots = querySnapshots(studentId, ctx.getCourseId());
        sb.append("=== 风险趋势（近5次快照） ===\n");
        if (snapshots.isEmpty()) {
            sb.append("暂无历史快照。\n");
        } else {
            snapshots.stream().limit(5).forEach(s -> {
                sb.append("日期=").append(s.getSnapshotDate())
                  .append(" 风险分=").append(nvl(s.getRiskScore()))
                  .append(" 等级=").append(s.getAlertLevel())
                  .append(" 预测=").append(nvl(s.getPredictedScore())).append("\n");
            });
        }

        sb.append("\n请分析该学生的学习异常情况，按以下 JSON 格式输出：\n");
        sb.append("{\n");
        sb.append("  \"summary\": \"整体摘要（200字以内）\",\n");
        sb.append("  \"riskDimensions\": {\n");
        sb.append("    \"academic\": { \"score\": 0-100, \"level\": \"GREEN|YELLOW|ORANGE|RED\", \"detail\": \"说明\" },\n");
        sb.append("    \"homework\": { \"score\": 0-100, \"level\": \"GREEN|YELLOW|ORANGE|RED\", \"detail\": \"说明\" },\n");
        sb.append("    \"attendance\": { \"score\": 0-100, \"level\": \"GREEN|YELLOW|ORANGE|RED\", \"detail\": \"说明\" },\n");
        sb.append("    \"knowledge\": { \"score\": 0-100, \"level\": \"GREEN|YELLOW|ORANGE|RED\", \"detail\": \"说明\" },\n");
        sb.append("    \"exercise\": { \"score\": 0-100, \"level\": \"GREEN|YELLOW|ORANGE|RED\", \"detail\": \"说明\" },\n");
        sb.append("    \"trend\": { \"score\": 0-100, \"level\": \"GREEN|YELLOW|ORANGE|RED\", \"detail\": \"说明\" }\n");
        sb.append("  },\n");
        sb.append("  \"anomalyFlags\": [\"异常1\", \"异常2\"],\n");
        sb.append("  \"overallLevel\": \"GREEN|YELLOW|ORANGE|RED\",\n");
        sb.append("  \"topWeakPoints\": [\"知识点A\", \"知识点B\"]\n");
        sb.append("}");
        return sb.toString();
    }

    @Override
    protected Map<String, Object> doExecute(AgentContext ctx) {
        Long studentId = ctx.getStudentId();

        // 1. 并行采集所有维度数据（5个独立查询并发执行）
        MonitorData data = collectDataParallel(ctx);

        // 2. 缓存数据，避免 buildUserPrompt / buildDataSnapshot 重复查询DB
        ctx.setAttribute("MONITOR_DATA", data);

        // 3. 构建数据快照（复用缓存，不查DB）
        Map<String, Object> dataSnapshot = buildDataSnapshot(data);

        // 4. 调用 LLM 进行异常分析（buildUserPrompt 也从缓存读取）
        String llmOutput = callLLM(ctx);
        Map<String, Object> llmResult = JsonUtils.parse(llmOutput);
        if (llmResult.containsKey("raw")) {
            llmResult = Map.of("summary", llmOutput);
        }

        // 5. 持久化监测报告
        MonitorReport report = null;
        if (!Boolean.TRUE.equals(ctx.isSkipMonitorPersistence())) {
            report = new MonitorReport();
            report.setStudentId(studentId);
            report.setCourseId(ctx.getCourseId());
            report.setReportDate(LocalDate.now());
            report.setSummary(Objects.toString(llmResult.getOrDefault("summary", llmOutput), ""));
            report.setRiskDimensions(JsonUtils.toJsonString(llmResult.get("riskDimensions")));
            report.setAnomalyFlags(JsonUtils.toJsonString(llmResult.get("anomalyFlags")));
            report.setCreateTime(LocalDateTime.now());
            monitorReportMapper.insert(report);
        }

        // 6. 构建返回结果
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("reportId", report != null ? report.getId() : null);
        result.put("studentId", studentId);
        result.put("courseId", ctx.getCourseId());
        result.put("summary", report != null ? report.getSummary()
                : Objects.toString(llmResult.getOrDefault("summary", llmOutput), ""));
        result.put("riskDimensions", llmResult.get("riskDimensions"));
        result.put("anomalyFlags", llmResult.get("anomalyFlags"));
        result.put("overallLevel", llmResult.getOrDefault("overallLevel", "YELLOW"));
        result.put("dataSnapshot", dataSnapshot);
        return result;
    }

    /**
     * 并行采集5个维度的数据（CompletableFuture 并发查询）
     * 相比串行查询，响应时间可降低约 60%
     */
    private MonitorData collectDataParallel(AgentContext ctx) {
        Long studentId = ctx.getStudentId();
        Long courseId = ctx.getCourseId();
        MonitorData data = new MonitorData();

        CompletableFuture<List<AlertRecord>> alertsFuture = CompletableFuture.supplyAsync(
                () -> queryAlerts(studentId), DATA_EXECUTOR);

        CompletableFuture<List<StudentWeakPoint>> weakPointsFuture = CompletableFuture.supplyAsync(
                () -> queryWeakPoints(studentId, courseId), DATA_EXECUTOR);

        CompletableFuture<List<KnowledgeMastery>> masteriesFuture = CompletableFuture.supplyAsync(
                () -> queryMasteries(studentId, courseId), DATA_EXECUTOR);

        CompletableFuture<List<ExerciseRecommendation>> recsFuture = CompletableFuture.supplyAsync(
                () -> queryRecentRecommendations(studentId), DATA_EXECUTOR);

        CompletableFuture<List<AlertSnapshot>> snapshotsFuture = CompletableFuture.supplyAsync(
                () -> querySnapshots(studentId, courseId), DATA_EXECUTOR);

        try {
            data.alerts = alertsFuture.get(10, TimeUnit.SECONDS);
            data.weakPoints = weakPointsFuture.get(10, TimeUnit.SECONDS);
            data.masteries = masteriesFuture.get(10, TimeUnit.SECONDS);
            data.recentRecommendations = recsFuture.get(10, TimeUnit.SECONDS);
            data.snapshots = snapshotsFuture.get(10, TimeUnit.SECONDS);

            // 补充分知识点名称映射（依赖 weakPoints 结果）
            data.kpNameMap = loadKpNames(data.weakPoints);
        } catch (Exception e) {
            log.error("[MonitorAgent] 并行数据采集失败: {}", e.getMessage(), e);
            throw new RuntimeException("数据采集失败: " + e.getMessage(), e);
        }

        return data;
    }

    // ===== 数据查询辅助方法 =====

    /**
     * 从并行采集数据构建快照（复用缓存，不查DB）
     */
    private Map<String, Object> buildDataSnapshot(MonitorData data) {
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("alerts", data.alerts.stream().map(this::alertToMap).toList());
        snapshot.put("weakPoints", data.weakPoints.size());
        snapshot.put("masteries", data.masteries.size());
        snapshot.put("snapshotCount", data.snapshots.size());
        snapshot.put("recentRecommendationCount", data.recentRecommendations.size());
        snapshot.put("completedCount",
                data.recentRecommendations.stream().filter(r -> "GRADED".equals(r.getStatus())).count());
        return snapshot;
    }

    private List<AlertRecord> queryAlerts(Long studentId) {
        return alertRecordMapper.selectList(
                new LambdaQueryWrapper<AlertRecord>()
                        .eq(AlertRecord::getStudentId, studentId)
                        .orderByDesc(AlertRecord::getCreateTime)
                        .last("LIMIT 1"));
    }

    private List<StudentWeakPoint> queryWeakPoints(Long studentId, Long courseId) {
        var wrapper = new LambdaQueryWrapper<StudentWeakPoint>()
                .eq(StudentWeakPoint::getStudentId, studentId)
                .orderByDesc(StudentWeakPoint::getErrorRate);
        if (courseId != null) wrapper.eq(StudentWeakPoint::getCourseId, courseId);
        return weakPointMapper.selectList(wrapper);
    }

    private List<KnowledgeMastery> queryMasteries(Long studentId, Long courseId) {
        var wrapper = new LambdaQueryWrapper<KnowledgeMastery>()
                .eq(KnowledgeMastery::getStudentId, studentId);
        if (courseId != null) wrapper.eq(KnowledgeMastery::getCourseId, courseId);
        return masteryMapper.selectList(wrapper);
    }

    private List<ExerciseRecommendation> queryRecentRecommendations(Long studentId) {
        LocalDateTime thirtyDaysAgo = LocalDateTime.now().minusDays(30);
        return recommendMapper.selectList(
                new LambdaQueryWrapper<ExerciseRecommendation>()
                        .eq(ExerciseRecommendation::getStudentId, studentId)
                        .ge(ExerciseRecommendation::getRecommendTime, thirtyDaysAgo)
                        .orderByDesc(ExerciseRecommendation::getRecommendTime));
    }

    private List<AlertSnapshot> querySnapshots(Long studentId, Long courseId) {
        var wrapper = new LambdaQueryWrapper<AlertSnapshot>()
                .eq(AlertSnapshot::getStudentId, studentId)
                .orderByDesc(AlertSnapshot::getSnapshotDate);
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

    private Map<String, Object> alertToMap(AlertRecord a) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", a.getId());
        m.put("alertLevel", a.getAlertLevel());
        m.put("alertType", a.getAlertType());
        m.put("riskScore", a.getRiskScore());
        m.put("status", a.getStatus());
        m.put("createTime", a.getCreateTime());
        return m;
    }

    private String nvl(Object v) {
        return v != null ? v.toString() : "N/A";
    }

    @Override
    protected void validate(AgentContext ctx) {
        super.validate(ctx);
        if (ctx.getCourseId() == null) {
            log.warn("[MonitorAgent] courseId 为空，将采用全部课程数据");
        }
    }
}
