package com.example.academic_risk_warning.agent.strategy;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.academic_risk_warning.agent.core.AgentContext;
import com.example.academic_risk_warning.agent.core.BaseAgent;
import com.example.academic_risk_warning.agent.core.JsonUtils;
import com.example.academic_risk_warning.config.WarningSystemProperties;
import com.example.academic_risk_warning.entity.*;
import com.example.academic_risk_warning.llm.LLMClient;
import com.example.academic_risk_warning.mapper.*;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 智能体：策略调整 — 基于反馈、效果数据和风险趋势，动态调整干预策略
 *
 * 决策逻辑：
 * 1. 风险分连续下降 + 学生正反馈 → CONTINUE（保持策略）
 * 2. 风险分不变或小幅波动 → ADJUST_DIFFICULTY（调整难度/重点）
 * 3. 风险分上升 + 学生负反馈 → ESCALATE（升级干预强度）
 * 4. 风险分降至安全线以下 → DOWNGRADE（降低干预强度）
 *
 * 输出决策映射给 RecommendAgent 使用，实现「反馈 → 调整 → 重推」闭环。
 */
@Component
public class StrategyAgent extends BaseAgent<Map<String, Object>> {

    private final AlertRecordMapper alertRecordMapper;
    private final AlertSnapshotMapper snapshotMapper;
    private final StrategyRecordMapper strategyRecordMapper;
    private final StudentMapper studentMapper;
    private final ExerciseRecommendationMapper recommendMapper;
    private final StudentWeakPointMapper weakPointMapper;
    private final WarningSystemProperties warningProps;

    public StrategyAgent(LLMClient llmClient,
                         AlertRecordMapper alertRecordMapper,
                         AlertSnapshotMapper snapshotMapper,
                         StrategyRecordMapper strategyRecordMapper,
                         StudentMapper studentMapper,
                         ExerciseRecommendationMapper recommendMapper,
                         StudentWeakPointMapper weakPointMapper,
                         WarningSystemProperties warningProps) {
        super(llmClient, "StrategyAgent");
        this.alertRecordMapper = alertRecordMapper;
        this.snapshotMapper = snapshotMapper;
        this.strategyRecordMapper = strategyRecordMapper;
        this.studentMapper = studentMapper;
        this.recommendMapper = recommendMapper;
        this.weakPointMapper = weakPointMapper;
        this.warningProps = warningProps;
    }

    // ===== System Prompt =====

    @Override
    public String getSystemPrompt() {
        return """
            你是学业预警系统的「策略调整智能体」，负责根据学生反馈和学习效果数据，动态决策干预策略。

            决策维度：
            1. CONTINUE：风险改善明显且学生满意 → 保持当前策略
            2. ADJUST_DIFFICULTY：难度不匹配学生的实际水平 → 降低或提高练习题难度
            3. ADJUST_FOCUS：薄弱知识点分布变化 → 调整重点补强方向
            4. ESCALATE：风险恶化或学生未回应 → 升级为更高强度干预（更多练习、更频繁检查）
            5. DOWNGRADE：风险已降至安全水平 → 降级为日常维系

            决策依据（按优先级）：
            - 风险趋势：风险分走势（下降/平稳/上升）
            - 练习效果：推荐练习完成率、平均得分
            - 学生反馈：反馈类型和方向（太难/太简单/不相关/节奏问题）
            - 薄弱点演变：是否有新的薄弱知识点出现

            请按以下 JSON 格式输出决策：
            {
              "action": "CONTINUE|ADJUST_DIFFICULTY|ADJUST_FOCUS|ESCALATE|DOWNGRADE",
              "reason": "决策理由（100字以内）",
              "adjustDetail": {
                "targetDifficulty": "EASY|MEDIUM|HARD|MIXED",
                "focusAreas": ["知识点A", "知识点B"],
                "weeklyExerciseCount": 建议每周练习题数量,
                "checkFrequency": "建议检查频率",
                "tone": "鼓励型|督促型|严肃型"
              },
              "priorityChange": "UPGRADE|DOWNGRADE|MAINTAIN",
              "confidence": 0.0-1.0
            }

            不要输出 ```json 标记。""";
    }

