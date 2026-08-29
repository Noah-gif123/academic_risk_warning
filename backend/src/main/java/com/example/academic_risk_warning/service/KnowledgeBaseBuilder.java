package com.example.academic_risk_warning.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.academic_risk_warning.entity.Course;
import com.example.academic_risk_warning.entity.CourseKnowledgePoint;
import com.example.academic_risk_warning.entity.Exercise;
import com.example.academic_risk_warning.mapper.CourseKnowledgePointMapper;
import com.example.academic_risk_warning.mapper.CourseMapper;
import com.example.academic_risk_warning.mapper.ExerciseMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 知识库构建器：从数据库自动生成课程知识文本，无需外部教材
 *
 * 三层知识体系：
 *   第一层：知识点骨架 (CourseKnowledgePoint)
 *   第二层：题库知识 (Exercise)
 *   第三层：教师扩展资料（后续可扩展）
 *
 * 生成的文本可直接上传至百炼知识库应用
 */
@Service
public class KnowledgeBaseBuilder {

    private static final Logger log = LoggerFactory.getLogger(KnowledgeBaseBuilder.class);

    private final CourseKnowledgePointMapper kpMapper;
    private final ExerciseMapper exerciseMapper;
    private final CourseMapper courseMapper;

    public KnowledgeBaseBuilder(CourseKnowledgePointMapper kpMapper,
                                 ExerciseMapper exerciseMapper,
                                 CourseMapper courseMapper) {
        this.kpMapper = kpMapper;
        this.exerciseMapper = exerciseMapper;
        this.courseMapper = courseMapper;
    }

    /**
     * 构建指定课程的完整知识库文本
     * 包含：课程信息 + 知识点体系 + 题库索引
     *
     * @param courseId 课程ID
     * @return 知识库文本，可直接上传百炼
     */
    public String buildCourseKnowledgeText(Long courseId) {
        Course course = courseMapper.selectById(courseId);
        String courseName = course != null ? course.getCourseName() : "未知课程";

        StringBuilder sb = new StringBuilder();

        // ===== 课程标题 =====
        sb.append("=== ").append(courseName).append(" 知识库 ===\n\n");

        // ===== 第一层：知识点体系 =====
        sb.append(buildKnowledgeTree(courseId));

        // ===== 第二层：题库索引 =====
        sb.append(buildExerciseIndex(courseId));

        log.info("[KnowledgeBaseBuilder] 课程「{}」知识库文本生成完成，共 {} 字符",
                courseName, sb.length());
        return sb.toString();
    }

    /**
     * 构建知识点体系文本（第一层）
     */
    public String buildKnowledgeTree(Long courseId) {
        List<CourseKnowledgePoint> allKps = kpMapper.selectList(
                new LambdaQueryWrapper<CourseKnowledgePoint>()
                        .eq(CourseKnowledgePoint::getCourseId, courseId)
                        .orderByAsc(CourseKnowledgePoint::getSortOrder));

        if (allKps.isEmpty()) return "（该课程暂无知识点）\n\n";

        // 按 parentId 分组
        Map<Long, List<CourseKnowledgePoint>> childrenMap = new HashMap<>();
        List<CourseKnowledgePoint> roots = new ArrayList<>();

        for (CourseKnowledgePoint kp : allKps) {
            if (kp.getParentId() == null) {
                roots.add(kp);
            } else {
                childrenMap.computeIfAbsent(kp.getParentId(), k -> new ArrayList<>()).add(kp);
            }
        }

        StringBuilder sb = new StringBuilder();
        sb.append("--- 知识点体系 ---\n\n");

        for (CourseKnowledgePoint root : roots) {
            // 一级知识点
            sb.append("■ ").append(root.getName());
            if (root.getDescription() != null && !root.getDescription().isBlank()) {
                sb.append("：").append(root.getDescription());
            }
            sb.append("\n");

            // 二级知识点
            List<CourseKnowledgePoint> children = childrenMap.get(root.getId());
            if (children != null) {
                for (CourseKnowledgePoint child : children) {
                    sb.append("  □ ").append(child.getName());
                    if (child.getDescription() != null && !child.getDescription().isBlank()) {
                        sb.append("：").append(child.getDescription());
                    }
                    sb.append("\n");
                }
            }
            sb.append("\n");
        }

        return sb.toString();
    }

