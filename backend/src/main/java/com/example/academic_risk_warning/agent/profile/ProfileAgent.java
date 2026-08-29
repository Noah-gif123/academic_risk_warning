package com.example.academic_risk_warning.agent.profile;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.academic_risk_warning.agent.core.AgentContext;
import com.example.academic_risk_warning.agent.core.BaseAgent;
import com.example.academic_risk_warning.agent.core.JsonUtils;
import com.example.academic_risk_warning.entity.*;
import com.example.academic_risk_warning.llm.LLMClient;
import com.example.academic_risk_warning.mapper.*;
import com.example.academic_risk_warning.service.StudentGoalService;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 学生三维画像智能体：知识掌握度画像 + 学习习惯画像 + 学习目标画像
 */
@Component
public class ProfileAgent extends BaseAgent<Map<String, Object>> {

    private final StudentWeakPointMapper weakPointMapper;
    private final KnowledgeMasteryMapper masteryMapper;
    private final ExerciseRecommendationMapper recommendMapper;
    private final AlertRecordMapper alertRecordMapper;
    private final AlertSnapshotMapper snapshotMapper;
    private final StudentMapper studentMapper;
    private final CourseKnowledgePointMapper kpMapper;
    private final StudentProfileMapper studentProfileMapper;
    private final StudentGoalService studentGoalService;

    public ProfileAgent(LLMClient llmClient,
                        StudentWeakPointMapper weakPointMapper,
                        KnowledgeMasteryMapper masteryMapper,
                        ExerciseRecommendationMapper recommendMapper,
                        AlertRecordMapper alertRecordMapper,
                        AlertSnapshotMapper snapshotMapper,
                        StudentMapper studentMapper,
                        CourseKnowledgePointMapper kpMapper,
                        StudentProfileMapper studentProfileMapper,
                        StudentGoalService studentGoalService) {
        super(llmClient, "ProfileAgent");
        this.weakPointMapper = weakPointMapper;
        this.masteryMapper = masteryMapper;
        this.recommendMapper = recommendMapper;
        this.alertRecordMapper = alertRecordMapper;
        this.snapshotMapper = snapshotMapper;
        this.studentMapper = studentMapper;
        this.kpMapper = kpMapper;
        this.studentProfileMapper = studentProfileMapper;
        this.studentGoalService = studentGoalService;
    }

    @Override
    public String getSystemPrompt() {
        return """
            你是一位学习行为画像分析师，负责为每个学生生成三维学习画像。
            画像包含以下三个维度：
            1. 知识掌握画像（Knowledge Profile）：哪些知识点已经掌握、哪些薄弱、知识结构如何
            2. 学习习惯画像（Habit Profile）：学习频率、作业完成规律、响应及时性、专注度
            3. 学习目标画像（Goal Profile）：当前成绩水平、进步空间、参考历史趋势给出合理预期目标
            
            请基于数据给出客观、量化的评价，使用简洁中文。""";
    }