    @Override
    public String buildUserPrompt(AgentContext ctx) {
        Long studentId = ctx.getStudentId();
        Long courseId = ctx.getCourseId();
        Student student = studentMapper.selectById(studentId);
        String studentName = student != null ? student.getStudentName() : "未知";

        StringBuilder sb = new StringBuilder();
        sb.append("学生：").append(studentName).append("（ID=").append(studentId).append("）\n");
        sb.append("课程ID：").append(courseId != null ? courseId : "全部").append("\n");
        sb.append("触发类型：").append(ctx.getTriggerType()).append("\n\n");

        // 1. 学生反馈（来自 FeedbackAgent 或外部输入）
        Map<String, Object> feedback = ctx.getFeedbackResult();
        sb.append("=== 学生反馈 ===\n");
        if (feedback != null && !feedback.isEmpty()) {
            appendFeedback(sb, feedback);
        } else {
            sb.append("无直接学生反馈（基于效果数据判断）。\n");
        }
        sb.append("\n");

        // 2. 当前风险状态
        AlertRecord latestAlert = queryLatestAlert(studentId);
        sb.append("=== 当前风险状态 ===\n");
        if (latestAlert != null) {
            sb.append("风险等级：").append(latestAlert.getAlertLevel()).append("\n");
            sb.append("综合风险分：").append(nvl(latestAlert.getRiskScore())).append("\n");
            sb.append("预测成绩：").append(nvl(latestAlert.getPredictedScore())).append("\n");
            sb.append("风险维度：学业=").append(nvl(latestAlert.getAcademicRiskScore()))
              .append("，作业=").append(nvl(latestAlert.getHomeworkRiskScore()))
              .append("，出勤=").append(nvl(latestAlert.getAttendanceRiskScore()))
              .append("，知识=").append(nvl(latestAlert.getKnowledgeRiskScore()))
              .append("，历史=").append(nvl(latestAlert.getHistoryRiskScore())).append("\n");
        } else {
            sb.append("暂无预警记录。\n");
        }
        sb.append("\n");

        // 3. 风险趋势
        List<AlertSnapshot> snapshots = querySnapshots(studentId, courseId);
        sb.append("=== 风险趋势（近").append(snapshots.size()).append("次） ===\n");
        if (snapshots.size() >= 2) {
            for (int i = 0; i < Math.min(snapshots.size(), 10); i++) {
                AlertSnapshot s = snapshots.get(i);
                sb.append(String.format("  %s | 风险分=%s | 等级=%s | 预测=%s\n",
                        s.getSnapshotDate(), nvl(s.getRiskScore()),
                        s.getAlertLevel(), nvl(s.getPredictedScore())));
            }
            // 趋势分析
            AlertSnapshot latest = snapshots.get(0);
            AlertSnapshot earliest = snapshots.get(snapshots.size() - 1);
            double delta = toDouble(latest.getRiskScore()) - toDouble(earliest.getRiskScore());
            String trendDesc = delta < -warningProps.getInterventionImproveThreshold() ? "显著改善"
                    : delta < 0 ? "小幅改善"
                    : delta > warningProps.getInterventionImproveThreshold() ? "显著恶化"
                    : delta > 0 ? "小幅恶化" : "稳定";
            sb.append(String.format("趋势：%s（变化量=%.1f）\n", trendDesc, delta));
        } else {
            sb.append("历史快照不足（需要至少2次）。\n");
        }
        sb.append("\n");

        // 4. 练习完成情况（干预效果指标）
        List<ExerciseRecommendation> recentRecs = queryRecentRecommendations(studentId);
        sb.append("=== 近期练习效果 ===\n");
        long completed = recentRecs.stream().filter(r -> "GRADED".equals(r.getStatus())).count();
        long pending = recentRecs.stream().filter(r -> "PENDING".equals(r.getStatus())).count();
        double avgScore = recentRecs.stream()
                .filter(r -> r.getScore() != null && "GRADED".equals(r.getStatus()))
                .mapToInt(ExerciseRecommendation::getScore).average().orElse(0);
        double completionRate = recentRecs.isEmpty() ? 0 : (double) completed / recentRecs.size() * 100;
        sb.append("推荐总数=").append(recentRecs.size())
          .append("，已完成=").append(completed)
          .append("，待完成=").append(pending)
          .append("，完成率=").append(String.format("%.0f%%", completionRate))
          .append("，平均得分=").append(String.format("%.1f", avgScore)).append("\n");

        // 干预效果评估
        if (avgScore >= warningProps.getPassScoreThreshold() && completionRate >= 50) {
            sb.append("效果评估：良好——学生积极参与且得分合格。\n");
        } else if (completionRate < 30) {
            sb.append("效果评估：差——学生几乎未完成推荐练习，需升级关注。\n");
        } else if (avgScore < warningProps.getPassScoreThreshold()) {
            sb.append("效果评估：中等——学生参与但得分偏低，需调整难度。\n");
        } else {
            sb.append("效果评估：一般——有待观察。\n");
        }
        sb.append("\n");

        // 5. 薄弱知识点演变
        List<StudentWeakPoint> weakPoints = queryWeakPoints(studentId, courseId);
        sb.append("=== 薄弱知识点概况 ===\n");
        sb.append("薄弱点数=").append(weakPoints.size());
        if (!weakPoints.isEmpty()) {
            double avgErrorRate = weakPoints.stream()
                    .mapToDouble(wp -> wp.getErrorRate() != null ? wp.getErrorRate().doubleValue() : 0)
                    .average().orElse(0);
            sb.append("，平均错误率=").append(String.format("%.1f%%", avgErrorRate));
            sb.append("\n高错误率（>" + warningProps.getWeakPointErrorRateThreshold() + "%）：");
            sb.append(weakPoints.stream()
                    .filter(wp -> wp.getErrorRate() != null
                            && wp.getErrorRate().doubleValue() > warningProps.getWeakPointErrorRateThreshold())
                    .map(wp -> wp.getKpId().toString())
                    .collect(Collectors.joining("、")));
        }
        sb.append("\n\n");

        // 6. 历史策略
        List<StrategyRecord> prevStrategies = queryPreviousStrategies(studentId, courseId);
        sb.append("=== 历史策略（最近3次） ===\n");
        if (prevStrategies.isEmpty()) {
            sb.append("无历史策略记录（首次干预）。\n");
        } else {
            prevStrategies.stream().limit(3).forEach(s -> {
                sb.append("- ").append(s.getCreateTime()).append(" | ")
                  .append(s.getNewStrategy()).append(" | 原因：")
                  .append(s.getChangeReason()).append("\n");
            });
        }
        sb.append("\n");

        // 7. 当前策略（如果有）
        Map<String, Object> currentStrategy = ctx.getStrategyResult();
        if (currentStrategy != null && !currentStrategy.isEmpty()) {
            sb.append("=== 当前策略 ===\n");
            sb.append(currentStrategy.toString()).append("\n\n");
        }

        sb.append("请基于以上多维数据，输出策略调整决策。");
        return sb.toString();
    }

