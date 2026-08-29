package com.example.academic_risk_warning.agent.recommend;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.academic_risk_warning.agent.core.AgentContext;
import com.example.academic_risk_warning.agent.core.BaseAgent;
import com.example.academic_risk_warning.agent.core.JsonUtils;
import com.example.academic_risk_warning.entity.*;
import com.example.academic_risk_warning.llm.LLMClient;
import com.example.academic_risk_warning.mapper.*;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 推荐智能体：基于画像和分析结果，生成个性化学习计划并推荐练习资源
 */
@Component
public class RecommendAgent extends BaseAgent<Map<String, Object>> {

    private final StudyPlanMapper studyPlanMapper;
    private final StudentWeakPointMapper weakPointMapper;
    private final ExerciseMapper exerciseMapper;
    private final ExerciseKnowledgePointMapper exerciseKpMapper;
    private final ExerciseRecommendationMapper recommendMapper;
    private final StudentMapper studentMapper;
    private final CourseKnowledgePointMapper kpMapper;
    private final AlertRecordMapper alertRecordMapper;

    public RecommendAgent(LLMClient llmClient,
                          StudyPlanMapper studyPlanMapper,
                          StudentWeakPointMapper weakPointMapper,
                          ExerciseMapper exerciseMapper,
                          ExerciseKnowledgePointMapper exerciseKpMapper,
                          ExerciseRecommendationMapper recommendMapper,
                          StudentMapper studentMapper,
                          CourseKnowledgePointMapper kpMapper,
                          AlertRecordMapper alertRecordMapper) {
        super(llmClient, "RecommendAgent");
        this.studyPlanMapper = studyPlanMapper;
        this.weakPointMapper = weakPointMapper;
        this.exerciseMapper = exerciseMapper;
        this.exerciseKpMapper = exerciseKpMapper;
        this.recommendMapper = recommendMapper;
        this.studentMapper = studentMapper;
        this.kpMapper = kpMapper;
        this.alertRecordMapper = alertRecordMapper;
    }

    @Override
    public String getSystemPrompt() {
        return """
            你是一位智能学习推荐顾问，负责为每个学生量身定制学习计划和资源推荐。
            基于学生的知识画像、学习习惯和风险分析，你需要：
            1. 制定分阶段学习计划（短期/中期/长期目标）
            2. 推荐优先补强知识点和练习类型
            3. 给出具体的学习节奏建议（每周建议学习时长、练习数量）
            4. 为每个推荐项给出理由
            请使用简洁中文输出，建议切实可行、难度递进合理。""";
    }

