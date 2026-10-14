package com.example.academic_risk_warning.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.academic_risk_warning.entity.*;
import com.example.academic_risk_warning.mapper.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 学生上下文数据服务：为AI助手提供学生个人的学情数据上下文
 * 面向单个学生，只查询该学生自己的数据
 */
@Service
public class StudentContextService {

    private static final Logger log = LoggerFactory.getLogger(StudentContextService.class);

    private final StudentMapper studentMapper;
    private final StudentCourseMapper studentCourseMapper;
    private final CourseMapper courseMapper;
    private final AlertRecordMapper alertRecordMapper;
    private final StudentWeakPointMapper studentWeakPointMapper;
    private final CourseKnowledgePointMapper courseKnowledgePointMapper;
    private final ScoreInfoMapper scoreInfoMapper;
    private final HomeworkInfoMapper homeworkInfoMapper;
    private final ClassPerformanceMapper classPerformanceMapper;
    private final KnowledgeMasteryMapper knowledgeMasteryMapper;
    private final AlertSnapshotMapper alertSnapshotMapper;

    public StudentContextService(StudentMapper studentMapper,
                                  StudentCourseMapper studentCourseMapper,
                                  CourseMapper courseMapper,
                                  AlertRecordMapper alertRecordMapper,
                                  StudentWeakPointMapper studentWeakPointMapper,
                                  CourseKnowledgePointMapper courseKnowledgePointMapper,
                                  ScoreInfoMapper scoreInfoMapper,
                                  HomeworkInfoMapper homeworkInfoMapper,
                                  ClassPerformanceMapper classPerformanceMapper,
                                  KnowledgeMasteryMapper knowledgeMasteryMapper,
                                  AlertSnapshotMapper alertSnapshotMapper) {
        this.studentMapper = studentMapper;
        this.studentCourseMapper = studentCourseMapper;
        this.courseMapper = courseMapper;
        this.alertRecordMapper = alertRecordMapper;
        this.studentWeakPointMapper = studentWeakPointMapper;
        this.courseKnowledgePointMapper = courseKnowledgePointMapper;
        this.scoreInfoMapper = scoreInfoMapper;
        this.homeworkInfoMapper = homeworkInfoMapper;
        this.classPerformanceMapper = classPerformanceMapper;
        this.knowledgeMasteryMapper = knowledgeMasteryMapper;
        this.alertSnapshotMapper = alertSnapshotMapper;
    }

    /**
     * 获取学生选修的课程列表
     */
    public List<Map<String, Object>> getStudentCourses(Long studentId) {
        List<StudentCourse> scs = studentCourseMapper.selectList(
                new LambdaQueryWrapper<StudentCourse>()
                        .eq(StudentCourse::getStudentId, studentId));
        if (scs.isEmpty()) return List.of();

        List<Long> courseIds = scs.stream().map(StudentCourse::getCourseId).distinct().toList();
        Map<Long, Course> courseMap = courseMapper.selectBatchIds(courseIds).stream()
                .collect(Collectors.toMap(Course::getId, c -> c));

        return courseIds.stream().map(id -> {
            Course c = courseMap.get(id);
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("courseId", id);
            m.put("courseName", c != null ? c.getCourseName() : "未知课程");
            m.put("passScore", c != null ? c.getPassScore() : 60);
            return m;
        }).collect(Collectors.toList());
    }