    // ===== 核心执行 =====

    @Override
    protected Map<String, Object> doExecute(AgentContext ctx) {
        Long studentId = ctx.getStudentId();
        Long courseId = ctx.getCourseId();

        // 1. 基于规则的预判（快速路径）
        Map<String, Object> ruleBasedDecision = makeRuleBasedDecision(ctx);
        boolean useRuleOnly = Boolean.TRUE.equals(ruleBasedDecision.get("ruleOnly"));

        // 2. LLM 深度分析（复杂场景）
        Map<String, Object> llmDecision;
        if (useRuleOnly) {
            llmDecision = ruleBasedDecision;
            log.info("[StrategyAgent] 使用规则引擎快速决策: action={}", llmDecision.get("action"));
        } else {
            String llmOutput = callLLM(ctx);
            llmDecision = JsonUtils.parse(llmOutput);
            if (llmDecision.containsKey("raw")) {
                llmDecision = Map.of("action", "CONTINUE", "reason", "解析异常，保持默认策略");
            }
            // 规则预判作为参考
            llmDecision.put("ruleBasedFallback", ruleBasedDecision);
        }

        // 3. 提取决策字段
        String action = Objects.toString(
                llmDecision.getOrDefault("action", "CONTINUE"), "CONTINUE");
        String reason = Objects.toString(
                llmDecision.getOrDefault("reason", "基于多维度数据综合分析"), "基于多维度数据综合分析");
        String priorityChange = Objects.toString(
                llmDecision.getOrDefault("priorityChange", "MAINTAIN"), "MAINTAIN");

        @SuppressWarnings("unchecked")
        Map<String, Object> adjustDetail = llmDecision.get("adjustDetail") instanceof Map
                ? (Map<String, Object>) llmDecision.get("adjustDetail")
                : Map.of();

        // 4. 持久化策略记录
        StrategyRecord record = new StrategyRecord();
        record.setStudentId(studentId);
        record.setCourseId(courseId);
        record.setPreviousStrategy(getPreviousStrategySummary(studentId, courseId));
        record.setNewStrategy(action + ": " + reason);
        record.setChangeReason(reason);
        record.setTriggerSource(ctx.getTriggerType() != null ? ctx.getTriggerType() : "MANUAL");
        record.setCreateTime(LocalDateTime.now());
        strategyRecordMapper.insert(record);

        // 5. 构建返回结果
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("recordId", record.getId());
        result.put("action", action);
        result.put("reason", reason);
        result.put("priorityChange", priorityChange);
        result.put("adjustDetail", adjustDetail);
        result.put("studentId", studentId);
        result.put("courseId", courseId);
        result.put("agent", agentName);
        result.put("confidence", llmDecision.getOrDefault("confidence", 0.7));

        log.info("[StrategyAgent] 决策完成: action={}, reason={}", action, reason);
        return result;
    }

