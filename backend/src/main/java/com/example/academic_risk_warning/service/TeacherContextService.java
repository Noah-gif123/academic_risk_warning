package com.example.academic_risk_warning.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.academic_risk_warning.entity.*;
import com.example.academic_risk_warning.mapper.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 教师上下文数据服务：根据教师ID + 课程ID 聚合相关学情数据
 * 为教师端智能体提供真实数据上下文，用于生成推荐问题、数据分析、干预建议等
 */
@Service
public class TeacherContextService {

    private static final Logger log = LoggerFactory.getLogger(TeacherContextService.class);

    private final TeacherMapper teacherMapper;
    private final TeacherClassMapper teacherClassMapper;
    private final StudentMapper studentMapper;
    private final StudentCourseMapper studentCourseMapper;
    private final CourseMapper courseMapper;
    private final AlertRecordMapper alertRecordMapper;
    private final StudentWeakPointMapper studentWeakPointMapper;
    private final AlertRuleConfigMapper alertRuleConfigMapper;
    private final AlertSnapshotMapper alertSnapshotMapper;
    private final CourseKnowledgePointMapper courseKnowledgePointMapper;

    public TeacherContextService(TeacherMapper teacherMapper,
                                  TeacherClassMapper teacherClassMapper,
                                  StudentMapper studentMapper,
                                  StudentCourseMapper studentCourseMapper,
                                  CourseMapper courseMapper,
                                  AlertRecordMapper alertRecordMapper,
                                  StudentWeakPointMapper studentWeakPointMapper,
                                  AlertRuleConfigMapper alertRuleConfigMapper,
                                  AlertSnapshotMapper alertSnapshotMapper,
                                  CourseKnowledgePointMapper courseKnowledgePointMapper) {
        this.teacherMapper = teacherMapper;
        this.teacherClassMapper = teacherClassMapper;
        this.studentMapper = studentMapper;
        this.studentCourseMapper = studentCourseMapper;
        this.courseMapper = courseMapper;
        this.alertRecordMapper = alertRecordMapper;
        this.studentWeakPointMapper = studentWeakPointMapper;
        this.alertRuleConfigMapper = alertRuleConfigMapper;
        this.alertSnapshotMapper = alertSnapshotMapper;
        this.courseKnowledgePointMapper = courseKnowledgePointMapper;
    }

    // ========== 1. 课程风险概览 ==========

    /**
     * 获取指定课程的风险等级分布
     */
    public Map<String, Object> getRiskOverview(Long teacherId, Long courseId) {
        Map<String, Object> result = new LinkedHashMap<>();

        // 课程基本信息
        Course course = courseMapper.selectById(courseId);
        if (course != null) {
            result.put("courseName", course.getCourseName());
            result.put("passScore", course.getPassScore());
        }

        // 获取该课程下的所有学生ID
        List<Long> courseStudentIds = getCourseStudentIds(teacherId, courseId);
        result.put("totalStudents", courseStudentIds.size());

        if (courseStudentIds.isEmpty()) {
            result.put("alertDistribution", Map.of());
            result.put("alertList", List.of());
            result.put("topRiskStudents", List.of());
            return result;
        }

        // 查询该课程下所有有效预警记录
        List<AlertRecord> alerts = alertRecordMapper.selectList(
                new LambdaQueryWrapper<AlertRecord>()
                        .eq(AlertRecord::getCourseId, courseId)
                        .in(AlertRecord::getStudentId, courseStudentIds)
                        .eq(AlertRecord::getStatus, "ACTIVE")
                        .orderByDesc(AlertRecord::getRiskScore));

        // 各等级分布
        long redCount = alerts.stream().filter(a -> "RED".equals(a.getAlertLevel())).count();
        long orangeCount = alerts.stream().filter(a -> "ORANGE".equals(a.getAlertLevel())).count();
        long yellowCount = alerts.stream().filter(a -> "YELLOW".equals(a.getAlertLevel())).count();
        long greenCount = courseStudentIds.size() - redCount - orangeCount - yellowCount;

        Map<String, Object> distribution = new LinkedHashMap<>();
        distribution.put("RED", Map.of("count", redCount, "label", "红色预警"));
        distribution.put("ORANGE", Map.of("count", orangeCount, "label", "橙色预警"));
        distribution.put("YELLOW", Map.of("count", yellowCount, "label", "黄色预警"));
        distribution.put("GREEN", Map.of("count", Math.max(0, greenCount), "label", "正常"));
        result.put("alertDistribution", distribution);

        // Top 10 风险学生
        Map<Long, Student> studentMap = studentMapper.selectBatchIds(courseStudentIds)
                .stream().collect(Collectors.toMap(Student::getId, s -> s));
        List<Map<String, Object>> topRisk = alerts.stream()
                .limit(10)
                .map(a -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("studentId", a.getStudentId());
                    m.put("studentName", studentMap.containsKey(a.getStudentId())
                            ? studentMap.get(a.getStudentId()).getStudentName() : "未知");
                    m.put("alertLevel", a.getAlertLevel());
                    m.put("alertType", a.getAlertType());
                    m.put("riskScore", a.getRiskScore());
                    m.put("alertReason", a.getAlertReason());
                    m.put("academicRiskScore", a.getAcademicRiskScore());
                    m.put("homeworkRiskScore", a.getHomeworkRiskScore());
                    m.put("attendanceRiskScore", a.getAttendanceRiskScore());
                    m.put("knowledgeRiskScore", a.getKnowledgeRiskScore());
                    return m;
                }).collect(Collectors.toList());
        result.put("topRiskStudents", topRisk);