    /**
     * 获取学生的个人风险诊断数据（按课程）
     */
    public Map<String, Object> getStudentRiskProfile(Long studentId, Long courseId) {
        Map<String, Object> result = new LinkedHashMap<>();

        Student student = studentMapper.selectById(studentId);
        if (student != null) {
            result.put("studentName", student.getStudentName());
            result.put("grade", student.getGrade());
            result.put("className", student.getClassName());
        }

        Course course = courseId != null ? courseMapper.selectById(courseId) : null;
        if (course != null) {
            result.put("courseName", course.getCourseName());
            result.put("passScore", course.getPassScore());
        }

        // 查询该学生在该课程的 ACTIVE 预警
        LambdaQueryWrapper<AlertRecord> wrapper = new LambdaQueryWrapper<AlertRecord>()
                .eq(AlertRecord::getStudentId, studentId)
                .eq(AlertRecord::getStatus, "ACTIVE")
                .orderByDesc(AlertRecord::getRiskScore);
        if (courseId != null) {
            wrapper.eq(AlertRecord::getCourseId, courseId);
        }

        List<AlertRecord> alerts = alertRecordMapper.selectList(wrapper);
        List<Map<String, Object>> alertList = alerts.stream().map(a -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("alertLevel", a.getAlertLevel());
            m.put("alertLevelName", levelName(a.getAlertLevel()));
            m.put("alertType", a.getAlertType());
            m.put("alertTypeName", typeName(a.getAlertType()));
            m.put("riskScore", a.getRiskScore());
            m.put("predictedScore", a.getPredictedScore());
            m.put("alertReason", a.getAlertReason());
            m.put("academicRiskScore", a.getAcademicRiskScore());
            m.put("homeworkRiskScore", a.getHomeworkRiskScore());
            m.put("attendanceRiskScore", a.getAttendanceRiskScore());
            m.put("knowledgeRiskScore", a.getKnowledgeRiskScore());
            m.put("historyRiskScore", a.getHistoryRiskScore());
            return m;
        }).collect(Collectors.toList());

        result.put("alerts", alertList);
        result.put("totalAlerts", alerts.size());

        // 最高风险等级/风险分：优先 ACTIVE 预警；没有预警时退回最新每日快照
        // （方案A之后每个选了课的学生每天都有快照，不能再用 0 冒充"风险分"）
        if (!alerts.isEmpty()) {
            AlertRecord top = alerts.get(0);
            result.put("highestLevel", top.getAlertLevel());
            result.put("highestLevelName", levelName(top.getAlertLevel()));
            result.put("highestRiskScore", top.getRiskScore());
            result.put("riskSource", "ALERT");
        } else {
            AlertSnapshot snapshot = latestSnapshot(studentId, courseId);
            if (snapshot != null) {
                result.put("highestLevel", snapshot.getAlertLevel());
                result.put("highestLevelName", levelName(snapshot.getAlertLevel()));
                result.put("highestRiskScore", snapshot.getRiskScore());
                result.put("snapshotDate", snapshot.getSnapshotDate());
                result.put("riskSource", "SNAPSHOT");
            } else {
                result.put("highestLevel", "NONE");
                result.put("highestLevelName", "暂无数据");
                result.put("highestRiskScore", null);
                result.put("riskSource", "NONE");
            }
        }