    // ===== 规则引擎快速决策 =====

    /**
     * 基于规则的预判：适用于信号明确、无需 LLM 深度分析的场景
     */
    private Map<String, Object> makeRuleBasedDecision(AgentContext ctx) {
        Long studentId = ctx.getStudentId();
        Long courseId = ctx.getCourseId();

        AlertRecord alert = queryLatestAlert(studentId);
        List<AlertSnapshot> snapshots = querySnapshots(studentId, courseId);
        List<ExerciseRecommendation> recs = queryRecentRecommendations(studentId);
        Map<String, Object> feedback = ctx.getFeedbackResult();

        double currentRisk = alert != null && alert.getRiskScore() != null
                ? alert.getRiskScore().doubleValue() : 50;
        double riskDelta = calculateRiskDelta(snapshots);
        double completionRate = recs.isEmpty() ? 0
                : (double) recs.stream().filter(r -> "GRADED".equals(r.getStatus())).count() / recs.size();
        double avgScore = recs.stream()
                .filter(r -> r.getScore() != null && "GRADED".equals(r.getStatus()))
                .mapToInt(ExerciseRecommendation::getScore).average().orElse(0);

        // 信号 1：风险已降至安全线 → 降级
        if (currentRisk < warningProps.getInterventionLowRiskThreshold()
                && riskDelta < warningProps.getInterventionImproveThreshold()) {
            return Map.of(
                    "action", "DOWNGRADE",
                    "reason", String.format("风险分(%.1f)已降至安全线(%.1f)以下，且呈持续改善趋势",
                            currentRisk, warningProps.getInterventionLowRiskThreshold()),
                    "ruleOnly", true,
                    "adjustDetail", Map.of(
                            "targetDifficulty", "MEDIUM",
                            "focusAreas", List.of(),
                            "weeklyExerciseCount", 3,
                            "checkFrequency", "每月一次",
                            "tone", "鼓励型"),
                    "priorityChange", "DOWNGRADE",
                    "confidence", 0.85
            );
        }

        // 信号 2：风险显著恶化 + 低参与度 → 升级
        if (riskDelta > warningProps.getInterventionImproveThreshold()
                && completionRate < 0.3) {
            return Map.of(
                    "action", "ESCALATE",
                    "reason", String.format("风险分上升%.0f分且练习完成率仅%.0f%%，需升级干预",
                            riskDelta, completionRate * 100),
                    "ruleOnly", true,
                    "adjustDetail", Map.of(
                            "targetDifficulty", "EASY",
                            "focusAreas", List.of(),
                            "weeklyExerciseCount", 10,
                            "checkFrequency", "每三天一次",
                            "tone", "严肃型"),
                    "priorityChange", "UPGRADE",
                    "confidence", 0.9
            );
        }

        // 信号 3：学生明确反馈太难 → 降难度
        if (feedback != null && hasNegativeFeedback(feedback, "TOO_HARD")) {
            return Map.of(
                    "action", "ADJUST_DIFFICULTY",
                    "reason", "学生反馈练习题难度过高",
                    "ruleOnly", true,
                    "adjustDetail", Map.of(
                            "targetDifficulty", "EASY",
                            "focusAreas", List.of(),
                            "weeklyExerciseCount", 5,
                            "checkFrequency", "每周一次",
                            "tone", "鼓励型"),
                    "priorityChange", "MAINTAIN",
                    "confidence", 0.8
            );
        }

        // 信号 4：练习得分低（低于及格线） → 降难度
        if (avgScore > 0 && avgScore < warningProps.getPassScoreThreshold() && completionRate > 0.3) {
            return Map.of(
                    "action", "ADJUST_DIFFICULTY",
                    "reason", String.format("近期练习平均得分(%.1f)低于及格线(%d)，需降低难度",
                            avgScore, warningProps.getPassScoreThreshold()),
                    "ruleOnly", true,
                    "adjustDetail", Map.of(
                            "targetDifficulty", "EASY",
                            "focusAreas", List.of(),
                            "weeklyExerciseCount", 6,
                            "checkFrequency", "每周一次",
                            "tone", "鼓励型"),
                    "priorityChange", "MAINTAIN",
                    "confidence", 0.8
            );
        }

        // 信号 5：风险持续改善 + 学生正反馈 → 保持
        if (riskDelta < -5 && completionRate > 0.5 && avgScore >= warningProps.getPassScoreThreshold()) {
            return Map.of(
                    "action", "CONTINUE",
                    "reason", "风险改善且练习效果良好，保持当前策略",
                    "ruleOnly", true,
                    "adjustDetail", Map.of(
                            "targetDifficulty", "MIXED",
                            "focusAreas", List.of(),
                            "weeklyExerciseCount", 5,
                            "checkFrequency", "每两周一次",
                            "tone", "鼓励型"),
                    "priorityChange", "MAINTAIN",
                    "confidence", 0.85
            );
        }

        // 默认：需要 LLM 深度分析
        Map<String, Object> hint = new LinkedHashMap<>();
        hint.put("ruleOnly", false);
        hint.put("hint", String.format("风险分=%.1f, 趋势变化=%.1f, 完成率=%.0f%%, 平均分=%.1f",
                currentRisk, riskDelta, completionRate * 100, avgScore));
        return hint;
    }