    @Override
    public String buildUserPrompt(AgentContext ctx) {
        Long studentId = ctx.getStudentId();
        Long courseId = ctx.getCourseId();
        Student student = studentMapper.selectById(studentId);
        String studentName = student != null ? student.getStudentName() : "未知";

        // 从上游获取分析结果
        Map<String, Object> analysis = ctx.getAnalysisResult();
        Map<String, Object> monitor = ctx.getMonitorResult();

        StringBuilder sb = new StringBuilder();
        sb.append("学生：").append(studentName).append("（ID=").append(studentId).append("）\n");
        sb.append("课程ID：").append(courseId != null ? courseId : "全部").append("\n\n");

        // 上游分析摘要
        if (analysis != null && !analysis.isEmpty()) {
            sb.append("=== 上游分析摘要 ===\n");
            sb.append("薄弱类别：").append(analysis.getOrDefault("weaknessCategory", "未知")).append("\n");
            sb.append("根源分析：").append(analysis.getOrDefault("rootCause", "未知")).append("\n\n");
        }

        // 1. 知识维度数据
        List<StudentWeakPoint> weakPoints = queryWeakPoints(studentId, courseId);
        Map<Long, String> kpNameMap = loadKpNames(weakPoints);
        sb.append("=== 知识掌握维度 ===\n");
        sb.append("薄弱知识点数：").append(weakPoints.size()).append("\n");
        weakPoints.stream().limit(10).forEach(wp -> {
            sb.append("- ").append(kpNameMap.getOrDefault(wp.getKpId(), "未知"))
              .append(" 错误率=").append(wp.getErrorRate()).append("%\n");
        });

        KnowledgeMastery mastery = queryMastery(studentId, courseId);
        if (mastery != null) {
            sb.append("总题数/错误数：").append(nvl(mastery.getTotalQuestion()))
              .append("/").append(nvl(mastery.getErrorCount())).append("\n");
            double correctRate = mastery.getTotalQuestion() != null && mastery.getTotalQuestion() > 0
                    ? (1 - (double)(mastery.getErrorCount() == null ? 0 : mastery.getErrorCount())
                        / mastery.getTotalQuestion()) * 100
                    : 0;
            sb.append("综合正确率：").append(String.format("%.1f%%", correctRate)).append("\n");
        }
        sb.append("\n");

        // 2. 学习习惯维度数据
        List<ExerciseRecommendation> recs = queryAllRecommendations(studentId);
        sb.append("=== 学习习惯维度 ===\n");
        sb.append("推荐练习总数：").append(recs.size()).append("\n");
        long completed = recs.stream().filter(r -> "GRADED".equals(r.getStatus())).count();
        long answered = recs.stream().filter(r -> "ANSWERED".equals(r.getStatus())).count();
        long pending = recs.stream().filter(r -> "PENDING".equals(r.getStatus())).count();
        sb.append("已完成=").append(completed)
          .append("，已回答=").append(answered)
          .append("，待完成=").append(pending).append("\n");

        // 平均完成时间
        double avgHours = recs.stream()
                .filter(r -> r.getCompleteTime() != null && r.getRecommendTime() != null)
                .mapToLong(r -> java.time.Duration.between(r.getRecommendTime(), r.getCompleteTime()).toHours())
                .average().orElse(0);
        sb.append("平均完成耗时：").append(String.format("%.1f 小时", avgHours)).append("\n");

        // 练习间隔分析
        if (recs.size() >= 2) {
            List<LocalDateTime> times = recs.stream()
                    .map(ExerciseRecommendation::getRecommendTime)
                    .filter(Objects::nonNull)
                    .sorted(Comparator.reverseOrder())
                    .toList();
            if (times.size() >= 2) {
                double avgIntervalHours = 0;
                int count = 0;
                for (int i = 0; i < times.size() - 1; i++) {
                    avgIntervalHours += java.time.Duration.between(times.get(i + 1), times.get(i)).toHours();
                    count++;
                }
                sb.append("平均练习间隔：").append(String.format("%.1f 小时", avgIntervalHours / Math.max(count, 1))).append("\n");
            }
        }

        // 平均得分
        double avgScore = recs.stream()
                .filter(r -> r.getScore() != null)
                .mapToInt(ExerciseRecommendation::getScore).average().orElse(0);
        sb.append("平均练习得分：").append(String.format("%.1f 分", avgScore)).append("\n\n");

        // 3. 学习目标维度数据
        List<AlertSnapshot> snapshots = querySnapshots(studentId, courseId);
        sb.append("=== 学习目标维度 ===\n");
        if (!snapshots.isEmpty()) {
            double first = nvlDouble(snapshots.get(snapshots.size() - 1).getRiskScore());
            double last = nvlDouble(snapshots.get(0).getRiskScore());
            double firstPred = nvlDouble(snapshots.get(snapshots.size() - 1).getPredictedScore());
            double lastPred = nvlDouble(snapshots.get(0).getPredictedScore());

            sb.append("首次风险分=").append(String.format("%.1f", first))
              .append("，最新风险分=").append(String.format("%.1f", last)).append("\n");
            sb.append("首次预测分=").append(String.format("%.1f", firstPred))
              .append("，最新预测分=").append(String.format("%.1f", lastPred)).append("\n");
            sb.append("风险浓度：").append(snapshots.stream().filter(s -> "RED".equals(s.getAlertLevel()) || "ORANGE".equals(s.getAlertLevel())).count())
              .append("/").append(snapshots.size()).append(" 次\n");
        } else {
            sb.append("暂无历史快照。\n");
        }

        // 当前预警
        AlertRecord latest = queryLatestAlert(studentId);
        if (latest != null) {
            sb.append("当前预警等级=").append(latest.getAlertLevel())
              .append("，类型=").append(latest.getAlertType())
              .append("，预测成绩=").append(nvl(latest.getPredictedScore())).append("\n");
        }
        sb.append("\n");

        // 学生自定义学习目标（优先采纳，作为画像生成的参考）
        StudentGoal studentGoal = studentGoalService.getByStudentAndCourse(studentId, courseId);
        if (studentGoal != null) {
            sb.append("=== 学生自定义学习目标（优先参考） ===\n");
            sb.append("目标等级：").append(studentGoal.getGoalLevel()).append("\n");
            if (studentGoal.getTargetScore() != null) {
                sb.append("目标分数：").append(studentGoal.getTargetScore()).append("\n");
            }
            if (studentGoal.getMilestones() != null && !studentGoal.getMilestones().isBlank()) {
                sb.append("里程碑：").append(studentGoal.getMilestones()).append("\n");
            }
            if (studentGoal.getNote() != null && !studentGoal.getNote().isBlank()) {
                sb.append("学生备注：").append(studentGoal.getNote()).append("\n");
            }
            sb.append("\n");
        }

        sb.append("请生成三维画像，按以下 JSON 格式输出：\n");
        sb.append("{\n");
        sb.append("  \"knowledgeProfile\": {\n");
        sb.append("    \"level\": \"优秀|良好|一般|薄弱|严重\",\n");
        sb.append("    \"summary\": \"知识维度总结（150字以内）\",\n");
        sb.append("    \"strongPoints\": [\"已掌握知识点A\", \"已掌握知识点B\"],\n");
        sb.append("    \"weakPoints\": [\"薄弱知识点A\", \"薄弱知识点B\"],\n");
        sb.append("    \"masteryPercent\": 0-100\n");
        sb.append("  },\n");
        sb.append("  \"habitProfile\": {\n");
        sb.append("    \"level\": \"自律型|规律型|波动型|拖延型|放弃型\",\n");
        sb.append("    \"summary\": \"习惯维度总结（150字以内）\",\n");
        sb.append("    \"traits\": [\"特征1\", \"特征2\"],\n");
        sb.append("    \"suggestedHabits\": [\"建议培养习惯1\", \"建议培养习惯2\"]\n");
        sb.append("  },\n");
        sb.append("  \"goalProfile\": {\n");
        sb.append("    \"level\": \"冲刺|稳定|追赶|保底\",\n");
        sb.append("    \"summary\": \"目标维度总结（150字以内）\",\n");
        sb.append("    \"targetScore\": 目标分数,\n");
        sb.append("    \"feasibility\": \"高|中|低\",\n");
        sb.append("    \"milestones\": [\"里程碑1\", \"里程碑2\"]\n");
        sb.append("  }\n");
        sb.append("}");
        return sb.toString();
    }

