package com.example.academic_risk_warning.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.academic_risk_warning.agent.core.JsonUtils;
import com.example.academic_risk_warning.entity.*;
import com.example.academic_risk_warning.mapper.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * 学生长期记忆服务（W3）：让智能体"记得上次的建议"。
 *
 * <p>每次流水线跑完，把该生该课程的画像等级、上期策略、最近干预、学生自设目标、
 * 最近预警与上次学习计划<b>规则化压缩</b>成一段短文本（不调用大模型，确定性、可测试），
 * 按版本号保留历史；下次分析与推荐时把最新一条注入 prompt。
 */
@Service
public class StudentMemoryService {

    private static final Logger log = LoggerFactory.getLogger(StudentMemoryService.class);

    private final StudentMemoryMapper memoryMapper;
    private final StudentProfileMapper profileMapper;
    private final StrategyRecordMapper strategyMapper;
    private final InterventionRecordMapper interventionMapper;
    private final StudentGoalMapper goalMapper;
    private final AlertRecordMapper alertRecordMapper;
    private final StudyPlanMapper studyPlanMapper;

    public StudentMemoryService(StudentMemoryMapper memoryMapper,
                                StudentProfileMapper profileMapper,
                                StrategyRecordMapper strategyMapper,
                                InterventionRecordMapper interventionMapper,
                                StudentGoalMapper goalMapper,
                                AlertRecordMapper alertRecordMapper,
                                StudyPlanMapper studyPlanMapper) {
        this.memoryMapper = memoryMapper;
        this.profileMapper = profileMapper;
        this.strategyMapper = strategyMapper;
        this.interventionMapper = interventionMapper;
        this.goalMapper = goalMapper;
        this.alertRecordMapper = alertRecordMapper;
        this.studyPlanMapper = studyPlanMapper;
    }

    /** 最新一条记忆（课程级优先，其次综合），没有则 null */
    public StudentMemory getLatest(Long studentId, Long courseId) {
        if (studentId == null) {
            return null;
        }
        StudentMemory memory = memoryMapper.selectOne(new LambdaQueryWrapper<StudentMemory>()
                .eq(StudentMemory::getStudentId, studentId)
                .eq(courseId != null, StudentMemory::getCourseId, courseId)
                .isNull(courseId == null, StudentMemory::getCourseId)
                .orderByDesc(StudentMemory::getVersion)
                .last("LIMIT 1"));
        if (memory == null && courseId != null) {
            memory = memoryMapper.selectOne(new LambdaQueryWrapper<StudentMemory>()
                    .eq(StudentMemory::getStudentId, studentId)
                    .isNull(StudentMemory::getCourseId)
                    .orderByDesc(StudentMemory::getVersion)
                    .last("LIMIT 1"));
        }
        return memory;
    }

    /** 供 prompt 注入的记忆文本 */
    public String getMemoryText(Long studentId, Long courseId) {
        StudentMemory memory = getLatest(studentId, courseId);
        return memory == null ? null : memory.getSummary();
    }

    /** 记忆历史（供前端展示/对比） */
    public List<StudentMemory> listHistory(Long studentId, Long courseId, int limit) {
        return memoryMapper.selectList(new LambdaQueryWrapper<StudentMemory>()
                .eq(StudentMemory::getStudentId, studentId)
                .eq(courseId != null, StudentMemory::getCourseId, courseId)
                .orderByDesc(StudentMemory::getVersion)
                .last("LIMIT " + (limit <= 0 ? 5 : Math.min(limit, 20))));
    }

    /**
     * 用当前库里的数据重新压缩一条记忆（版本号自增）。
     *
     * @param runId 产生该记忆的流水线运行ID（可为 null）
     */
    @Transactional
    public StudentMemory refreshMemory(Long studentId, Long courseId, Long runId) {
        StudentMemory previous = getLatest(studentId, courseId);
        int version = previous == null || previous.getVersion() == null ? 1 : previous.getVersion() + 1;
        String summary = buildSummary(studentId, courseId, version);

        StudentMemory memory = new StudentMemory();
        memory.setStudentId(studentId);
        memory.setCourseId(courseId);
        memory.setVersion(version);
        memory.setSummary(summary);
        memory.setSourceRunId(runId);
        memory.setCreateTime(java.time.LocalDateTime.now());
        memoryMapper.insert(memory);
        log.info("[StudentMemory] 学生 {} 课程 {} 记忆已刷新到 v{}", studentId, courseId, version);
        return memory;
    }