    // ===== 数据查询 =====

    private AlertRecord queryLatestAlert(Long studentId) {
        return alertRecordMapper.selectOne(
                new LambdaQueryWrapper<AlertRecord>()
                        .eq(AlertRecord::getStudentId, studentId)
                        .orderByDesc(AlertRecord::getCreateTime)
                        .last("LIMIT 1"));
    }

    private List<AlertSnapshot> querySnapshots(Long studentId, Long courseId) {
        var wrapper = new LambdaQueryWrapper<AlertSnapshot>()
                .eq(AlertSnapshot::getStudentId, studentId)
                .orderByDesc(AlertSnapshot::getSnapshotDate);
        if (courseId != null) wrapper.eq(AlertSnapshot::getCourseId, courseId);
        return snapshotMapper.selectList(wrapper);
    }

    private List<ExerciseRecommendation> queryRecentRecommendations(Long studentId) {
        LocalDateTime thirtyDaysAgo = LocalDateTime.now().minusDays(30);
        return recommendMapper.selectList(
                new LambdaQueryWrapper<ExerciseRecommendation>()
                        .eq(ExerciseRecommendation::getStudentId, studentId)
                        .ge(ExerciseRecommendation::getRecommendTime, thirtyDaysAgo)
                        .orderByDesc(ExerciseRecommendation::getRecommendTime));
    }