        return result;
    }

    /** 取该生(指定课程优先，其次综合快照)的最新一条每日快照 */
    private AlertSnapshot latestSnapshot(Long studentId, Long courseId) {
        AlertSnapshot snapshot = alertSnapshotMapper.selectOne(
                new LambdaQueryWrapper<AlertSnapshot>()
                        .eq(AlertSnapshot::getStudentId, studentId)
                        .eq(courseId != null, AlertSnapshot::getCourseId, courseId)
                        .orderByDesc(AlertSnapshot::getSnapshotDate)
                        .orderByDesc(AlertSnapshot::getId)
                        .last("LIMIT 1"));
        if (snapshot == null && courseId != null) {
            snapshot = alertSnapshotMapper.selectOne(
                    new LambdaQueryWrapper<AlertSnapshot>()
                            .eq(AlertSnapshot::getStudentId, studentId)
                            .isNull(AlertSnapshot::getCourseId)
                            .orderByDesc(AlertSnapshot::getSnapshotDate)
                            .orderByDesc(AlertSnapshot::getId)
                            .last("LIMIT 1"));
        }
        return snapshot;
    }

    /**
     * 获取学生的薄弱知识点
     */
    public List<Map<String, Object>> getStudentWeakPoints(Long studentId, Long courseId) {
        LambdaQueryWrapper<StudentWeakPoint> wrapper = new LambdaQueryWrapper<StudentWeakPoint>()
                .eq(StudentWeakPoint::getStudentId, studentId)
                .orderByDesc(StudentWeakPoint::getErrorRate);
        if (courseId != null) {
            wrapper.eq(StudentWeakPoint::getCourseId, courseId);
        }

        List<StudentWeakPoint> weakPoints = studentWeakPointMapper.selectList(wrapper);
        if (weakPoints.isEmpty()) return List.of();

        // 批量查知识点名称
        Set<Long> kpIds = weakPoints.stream().map(StudentWeakPoint::getKpId).collect(Collectors.toSet());
        Map<Long, String> kpNameMap = new HashMap<>();
        for (Long kpId : kpIds) {
            CourseKnowledgePoint kp = courseKnowledgePointMapper.selectById(kpId);
            kpNameMap.put(kpId, kp != null ? kp.getName() : "未知知识点");
        }

        return weakPoints.stream().map(wp -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("kpId", wp.getKpId());
            m.put("kpName", kpNameMap.getOrDefault(wp.getKpId(), "未知"));
            m.put("errorRate", wp.getErrorRate());
            m.put("errorCount", wp.getErrorCount());
            return m;
        }).collect(Collectors.toList());
    }

    /**
     * 获取学生的学业成绩数据
     */
    public Map<String, Object> getStudentScoreInfo(Long studentId, Long courseId) {
        Map<String, Object> result = new LinkedHashMap<>();

        LambdaQueryWrapper<ScoreInfo> scoreWrapper = new LambdaQueryWrapper<ScoreInfo>()
                .eq(ScoreInfo::getStudentId, studentId);
        if (courseId != null) scoreWrapper.eq(ScoreInfo::getCourseId, courseId);

        List<ScoreInfo> scores = scoreInfoMapper.selectList(scoreWrapper);

        LambdaQueryWrapper<HomeworkInfo> hwWrapper = new LambdaQueryWrapper<HomeworkInfo>()
                .eq(HomeworkInfo::getStudentId, studentId);
        if (courseId != null) hwWrapper.eq(HomeworkInfo::getCourseId, courseId);

        List<HomeworkInfo> homeworks = homeworkInfoMapper.selectList(hwWrapper);

        LambdaQueryWrapper<ClassPerformance> cpWrapper = new LambdaQueryWrapper<ClassPerformance>()
                .eq(ClassPerformance::getStudentId, studentId);
        if (courseId != null) cpWrapper.eq(ClassPerformance::getCourseId, courseId);

        List<ClassPerformance> performances = classPerformanceMapper.selectList(cpWrapper);

        LambdaQueryWrapper<KnowledgeMastery> kmWrapper = new LambdaQueryWrapper<KnowledgeMastery>()
                .eq(KnowledgeMastery::getStudentId, studentId);
        if (courseId != null) kmWrapper.eq(KnowledgeMastery::getCourseId, courseId);

        List<KnowledgeMastery> masteries = knowledgeMasteryMapper.selectList(kmWrapper);

        // 汇总成绩
        if (!scores.isEmpty()) {
            ScoreInfo s = scores.get(0);
            result.put("usualScore", s.getUsualScore());
            result.put("midScore", s.getMidScore());
            result.put("finalScore", s.getFinalScore());
        }

        // 汇总作业
        if (!homeworks.isEmpty()) {
            HomeworkInfo hw = homeworks.get(0);
            result.put("totalHomework", hw.getTotalHomework());
            result.put("submitCount", hw.getSubmitCount());
            result.put("notSubmitCount", hw.getNotSubmitCount());
            result.put("lateSubmitCount", hw.getLateSubmitCount());
            result.put("avgHomeworkScore", hw.getAvgScore());
            if (hw.getTotalHomework() != null && hw.getTotalHomework() > 0) {
                result.put("submitRate", Math.round(hw.getSubmitCount() * 100.0 / hw.getTotalHomework() * 10) / 10.0);
            }
        }

        // 汇总课堂表现
        if (!performances.isEmpty()) {
            ClassPerformance cp = performances.get(0);
            result.put("totalClassTimes", cp.getTotalClassTimes());
            result.put("absentCount", cp.getAbsentCount());
            result.put("lateCount", cp.getLateCount());
            result.put("quizScore", cp.getQuizScore());
            if (cp.getTotalClassTimes() != null && cp.getTotalClassTimes() > 0) {
                int present = cp.getTotalClassTimes() - (cp.getAbsentCount() != null ? cp.getAbsentCount() : 0);
                result.put("attendanceRate", Math.round(present * 100.0 / cp.getTotalClassTimes() * 10) / 10.0);
            }
        }

        // 汇总知识点掌握
        if (!masteries.isEmpty()) {
            KnowledgeMastery km = masteries.get(0);
            result.put("totalQuestion", km.getTotalQuestion());
            result.put("errorCount", km.getErrorCount());
            result.put("basicTotal", km.getBasicTotal());
            result.put("basicCorrect", km.getBasicCorrect());
            if (km.getTotalQuestion() != null && km.getTotalQuestion() > 0) {
                int correct = km.getTotalQuestion() - (km.getErrorCount() != null ? km.getErrorCount() : 0);
                result.put("correctRate", Math.round(correct * 100.0 / km.getTotalQuestion() * 10) / 10.0);
            }
        }

        return result;
    }

    /**
     * 构建完整的学生学情上下文文本，用于注入LLM prompt
     */
    public String buildStudentContext(Long studentId, Long courseId) {
        StringBuilder ctx = new StringBuilder();

        // 基本信息
        Student student = studentMapper.selectById(studentId);
        if (student != null) {
            ctx.append("学生姓名：").append(student.getStudentName()).append("\n");
            ctx.append("年级：").append(student.getGrade()).append("\n");
            ctx.append("班级：").append(student.getClassName()).append("\n");
        }

        // 课程信息
        if (courseId != null) {
            Course course = courseMapper.selectById(courseId);
            if (course != null) {
                ctx.append("课程：").append(course.getCourseName()).append("\n");
                ctx.append("及格线：").append(course.getPassScore()).append("分\n");
            }
        }
        ctx.append("\n");

        // 风险诊断
        Map<String, Object> risk = getStudentRiskProfile(studentId, courseId);
        ctx.append("【风险状态】\n");
        ctx.append("最高风险等级：").append(risk.get("highestLevelName")).append("\n");
        Object highestRisk = risk.get("highestRiskScore");
        ctx.append("最高风险分：").append(highestRisk != null ? highestRisk : "暂无").append("\n");
        if ("SNAPSHOT".equals(risk.get("riskSource"))) {
            ctx.append("（以上风险分来自最近的每日快照：").append(risk.get("snapshotDate")).append("）\n");
        }
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> alerts = (List<Map<String, Object>>) risk.get("alerts");
        if (alerts != null && !alerts.isEmpty()) {
            ctx.append("详细预警：\n");
            for (Map<String, Object> a : alerts) {
                ctx.append("  - ").append(a.get("alertLevelName"))
                   .append("（").append(a.get("alertTypeName")).append("）")
                   .append(" 风险分：").append(a.get("riskScore"))
                   .append(" 原因：").append(a.get("alertReason")).append("\n");
                // 维度分值
                ctx.append("    学业").append(a.get("academicRiskScore"))
                   .append(" 作业").append(a.get("homeworkRiskScore"))
                   .append(" 出勤").append(a.get("attendanceRiskScore"))
                   .append(" 知识").append(a.get("knowledgeRiskScore"))
                   .append(" 历史").append(a.get("historyRiskScore")).append("\n");
            }
        } else {
            ctx.append("当前暂无处理中的预警。\n");
        }
        ctx.append("\n");

        // 薄弱知识点
        List<Map<String, Object>> weakPoints = getStudentWeakPoints(studentId, courseId);
        ctx.append("【薄弱知识点】\n");
        if (!weakPoints.isEmpty()) {
            for (int i = 0; i < Math.min(10, weakPoints.size()); i++) {
                Map<String, Object> wp = weakPoints.get(i);
                ctx.append("  ").append(i + 1).append(". ").append(wp.get("kpName"))
                   .append(" 错误率：").append(wp.get("errorRate")).append("%")
                   .append(" 错题数：").append(wp.get("errorCount")).append("\n");
            }
        } else {
            ctx.append("暂无薄弱知识点记录。\n");
        }
        ctx.append("\n");

        // 学业成绩
        Map<String, Object> scores = getStudentScoreInfo(studentId, courseId);
        ctx.append("【学业数据】\n");
        if (scores.containsKey("usualScore")) {
            ctx.append("平时成绩：").append(scores.get("usualScore"))
               .append(" 期中：").append(scores.get("midScore"))
               .append(" 期末：").append(scores.get("finalScore")).append("\n");
        }
        if (scores.containsKey("submitRate")) {
            ctx.append("作业提交率：").append(scores.get("submitRate")).append("%")
               .append("（共").append(scores.get("totalHomework")).append("次，提交").append(scores.get("submitCount")).append("次")
               .append("，迟交").append(scores.get("lateSubmitCount")).append("次）")
               .append(" 均分：").append(scores.get("avgHomeworkScore")).append("\n");
        }
        if (scores.containsKey("attendanceRate")) {
            ctx.append("出勤率：").append(scores.get("attendanceRate")).append("%")
               .append("（共").append(scores.get("totalClassTimes")).append("次")
               .append("，缺勤").append(scores.get("absentCount")).append("次")
               .append("，迟到").append(scores.get("lateCount")).append("次）")
               .append(" 随堂测验：").append(scores.get("quizScore")).append("\n");
        }
        if (scores.containsKey("correctRate")) {
            ctx.append("知识点正确率：").append(scores.get("correctRate")).append("%")
               .append("（共").append(scores.get("totalQuestion")).append("题")
               .append("，错").append(scores.get("errorCount")).append("题）")
               .append(" 基础正确率：");
            if (scores.get("basicTotal") != null && (Integer) scores.get("basicTotal") > 0) {
                ctx.append(Math.round((Integer) scores.get("basicCorrect") * 100.0 / (Integer) scores.get("basicTotal") * 10) / 10.0).append("%");
            } else {
                ctx.append("无数据");
            }
            ctx.append("\n");
        }

        return ctx.toString();
    }

    /**
     * 生成学生端的动态推荐问题
     */
    public List<String> getStudentSuggestions(Long studentId, Long courseId) {
        List<String> suggestions = new ArrayList<>();
        Map<String, Object> risk = getStudentRiskProfile(studentId, courseId);
        List<Map<String, Object>> weakPoints = getStudentWeakPoints(studentId, courseId);

        // 根据风险等级生成问题
        String level = (String) risk.get("highestLevel");
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> alerts = (List<Map<String, Object>>) risk.get("alerts");

        if (alerts != null && !alerts.isEmpty()) {
            suggestions.add("我目前的最高风险等级是" + risk.get("highestLevelName") + "，具体是什么原因？");
            if (alerts.size() > 1) {
                suggestions.add("我有多条预警，最需要优先解决的是哪一个？");
            }
            // 根据预警类型生成针对性问题
            for (Map<String, Object> a : alerts) {
                String type = (String) a.get("alertType");
                if ("HOMEWORK".equals(type)) {
                    suggestions.add("我的作业方面存在风险，应该如何提高作业提交率？");
                } else if ("ABSENTEEISM".equals(type)) {
                    suggestions.add("我的出勤率较低，会影响最终成绩吗？该如何补救？");
                } else if ("FAILURE".equals(type)) {
                    suggestions.add("我有挂科风险，目前最应该加强哪些方面的学习？");
                } else if ("KNOWLEDGE".equals(type)) {
                    suggestions.add("我的知识点掌握有不足，应该如何系统性地查漏补缺？");
                }
            }
        } else {
            suggestions.add("我目前的学习状态如何？有哪些可以提升的地方？");
        }

        // 根据薄弱知识点生成问题
        if (!weakPoints.isEmpty()) {
            String topKp = (String) weakPoints.get(0).get("kpName");
            suggestions.add("「" + topKp + "」这个知识点我比较薄弱，有什么好的学习方法？");
            if (weakPoints.size() >= 3) {
                suggestions.add("我最薄弱的3个知识点分别是什么？应该如何安排复习计划？");
            }
        }

        // 通用问题兜底
        suggestions.add("我该如何制定每天的学习计划？");
        suggestions.add("如何更有效地进行课后复习？");
        suggestions.add("学习中遇到不懂的问题该怎么办？");

        // 控制不超过6个
        return suggestions.size() > 6 ? suggestions.subList(0, 6) : suggestions;
    }

    private String levelName(String level) {
        if (level == null) return "暂无数据";
        return switch (level) {
            case "RED" -> "红色预警（高风险）";
            case "ORANGE" -> "橙色预警（中风险）";
            case "YELLOW" -> "黄色预警（低风险）";
            case "GREEN" -> "正常";
            case "NONE" -> "暂无数据";
            default -> "正常";
        };
    }

    private String typeName(String type) {
        return switch (type) {
            case "FAILURE" -> "挂科风险";
            case "HOMEWORK" -> "作业欠交";
            case "KNOWLEDGE" -> "知识点断层";
            case "ABSENTEEISM" -> "学习倦怠";
            case "DROP" -> "成绩骤降";
            case "CUMULATIVE" -> "累积风险";
            default -> type != null ? type : "未知";
        };
    }
}