    /** 规则化压缩：从画像/策略/干预/目标/预警/计划拼出短记忆 */
    String buildSummary(Long studentId, Long courseId, int version) {
        StringBuilder sb = new StringBuilder();
        sb.append("【历史记忆 v").append(version).append("｜").append(LocalDate.now()).append("】\n");

        StudentProfile profile = firstOrNull(profileMapper.selectList(new LambdaQueryWrapper<StudentProfile>()
                .eq(StudentProfile::getStudentId, studentId)
                .eq(courseId != null, StudentProfile::getCourseId, courseId)
                .isNull(courseId == null, StudentProfile::getCourseId)
                .orderByDesc(StudentProfile::getUpdateTime)));
        if (profile != null) {
            sb.append("· 学情画像：知识=").append(levelOf(profile.getKnowledgeProfile()))
              .append("，习惯=").append(levelOf(profile.getHabitProfile()))
              .append("，目标=").append(levelOf(profile.getGoalProfile())).append("\n");
        }

        StrategyRecord strategy = firstOrNull(strategyMapper.selectList(new LambdaQueryWrapper<StrategyRecord>()
                .eq(StrategyRecord::getStudentId, studentId)
                .eq(courseId != null, StrategyRecord::getCourseId, courseId)
                .orderByDesc(StrategyRecord::getCreateTime)));
        if (strategy != null) {
            sb.append("· 上期策略：").append(trim(strategy.getNewStrategy(), 60))
              .append(strategy.getChangeReason() != null ? "（原因：" + trim(strategy.getChangeReason(), 40) + "）" : "")
              .append("\n");
        }

        InterventionRecord intervention = firstOrNull(interventionMapper.selectList(
                new LambdaQueryWrapper<InterventionRecord>()
                        .eq(InterventionRecord::getStudentId, studentId)
                        .orderByDesc(InterventionRecord::getCreateTime)));
        if (intervention != null) {
            sb.append("· 最近干预：").append(nvl(intervention.getInterventionType(), "未分类"))
              .append(" —— ").append(trim(intervention.getDescription(), 60)).append("\n");
        }

        StudentGoal goal = firstOrNull(goalMapper.selectList(new LambdaQueryWrapper<StudentGoal>()
                .eq(StudentGoal::getStudentId, studentId)
                .eq(courseId != null, StudentGoal::getCourseId, courseId)
                .orderByDesc(StudentGoal::getUpdateTime)));
        if (goal != null) {
            sb.append("· 学生自设目标：").append(nvl(goal.getGoalLevel(), "未设置"));
            if (goal.getTargetScore() != null) {
                sb.append("，目标分 ").append(goal.getTargetScore());
            }
            sb.append("\n");
        }

        AlertRecord alert = firstOrNull(alertRecordMapper.selectList(new LambdaQueryWrapper<AlertRecord>()
                .eq(AlertRecord::getStudentId, studentId)
                .eq(courseId != null, AlertRecord::getCourseId, courseId)
                .orderByDesc(AlertRecord::getCreateTime)));
        if (alert != null) {
            sb.append("· 最近预警：").append(nvl(alert.getAlertLevel(), "无"))
              .append("（风险分 ").append(alert.getRiskScore() != null ? alert.getRiskScore() : "N/A")
              .append("，状态 ").append(nvl(alert.getStatus(), "UNKNOWN")).append("）\n");
        }

        StudyPlan plan = firstOrNull(studyPlanMapper.selectList(new LambdaQueryWrapper<StudyPlan>()
                .eq(StudyPlan::getStudentId, studentId)
                .eq(courseId != null, StudyPlan::getCourseId, courseId)
                .orderByDesc(StudyPlan::getCreateTime)));
        if (plan != null) {
            sb.append("· 上次学习计划：planId=").append(plan.getId())
              .append("，状态=").append(nvl(plan.getStatus(), "UNKNOWN")).append("\n");
        }

        return sb.toString();
    }

    // ==================== 工具 ====================

    private <T> T firstOrNull(List<T> list) {
        return list == null || list.isEmpty() ? null : list.get(0);
    }

    /** 从画像 JSON 里取 level */
    private String levelOf(String profileJson) {
        if (profileJson == null || profileJson.isBlank()) {
            return "暂无";
        }
        Map<String, Object> parsed = JsonUtils.parse(profileJson);
        Object level = parsed.get("level");
        return level == null ? "暂无" : String.valueOf(level);
    }

    private String trim(String text, int max) {
        if (text == null || text.isBlank()) {
            return "无";
        }
        String t = text.trim();
        return t.length() <= max ? t : t.substring(0, max - 1) + "…";
    }

    private String nvl(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }
}