    @Override
    public String buildUserPrompt(AgentContext ctx) {
        Long studentId = ctx.getStudentId();
        Long courseId = ctx.getCourseId();
        Student student = studentMapper.selectById(studentId);
        String studentName = student != null ? student.getStudentName() : "未知";

        Map<String, Object> profile = ctx.getProfileResult();
        Map<String, Object> analysis = ctx.getAnalysisResult();
        Map<String, Object> monitor = ctx.getMonitorResult();
        // 反馈场景时，可能有策略调整指导
        Map<String, Object> strategy = ctx.getStrategyResult();

        StringBuilder sb = new StringBuilder();
        sb.append("学生：").append(studentName).append("（ID=").append(studentId).append("）\n");
        sb.append("课程ID：").append(courseId != null ? courseId : "全部").append("\n\n");

        // 1. 三维画像摘要
        if (profile != null && !profile.isEmpty()) {
            sb.append("=== 学生三维画像 ===\n");
            sb.append("知识维度：").append(profile.getOrDefault("knowledgeProfile", "无")).append("\n");
            sb.append("习惯维度：").append(profile.getOrDefault("habitProfile", "无")).append("\n");
            sb.append("目标维度：").append(profile.getOrDefault("goalProfile", "无")).append("\n\n");
        }

        // 2. 分析摘要
        if (analysis != null && !analysis.isEmpty()) {
            sb.append("=== 学习分析摘要 ===\n");
            sb.append("薄弱类别：").append(analysis.getOrDefault("weaknessCategory", "无")).append("\n");
            sb.append("根源：").append(analysis.getOrDefault("rootCause", "无")).append("\n\n");
        }

        // 3. 薄弱知识点列表（用于匹配推荐）
        List<StudentWeakPoint> weakPoints = queryWeakPoints(studentId, courseId);
        Map<Long, String> kpNameMap = loadKpNames(weakPoints);
        sb.append("=== 薄弱知识点（前10个） ===\n");
        weakPoints.stream().limit(10).forEach(wp -> {
            sb.append("- ").append(kpNameMap.getOrDefault(wp.getKpId(), "未知"))
              .append(" 错误率=").append(wp.getErrorRate()).append("%\n");
        });
        sb.append("\n");

        // 4. 现有推荐情况
        List<ExerciseRecommendation> existingRecs = queryRecentRecommendations(studentId);
        sb.append("=== 已有练习推荐 ===\n");
        long completed = existingRecs.stream().filter(r -> "GRADED".equals(r.getStatus())).count();
        long pending = existingRecs.stream().filter(r -> "PENDING".equals(r.getStatus())).count();
        sb.append("已完成=").append(completed).append("，待完成=").append(pending).append("\n");
        if (completed > 0) {
            double avg = existingRecs.stream().filter(r -> r.getScore() != null)
                    .mapToInt(ExerciseRecommendation::getScore).average().orElse(0);
            sb.append("平均得分=").append(String.format("%.1f", avg)).append("\n");
        }
        sb.append("\n");

        // 5. 策略调整指导（反馈场景）
        if (strategy != null && !strategy.isEmpty()) {
            sb.append("=== 策略调整指导（反馈驱动） ===\n");
            sb.append(strategy.toString()).append("\n\n");
        }

        // 6. 当前风险
        String riskLevel = "未知";
        if (monitor != null) riskLevel = Objects.toString(monitor.getOrDefault("overallLevel", "未知"), "未知");
        sb.append("当前风险等级：").append(riskLevel).append("\n\n");

        sb.append("请生成学习计划和推荐，按以下 JSON 格式输出：\n");
        sb.append("{\n");
        sb.append("  \"plan\": {\n");
        sb.append("    \"title\": \"学习计划标题\",\n");
        sb.append("    \"phases\": [\n");
        sb.append("      { \"phase\": 1, \"name\": \"阶段名\", \"durationWeeks\": 2, \"focus\": \"重点内容\", \"targetScore\": 目标分 },\n");
        sb.append("      { \"phase\": 2, \"name\": \"阶段名\", \"durationWeeks\": 2, \"focus\": \"重点内容\", \"targetScore\": 目标分 },\n");
        sb.append("      { \"phase\": 3, \"name\": \"阶段名\", \"durationWeeks\": 2, \"focus\": \"重点内容\", \"targetScore\": 目标分 }\n");
        sb.append("    ],\n");
        sb.append("    \"weeklyPlan\": {\n");
        sb.append("      \"studyHours\": 建议每周学习小时数,\n");
        sb.append("      \"exerciseCount\": 建议每周练习数,\n");
        sb.append("      \"checkFrequency\": \"检查频率建议\"\n");
        sb.append("    },\n");
        sb.append("    \"totalWeeks\": 总周数\n");
        sb.append("  },\n");
        sb.append("  \"recommendations\": [\n");
        sb.append("    { \"kpName\": \"知识点名\", \"type\": \"EXERCISE|VIDEO|READING\", \"priority\": 1-10, \"reason\": \"推荐理由\" }\n");
        sb.append("  ]\n");
        sb.append("}");
        return sb.toString();
    }

    @Override
    protected Map<String, Object> doExecute(AgentContext ctx) {
        Long studentId = ctx.getStudentId();
        Long courseId = ctx.getCourseId();

        // 1. 调用 LLM 生成计划
        String llmOutput = callLLM(ctx);
        Map<String, Object> llmResult = JsonUtils.parse(llmOutput);
        if (llmResult.containsKey("raw")) {
            llmResult = Map.of("raw", llmOutput);
        }

        // 2. 保存学习计划
        @SuppressWarnings("unchecked")
        Map<String, Object> planData = (Map<String, Object>) llmResult.getOrDefault("plan", new LinkedHashMap<>());

        StudyPlan plan = new StudyPlan();
        plan.setStudentId(studentId);
        plan.setCourseId(courseId);
        plan.setPlanContent(llmOutput); // 保存完整 LLM 输出
        plan.setStartDate(LocalDate.now());
        int totalWeeks = parseTotalWeeks(planData);
        plan.setEndDate(LocalDate.now().plusWeeks(Math.max(totalWeeks, 4)));
        plan.setStatus("ACTIVE");
        plan.setCreateTime(LocalDateTime.now());
        plan.setUpdateTime(LocalDateTime.now());
        studyPlanMapper.insert(plan);

        // 3. 自动创建练习题推荐
        List<Map<String, Object>> recommendations = extractRecommendations(llmResult, ctx);
        int recCount = autoRecommendExercises(studentId, courseId, recommendations, ctx);

        // 4. 构建返回
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("planId", plan.getId());
        result.put("studentId", studentId);
        result.put("courseId", courseId);
        result.put("plan", planData);
        result.put("planStatus", "ACTIVE");
        result.put("recommendationCount", recCount);
        result.put("recommendations", recommendations);
        return result;
    }