    /**
     * 构建题库索引文本（第二层）
     */
    public String buildExerciseIndex(Long courseId) {
        List<Exercise> exercises = exerciseMapper.selectList(
                new LambdaQueryWrapper<Exercise>()
                        .eq(Exercise::getCourseId, courseId)
                        .eq(Exercise::getIsActive, 1)
                        .orderByAsc(Exercise::getKpId));

        if (exercises.isEmpty()) return "--- 题库 ---\n（暂无练习题）\n";

        // 按知识点分组
        Map<Long, List<Exercise>> grouped = exercises.stream()
                .collect(Collectors.groupingBy(
                        e -> e.getKpId() != null ? e.getKpId() : 0L,
                        LinkedHashMap::new,
                        Collectors.toList()));

        // 批量查知识点名称
        Set<Long> kpIds = new HashSet<>(grouped.keySet());
        kpIds.remove(0L);
        Map<Long, String> kpNameMap = new HashMap<>();
        if (!kpIds.isEmpty()) {
            List<CourseKnowledgePoint> kps = kpMapper.selectBatchIds(kpIds);
            kps.forEach(kp -> kpNameMap.put(kp.getId(), kp.getName()));
        }

        StringBuilder sb = new StringBuilder();
        sb.append("--- 题库 ---\n\n");

        int globalIndex = 1;
        for (Map.Entry<Long, List<Exercise>> entry : grouped.entrySet()) {
            Long kpId = entry.getKey();
            String kpName = kpNameMap.getOrDefault(kpId, "通用");
            List<Exercise> kpExercises = entry.getValue();

            sb.append("【").append(kpName).append("】（共").append(kpExercises.size()).append("题）\n");

            for (Exercise ex : kpExercises) {
                sb.append("Q").append(String.format("%03d", globalIndex)).append(": ");
                sb.append(ex.getTitle()).append("\n");

                if (ex.getDescription() != null && !ex.getDescription().isBlank()) {
                    sb.append("  题目描述: ").append(ex.getDescription()).append("\n");
                }

                if (ex.getAnswer() != null && !ex.getAnswer().isBlank()) {
                    sb.append("  答案: ").append(ex.getAnswer()).append("\n");
                }

                if (ex.getDifficulty() != null) {
                    sb.append("  难度: ").append(difficultyName(ex.getDifficulty())).append("\n");
                }

                sb.append("\n");
                globalIndex++;
            }
        }

        return sb.toString();
    }

    /**
     * 构建指定课程的轻量版知识文本（仅知识点体系，不含题库）
     * 用于快速预览或首次初始化
     */
    public String buildKnowledgeTreeOnly(Long courseId) {
        Course course = courseMapper.selectById(courseId);
        String courseName = course != null ? course.getCourseName() : "未知课程";

        StringBuilder sb = new StringBuilder();
        sb.append("=== ").append(courseName).append(" 知识点体系 ===\n\n");
        sb.append(buildKnowledgeTree(courseId));
        return sb.toString();
    }

    /**
     * 获取所有课程的知识库文本列表
     */
    public Map<Long, String> buildAllCourses() {
        Map<Long, String> result = new LinkedHashMap<>();
        List<Course> courses = courseMapper.selectList(null);
        for (Course course : courses) {
            result.put(course.getId(), buildCourseKnowledgeText(course.getId()));
        }
        log.info("[KnowledgeBaseBuilder] 批量构建完成，共 {} 门课程", courses.size());
        return result;
    }

    /**
     * 构建指定课程、指定知识点下的题库文本
     * 用于增量更新某知识点的新题目
     */
    public String buildExerciseIndexForKp(Long courseId, Long kpId) {
        CourseKnowledgePoint kp = kpMapper.selectById(kpId);
        String kpName = kp != null ? kp.getName() : "未知知识点";

        List<Exercise> exercises = exerciseMapper.selectList(
                new LambdaQueryWrapper<Exercise>()
                        .eq(Exercise::getCourseId, courseId)
                        .eq(Exercise::getKpId, kpId)
                        .eq(Exercise::getIsActive, 1));

        if (exercises.isEmpty()) return "";

        StringBuilder sb = new StringBuilder();
        sb.append("【").append(kpName).append("】新增题目\n");

        int idx = 1;
        for (Exercise ex : exercises) {
            sb.append("Q").append(idx).append(": ").append(ex.getTitle()).append("\n");
            if (ex.getAnswer() != null && !ex.getAnswer().isBlank()) {
                sb.append("A").append(idx).append(": ").append(ex.getAnswer()).append("\n");
            }
            idx++;
        }

        return sb.toString();
    }

    private String difficultyName(String difficulty) {
        return switch (difficulty != null ? difficulty.toUpperCase() : "") {
            case "EASY" -> "简单";
            case "MEDIUM" -> "中等";
            case "HARD" -> "困难";
            default -> difficulty;
        };
    }
}