    @Override
    protected Map<String, Object> doExecute(AgentContext ctx) {
        Long studentId = ctx.getStudentId();

        String llmOutput = callLLM(ctx);
        Map<String, Object> llmResult = JsonUtils.parse(llmOutput);
        if (llmResult.containsKey("raw")) {
            llmResult = Map.of("summary", llmOutput);
        }

        // 持久化学生三维画像
        StudentProfile profile = new StudentProfile();
        profile.setStudentId(studentId);
        // 更新已有或新增
        StudentProfile existing = studentProfileMapper.selectOne(
                new LambdaQueryWrapper<StudentProfile>()
                        .eq(StudentProfile::getStudentId, studentId)
                        .last("LIMIT 1"));
        if (existing != null) {
            profile.setId(existing.getId());
            profile.setCreateTime(existing.getCreateTime());
        }
        profile.setKnowledgeProfile(JsonUtils.toJsonString(llmResult.get("knowledgeProfile")));
        profile.setHabitProfile(JsonUtils.toJsonString(llmResult.get("habitProfile")));
        profile.setGoalProfile(JsonUtils.toJsonString(llmResult.get("goalProfile")));
        profile.setUpdateTime(LocalDateTime.now());
        if (profile.getId() == null) {
            profile.setCreateTime(LocalDateTime.now());
            studentProfileMapper.insert(profile);
        } else {
            studentProfileMapper.updateById(profile);
        }

        // 返回结果
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("profileId", profile.getId());
        result.put("studentId", studentId);
        result.put("knowledgeProfile", llmResult.get("knowledgeProfile"));
        result.put("habitProfile", llmResult.get("habitProfile"));
        result.put("goalProfile", llmResult.get("goalProfile"));
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

    private List<ExerciseRecommendation> queryAllRecommendations(Long studentId) {
        return recommendMapper.selectList(
                new LambdaQueryWrapper<ExerciseRecommendation>()
                        .eq(ExerciseRecommendation::getStudentId, studentId)
                        .orderByDesc(ExerciseRecommendation::getRecommendTime));
    }

    private List<AlertSnapshot> querySnapshots(Long studentId, Long courseId) {
        var wrapper = new LambdaQueryWrapper<AlertSnapshot>()
                .eq(AlertSnapshot::getStudentId, studentId)
                .orderByAsc(AlertSnapshot::getSnapshotDate);
        if (courseId != null) wrapper.eq(AlertSnapshot::getCourseId, courseId);
        return snapshotMapper.selectList(wrapper);
    }

    private AlertRecord queryLatestAlert(Long studentId) {
        return alertRecordMapper.selectOne(
                new LambdaQueryWrapper<AlertRecord>()
                        .eq(AlertRecord::getStudentId, studentId)
                        .orderByDesc(AlertRecord::getCreateTime)
                        .last("LIMIT 1"));
    }

    private Map<Long, String> loadKpNames(List<StudentWeakPoint> weakPoints) {
        Set<Long> kpIds = weakPoints.stream().map(StudentWeakPoint::getKpId).collect(Collectors.toSet());
        if (kpIds.isEmpty()) return Collections.emptyMap();
        return kpMapper.selectList(
                new LambdaQueryWrapper<CourseKnowledgePoint>().in(CourseKnowledgePoint::getId, kpIds))
                .stream().collect(Collectors.toMap(CourseKnowledgePoint::getId, CourseKnowledgePoint::getName));
    }

    private String nvl(Object v) { return v != null ? v.toString() : "N/A"; }

    private double nvlDouble(BigDecimal v) { return v != null ? v.doubleValue() : 0; }
}