    // ===== 自动推荐练习题 =====

    /**
     * 根据 LLM 推荐列表，从题库中匹配实际练习题并创建推荐记录
     */
    private int autoRecommendExercises(Long studentId, Long courseId,
                                        List<Map<String, Object>> llmRecs,
                                        AgentContext ctx) {
        if (llmRecs.isEmpty()) return 0;

        // 获取薄弱知识点 ID 列表
        List<StudentWeakPoint> weakPoints = queryWeakPoints(studentId, courseId);
        Set<Long> weakKpIds = weakPoints.stream()
                .map(StudentWeakPoint::getKpId).collect(Collectors.toSet());

        // 从题库匹配练习题
        List<ExerciseKnowledgePoint> ekps = exerciseKpMapper.selectList(
                new LambdaQueryWrapper<ExerciseKnowledgePoint>().in(ExerciseKnowledgePoint::getKpId, weakKpIds));
        Set<Long> matchedExerciseIds = ekps.stream()
                .map(ExerciseKnowledgePoint::getExerciseId).collect(Collectors.toSet());

        if (matchedExerciseIds.isEmpty()) {
            log.info("[RecommendAgent] 题库中无匹配薄弱知识点的练习题");
            return 0;
        }

        // 查询题目详情
        List<Exercise> exercises = exerciseMapper.selectList(
                new LambdaQueryWrapper<Exercise>()
                        .in(Exercise::getId, matchedExerciseIds)
                        .eq(Exercise::getIsActive, 1)
                        .eq(courseId != null, Exercise::getCourseId, courseId)
                        .eq(Exercise::getAuditStatus, "APPROVED")
                        .orderByAsc(Exercise::getDifficulty));

        // 按难度分组（默认混合模式已兼容 MIXED：同时取 EASY/MEDIUM/HARD 各级别题目）
        Map<String, List<Exercise>> byDifficulty = exercises.stream()
                .collect(Collectors.groupingBy(e -> e.getDifficulty() != null ? e.getDifficulty() : "MEDIUM"));

        // 智能选择：优先 EASY（薄弱点补基础），次选 MEDIUM
        List<Exercise> selected = new ArrayList<>();
        List<Exercise> easy = byDifficulty.getOrDefault("EASY", Collections.emptyList());
        List<Exercise> medium = byDifficulty.getOrDefault("MEDIUM", Collections.emptyList());
        List<Exercise> hard = byDifficulty.getOrDefault("HARD", Collections.emptyList());

        selected.addAll(easy.stream().limit(3).toList());
        selected.addAll(medium.stream().limit(3).toList());
        selected.addAll(hard.stream().limit(2).toList());

        if (selected.isEmpty()) selected = exercises.stream().limit(5).toList();

        // 批量预加载知识点名称
        Set<Long> allKpIds = new HashSet<>();
        Map<Long, List<Long>> exerciseKpMap = new HashMap<>();
        for (ExerciseKnowledgePoint ekp : ekps) {
            if (matchedExerciseIds.contains(ekp.getExerciseId())) {
                exerciseKpMap.computeIfAbsent(ekp.getExerciseId(), k -> new ArrayList<>()).add(ekp.getKpId());
                allKpIds.add(ekp.getKpId());
            }
        }
        Map<Long, String> kpNameMap = new HashMap<>();
        if (!allKpIds.isEmpty()) {
            kpMapper.selectList(new LambdaQueryWrapper<CourseKnowledgePoint>().in(CourseKnowledgePoint::getId, allKpIds))
                    .forEach(kp -> kpNameMap.put(kp.getId(), kp.getName()));
        }

        // 获取当前预警 ID
        AlertRecord latestAlert = alertRecordMapper.selectOne(
                new LambdaQueryWrapper<AlertRecord>()
                        .eq(AlertRecord::getStudentId, studentId)
                        .orderByDesc(AlertRecord::getCreateTime)
                        .last("LIMIT 1"));
        Long alertId = latestAlert != null ? latestAlert.getId() : null;

        // 创建推荐记录
        int count = 0;
        LocalDateTime now = LocalDateTime.now();
        for (int i = 0; i < Math.min(selected.size(), 8); i++) {
            Exercise e = selected.get(i);
            List<Long> kpIds = exerciseKpMap.getOrDefault(e.getId(), Collections.emptyList());
            String kpName = kpIds.stream()
                    .map(id -> kpNameMap.getOrDefault(id, ""))
                    .filter(s -> !s.isEmpty())
                    .collect(Collectors.joining("、"));
            if (kpName.isEmpty()) kpName = e.getTitle();

            int priority = i + 1;
            String aiReason = "";
            if (i < llmRecs.size()) {
                Object r = llmRecs.get(i).get("reason");
                aiReason = r != null ? r.toString() : "";
            }

            ExerciseRecommendation rec = new ExerciseRecommendation();
            rec.setExerciseId(e.getId());
            rec.setStudentId(studentId);
            rec.setAlertId(alertId);
            rec.setKnowledgePoint(kpName);
            rec.setStatus("PENDING");
            rec.setRecommendTime(now);
            rec.setAiReason(aiReason);
            rec.setPriority(priority);
            rec.setRecommendType("EXERCISE");
            recommendMapper.insert(rec);
            count++;
        }

        log.info("[RecommendAgent] 自动创建了 {} 条练习推荐", count);
        return count;
    }