        return result;
    }

    // ========== 2. 课程知识点薄弱项 ==========

    /**
     * 获取指定课程下所有学生的薄弱知识点汇总（按错误率降序）
     */
    public List<Map<String, Object>> getCourseWeakPoints(Long teacherId, Long courseId) {
        List<Long> courseStudentIds = getCourseStudentIds(teacherId, courseId);
        if (courseStudentIds.isEmpty()) {
            return List.of();
        }

        // 查询该课程所有学生的薄弱点
        List<StudentWeakPoint> weakPoints = studentWeakPointMapper.selectList(
                new LambdaQueryWrapper<StudentWeakPoint>()
                        .eq(StudentWeakPoint::getCourseId, courseId)
                        .in(StudentWeakPoint::getStudentId, courseStudentIds));

        // 查询所有知识点名称
        List<CourseKnowledgePoint> kps = courseKnowledgePointMapper.selectList(
                new LambdaQueryWrapper<CourseKnowledgePoint>()
                        .eq(CourseKnowledgePoint::getCourseId, courseId));
        Map<Long, String> kpNameMap = kps.stream()
                .collect(Collectors.toMap(CourseKnowledgePoint::getId, CourseKnowledgePoint::getName, (a, b) -> a));

        // 按知识点聚合：统计每个知识点的薄弱学生数、平均错误率
        Map<Long, List<StudentWeakPoint>> grouped = weakPoints.stream()
                .filter(w -> w.getKpId() != null)
                .collect(Collectors.groupingBy(StudentWeakPoint::getKpId));

        List<Map<String, Object>> result = grouped.entrySet().stream()
                .map(entry -> {
                    Long kpId = entry.getKey();
                    List<StudentWeakPoint> list = entry.getValue();
                    double avgErrorRate = list.stream()
                            .filter(w -> w.getErrorRate() != null)
                            .mapToDouble(w -> w.getErrorRate().doubleValue())
                            .average().orElse(0);
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("knowledgePointId", kpId);
                    m.put("knowledgePointName", kpNameMap.getOrDefault(kpId, "知识点" + kpId));
                    m.put("weakStudentCount", list.size());
                    m.put("avgErrorRate", Math.round(avgErrorRate * 10) / 10.0);
                    m.put("studentDetails", list.stream().limit(5).map(w -> {
                        Map<String, Object> sd = new LinkedHashMap<>();
                        sd.put("studentId", w.getStudentId());
                        sd.put("errorRate", w.getErrorRate());
                        return sd;
                    }).collect(Collectors.toList()));
                    return m;
                })
                .sorted((a, b) -> Double.compare(
                        (Double) b.get("avgErrorRate"), (Double) a.get("avgErrorRate")))
                .collect(Collectors.toList());

        return result;
    }

    // ========== 3. 预警规则配置 ==========

    /**
     * 获取指定课程的预警规则配置
     */
    public List<Map<String, Object>> getAlertRules(Long courseId) {
        List<AlertRuleConfig> configs = alertRuleConfigMapper.selectList(
                new LambdaQueryWrapper<AlertRuleConfig>()
                        .eq(AlertRuleConfig::getCourseId, courseId)
                        .orderByAsc(AlertRuleConfig::getId));

        // 课程无专属规则时，回退到体系默认规则（course_id IS NULL）
        if (configs.isEmpty()) {
            configs = alertRuleConfigMapper.selectList(
                    new LambdaQueryWrapper<AlertRuleConfig>()
                            .isNull(AlertRuleConfig::getCourseId)
                            .orderByAsc(AlertRuleConfig::getId));
        }

        return configs.stream().map(c -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", c.getId());
            m.put("systemType", c.getSystemType());
            m.put("courseId", c.getCourseId());
            m.put("weightAcademic", c.getWeightAcademic());
            m.put("weightHomework", c.getWeightHomework());
            m.put("weightKnowledge", c.getWeightKnowledge());
            m.put("weightAttendance", c.getWeightAttendance());
            m.put("weightHistory", c.getWeightHistory());
            m.put("thresholdRed", c.getThresholdRed());
            m.put("thresholdOrange", c.getThresholdOrange());
            m.put("thresholdYellow", c.getThresholdYellow());
            m.put("homeworkSubmitRateThreshold", c.getHomeworkSubmitRateThreshold());
            m.put("knowledgeCorrectRateThreshold", c.getKnowledgeCorrectRateThreshold());
            m.put("attendanceRateThreshold", c.getAttendanceRateThreshold());
            m.put("scoreDropThreshold", c.getScoreDropThreshold());
            m.put("predictedPassScore", c.getPredictedPassScore());
            return m;
        }).collect(Collectors.toList());
    }

    // ========== 4. 动态推荐问题生成 ==========

    /**
     * 根据教师课程的实际数据动态生成推荐问题
     */
    public List<String> getTeacherSuggestions(Long teacherId, Long courseId) {
        List<String> suggestions = new ArrayList<>();

        // 获取风险概览数据
        Map<String, Object> overview = getRiskOverview(teacherId, courseId);
        @SuppressWarnings("unchecked")
        Map<String, Object> distribution = (Map<String, Object>) overview.get("alertDistribution");

        if (distribution != null) {
            @SuppressWarnings("unchecked")
            Map<String, Object> red = (Map<String, Object>) distribution.get("RED");
            @SuppressWarnings("unchecked")
            Map<String, Object> orange = (Map<String, Object>) distribution.get("ORANGE");
            @SuppressWarnings("unchecked")
            Map<String, Object> yellow = (Map<String, Object>) distribution.get("YELLOW");

            long redCount = red != null ? ((Number) red.get("count")).longValue() : 0;
            long orangeCount = orange != null ? ((Number) orange.get("count")).longValue() : 0;

            String courseName = Objects.toString(overview.get("courseName"), "当前课程");

            if (redCount > 0) {
                suggestions.add(String.format("%s中%d名红色预警学生的主要风险来源是什么？",
                        courseName, redCount));
                suggestions.add(String.format("针对%d名红色预警学生，建议采取什么干预措施？", redCount));
            }
            if (orangeCount > 0) {
                suggestions.add(String.format("%s中%d名橙色预警学生需要关注哪些方面？", courseName, orangeCount));
            }
            if (redCount == 0 && orangeCount == 0 && yellow != null) {
                long yc = ((Number) yellow.get("count")).longValue();
                if (yc > 0) {
                    suggestions.add(String.format("%s整体风险较低，如何巩固学生薄弱知识点？", courseName));
                }
            }
        }

        // 薄弱知识点相关
        List<Map<String, Object>> weakPoints = getCourseWeakPoints(teacherId, courseId);
        if (!weakPoints.isEmpty()) {
            Map<String, Object> topKp = weakPoints.get(0);
            String kpName = Objects.toString(topKp.get("knowledgePointName"), "");
            suggestions.add(String.format("针对「%s」的高错误率，有哪些有效的教学补救方法？", kpName));

            if (weakPoints.size() > 1) {
                suggestions.add(String.format("当前课程有哪些知识点薄弱？应该如何调整教学重心？"));
            }
        }

        // 通用问题兜底
        suggestions.add("如何根据学生风险等级合理分配教学资源？");
        suggestions.add("预警等级是如何计算的？各维度权重是怎样的？");

        // 上限 6 个
        return suggestions.size() > 6 ? suggestions.subList(0, 6) : suggestions;
    }

    // ========== 5. 教师基础信息 ==========

    /**
     * 获取教师基本信息
     */
    public Teacher getTeacher(Long teacherId) {
        return teacherMapper.selectById(teacherId);
    }

    /**
     * 获取教师授课的课程列表
     */
    public List<Course> getTeacherCourses(Long teacherId) {
        List<TeacherClass> classes = teacherClassMapper.selectList(
                new LambdaQueryWrapper<TeacherClass>()
                        .eq(TeacherClass::getTeacherId, teacherId));

        List<String> classNames = classes.stream()
                .map(TeacherClass::getClassName)
                .distinct()
                .collect(Collectors.toList());

        if (classNames.isEmpty()) return List.of();

        // 通过班级名称关联学生，再关联课程
        List<Student> students = studentMapper.selectList(
                new LambdaQueryWrapper<Student>()
                        .in(Student::getClassName, classNames));

        if (students.isEmpty()) return List.of();

        List<Long> studentIds = students.stream().map(Student::getId).collect(Collectors.toList());
        List<StudentCourse> scs = studentCourseMapper.selectList(
                new LambdaQueryWrapper<StudentCourse>()
                        .in(StudentCourse::getStudentId, studentIds));

        List<Long> courseIds = scs.stream().map(StudentCourse::getCourseId).distinct().collect(Collectors.toList());
        if (courseIds.isEmpty()) return List.of();

        return courseMapper.selectBatchIds(courseIds);
    }

    // ========== 6. 干预方案数据 ==========

    /**
     * 获取课程中需要干预的高风险学生列表及其详细信息
     */
    public Map<String, Object> getInterventionData(Long teacherId, Long courseId) {
        Map<String, Object> overview = getRiskOverview(teacherId, courseId);
        List<Map<String, Object>> weakPoints = getCourseWeakPoints(teacherId, courseId);
        List<Map<String, Object>> rules = getAlertRules(courseId);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("riskOverview", overview);
        result.put("topWeakPoints", weakPoints.size() > 5 ? weakPoints.subList(0, 5) : weakPoints);
        result.put("alertRules", rules);
        result.put("courseId", courseId);
        result.put("teacherId", teacherId);
        return result;
    }

    // ========== 辅助方法 ==========

    /**
     * 获取教师某课程下的所有学生ID
     */
    private List<Long> getCourseStudentIds(Long teacherId, Long courseId) {
        if (teacherId == null || courseId == null) return List.of();

        // 1. 查教师负责的班级
        List<TeacherClass> classes = teacherClassMapper.selectList(
                new LambdaQueryWrapper<TeacherClass>()
                        .eq(TeacherClass::getTeacherId, teacherId));
        if (classes.isEmpty()) return List.of();

        // 2. 收集班级名称
        List<String> classNames = classes.stream()
                .map(TeacherClass::getClassName)
                .distinct()
                .collect(Collectors.toList());

        // 3. 查这些班级的所有学生
        List<Student> classStudents = studentMapper.selectList(
                new LambdaQueryWrapper<Student>()
                        .in(Student::getClassName, classNames));
        if (classStudents.isEmpty()) return List.of();

        // 4. 查选了这个课程的学生
        List<Long> allStudentIds = classStudents.stream().map(Student::getId).collect(Collectors.toList());
        List<StudentCourse> scs = studentCourseMapper.selectList(
                new LambdaQueryWrapper<StudentCourse>()
                        .eq(StudentCourse::getCourseId, courseId)
                        .in(StudentCourse::getStudentId, allStudentIds));

        return scs.stream().map(StudentCourse::getStudentId).distinct().collect(Collectors.toList());
    }
}