    private List<StudentWeakPoint> queryWeakPoints(Long studentId, Long courseId) {
        var wrapper = new LambdaQueryWrapper<StudentWeakPoint>()
                .eq(StudentWeakPoint::getStudentId, studentId)
                .orderByDesc(StudentWeakPoint::getErrorRate);
        if (courseId != null) wrapper.eq(StudentWeakPoint::getCourseId, courseId);
        return weakPointMapper.selectList(wrapper);
    }

    private List<StrategyRecord> queryPreviousStrategies(Long studentId, Long courseId) {
        var wrapper = new LambdaQueryWrapper<StrategyRecord>()
                .eq(StrategyRecord::getStudentId, studentId)
                .orderByDesc(StrategyRecord::getCreateTime);
        if (courseId != null) wrapper.eq(StrategyRecord::getCourseId, courseId);
        return strategyRecordMapper.selectList(wrapper);
    }

    // ===== 辅助方法 =====

    private double calculateRiskDelta(List<AlertSnapshot> snapshots) {
        if (snapshots.size() < 2) return 0;
        double latest = toDouble(snapshots.get(0).getRiskScore());
        double earliest = toDouble(snapshots.get(snapshots.size() - 1).getRiskScore());
        return latest - earliest;
    }

    private double toDouble(BigDecimal v) {
        return v != null ? v.doubleValue() : 0;
    }

    private String getPreviousStrategySummary(Long studentId, Long courseId) {
        List<StrategyRecord> prev = queryPreviousStrategies(studentId, courseId);
        if (prev.isEmpty()) return "初始策略";
        return prev.get(0).getNewStrategy();
    }

    @SuppressWarnings("unchecked")
    private void appendFeedback(StringBuilder sb, Map<String, Object> feedback) {
        Object content = feedback.get("content");
        Object intent = feedback.get("intent");
        Object sentiment = feedback.get("sentiment");
        sb.append("内容：").append(Objects.toString(content, "")).append("\n");
        if (intent != null) {
            sb.append("意图：").append(intent).append("\n");
        }
        if (sentiment != null) {
            sb.append("情感值：").append(sentiment).append("\n");
        }
        Object detail = feedback.get("feedbackDetail");
        if (detail instanceof Map) {
            Map<String, Object> fd = (Map<String, Object>) detail;
            Object dim = fd.get("dimension");
            Object dir = fd.get("direction");
            Object kw = fd.get("keywords");
            if (dim != null) sb.append("反馈维度：").append(dim).append("\n");
            if (dir != null) sb.append("方向：").append(dir).append("\n");
            if (kw != null) sb.append("关键词：").append(kw).append("\n");
        }
        Object requires = feedback.get("requiresStrategyAdjust");
        if (Boolean.TRUE.equals(requires)) {
            sb.append("【需要策略调整】\n");
        }
    }

    private boolean hasNegativeFeedback(Map<String, Object> feedback, String direction) {
        // 检查 feedbackDetail.direction
        Object detail = feedback.get("feedbackDetail");
        if (detail instanceof Map) {
            Object dir = ((Map<String, Object>) detail).get("direction");
            if (direction.equals(dir)) return true;
        }
        // 检查是否有 requiresStrategyAdjust 标记
        Object requires = feedback.get("requiresStrategyAdjust");
        if (Boolean.TRUE.equals(requires)) {
            // 进一步检查是否是特定方向
            Object intent = feedback.get("intent");
            if ("NEGATIVE_FEEDBACK".equals(intent)) return true;
        }
        return false;
    }

    private String nvl(Object v) {
        return v != null ? v.toString() : "N/A";
    }
}