    // ===== 数据查询 =====

    private List<StudentWeakPoint> queryWeakPoints(Long studentId, Long courseId) {
        var wrapper = new LambdaQueryWrapper<StudentWeakPoint>()
                .eq(StudentWeakPoint::getStudentId, studentId)
                .orderByDesc(StudentWeakPoint::getErrorRate);
        if (courseId != null) wrapper.eq(StudentWeakPoint::getCourseId, courseId);
        return weakPointMapper.selectList(wrapper);
    }

    private List<ExerciseRecommendation> queryRecentRecommendations(Long studentId) {
        LocalDateTime thirtyDaysAgo = LocalDateTime.now().minusDays(30);
        return recommendMapper.selectList(
                new LambdaQueryWrapper<ExerciseRecommendation>()
                        .eq(ExerciseRecommendation::getStudentId, studentId)
                        .ge(ExerciseRecommendation::getRecommendTime, thirtyDaysAgo)
                        .orderByDesc(ExerciseRecommendation::getRecommendTime));
    }

    private Map<Long, String> loadKpNames(List<StudentWeakPoint> weakPoints) {
        Set<Long> kpIds = weakPoints.stream().map(StudentWeakPoint::getKpId).collect(Collectors.toSet());
        if (kpIds.isEmpty()) return Collections.emptyMap();
        return kpMapper.selectList(
                new LambdaQueryWrapper<CourseKnowledgePoint>().in(CourseKnowledgePoint::getId, kpIds))
                .stream().collect(Collectors.toMap(CourseKnowledgePoint::getId, CourseKnowledgePoint::getName));
    }

    // ===== 辅助 =====

    private int parseTotalWeeks(Map<String, Object> planData) {
        try {
            Object w = planData.get("totalWeeks");
            if (w instanceof Number) return ((Number) w).intValue();
            if (w instanceof String) return Integer.parseInt((String) w);
        } catch (Exception ignored) {}
        return 6; // 默认 6 周
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> extractRecommendations(Map<String, Object> llmResult, AgentContext ctx) {
        Object recs = llmResult.get("recommendations");
        if (!(recs instanceof List)) return Collections.emptyList();
        return ((List<?>) recs).stream()
                .filter(item -> item instanceof Map)
                .map(item -> (Map<String, Object>) item)
                .toList();
    }


}
