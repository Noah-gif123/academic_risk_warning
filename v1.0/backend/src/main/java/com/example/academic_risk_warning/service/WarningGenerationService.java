package com.example.academic_risk_warning.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.academic_risk_warning.config.WarningSystemProperties;
import com.example.academic_risk_warning.entity.*;
import com.example.academic_risk_warning.mapper.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.IsoFields;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 预警生成算法服务 v2
 * 改动:
 *   - 权重/阈值从 alert_rule_config 表读取，不再硬编码
 *   - 学业成绩权重从 course 表读取(usual_ratio/mid_ratio/final_ratio)
 *   - 知识点正确率修正: 使用 (totalQuestion - errorCount) / totalQuestion
 *   - 成绩骤降(DROP)修正: 对比期中与平时成绩的反向关系
 *   - clearOldAlerts 改为归档到 alert_snapshot，不再物理删除
 *   - 新增第6维: 学习时长风险分(study_duration)
 *   - isFreshmanSystem 增强: 大一下也视为新生体系
 */
@Service
public class WarningGenerationService {

    private static final Logger log = LoggerFactory.getLogger(WarningGenerationService.class);

    private final AlertRecordMapper alertRecordMapper;
    private final StudentMapper studentMapper;
    private final ScoreInfoMapper scoreInfoMapper;
    private final HomeworkInfoMapper homeworkInfoMapper;
    private final ClassPerformanceMapper classPerformanceMapper;
    private final KnowledgeMasteryMapper knowledgeMasteryMapper;
    private final HistoryRiskMapper historyRiskMapper;
    private final CourseMapper courseMapper;
    private final StudentCourseMapper studentCourseMapper;
    private final TeacherClassMapper teacherClassMapper;
    private final AlertRuleConfigMapper alertRuleConfigMapper;
    private final StudyDurationMapper studyDurationMapper;
    private final AlertSnapshotMapper alertSnapshotMapper;
    private final NotificationService notificationService;
    private final WarningSystemProperties warningProperties;

    public WarningGenerationService(AlertRecordMapper alertRecordMapper,
                                    StudentMapper studentMapper,
                                    ScoreInfoMapper scoreInfoMapper,
                                    HomeworkInfoMapper homeworkInfoMapper,
                                    ClassPerformanceMapper classPerformanceMapper,
                                    KnowledgeMasteryMapper knowledgeMasteryMapper,
                                    HistoryRiskMapper historyRiskMapper,
                                    CourseMapper courseMapper,
                                    StudentCourseMapper studentCourseMapper,
                                    TeacherClassMapper teacherClassMapper,
                                    AlertRuleConfigMapper alertRuleConfigMapper,
                                    StudyDurationMapper studyDurationMapper,
                                    AlertSnapshotMapper alertSnapshotMapper,
                                    NotificationService notificationService,
                                    WarningSystemProperties warningProperties) {
        this.alertRecordMapper = alertRecordMapper;
        this.studentMapper = studentMapper;
        this.scoreInfoMapper = scoreInfoMapper;
        this.homeworkInfoMapper = homeworkInfoMapper;
        this.classPerformanceMapper = classPerformanceMapper;
        this.knowledgeMasteryMapper = knowledgeMasteryMapper;
        this.historyRiskMapper = historyRiskMapper;
        this.courseMapper = courseMapper;
        this.studentCourseMapper = studentCourseMapper;
        this.teacherClassMapper = teacherClassMapper;
        this.alertRuleConfigMapper = alertRuleConfigMapper;
        this.studyDurationMapper = studyDurationMapper;
        this.alertSnapshotMapper = alertSnapshotMapper;
        this.notificationService = notificationService;
        this.warningProperties = warningProperties;
    }

    // ==================== 预警等级/类型/状态常量(仅用于字符串值,不再承载阈值) ====================
    private static final String LEVEL_RED = "RED";
    private static final String LEVEL_ORANGE = "ORANGE";
    private static final String LEVEL_YELLOW = "YELLOW";
    private static final String LEVEL_GREEN = "GREEN";

    private static final String TYPE_FAILURE = "FAILURE";
    private static final String TYPE_HOMEWORK = "HOMEWORK";
    private static final String TYPE_KNOWLEDGE = "KNOWLEDGE";
    private static final String TYPE_ABSENTEEISM = "ABSENTEEISM";
    private static final String TYPE_DROP = "DROP";
    private static final String TYPE_CUMULATIVE = "CUMULATIVE";

    private static final String STATUS_ACTIVE = "ACTIVE";
    private static final String STATUS_ARCHIVED = "ARCHIVED";

    // ==================== 评分体系常量 ====================
    private static final String SYSTEM_FRESHMAN = "FRESHMAN";
    private static final String SYSTEM_SENIOR = "SENIOR";

    /** 规则配置缓存, 按systemType索引 */
    private final Map<String, AlertRuleConfig> ruleConfigCache = new HashMap<>();

    /** 清除规则缓存(配置更新后调用) */
    public void invalidateRuleCache(String systemType) {
        if (systemType != null) ruleConfigCache.remove(systemType);
        else ruleConfigCache.clear();
    }

    // ==================== 公开方法 ====================

    /**
     * 为指定教师的所有学生生成预警
     */
    @Transactional
    public int generateWarningsForTeacher(Long teacherId) {
        log.info("========== [预警生成] 开始为教师 {} 生成预警 ==========", teacherId);
        List<Student> students = getStudentsByTeacherId(teacherId);
        log.info("[预警生成] 获取到学生数量: {}", students.size());
        if (students.isEmpty()) {
            log.info("[预警生成] 教师 {} 没有负责的学生，返回0", teacherId);
            return 0;
        }
        int count = generateWarningsForStudents(students);
        log.info("[预警生成] 生成预警数量: {}", count);
        log.info("========== [预警生成] 完成 ==========");
        return count;
    }

    /**
     * 为指定学生列表生成预警(批量优化版)
     */
    @Transactional
    public int generateWarningsForStudents(List<Student> students) {
        if (students.isEmpty()) return 0;

        Set<Long> studentIds = students.stream().map(Student::getId).collect(Collectors.toSet());

        // 批量预加载所有数据
        Map<Long, Map<Long, ScoreInfo>> allScores = batchLoadScores(studentIds);
        Map<Long, Map<Long, HomeworkInfo>> allHomeworks = batchLoadHomeworks(studentIds);
        Map<Long, Map<Long, ClassPerformance>> allPerformances = batchLoadPerformances(studentIds);
        Map<Long, Map<Long, KnowledgeMastery>> allMasteries = batchLoadMasteries(studentIds);
        Map<Long, Map<Long, HistoryRisk>> allRisks = batchLoadRisks(studentIds);
        Map<Long, Map<Long, List<StudyDuration>>> allStudyDurations = batchLoadStudyDurations(studentIds);
        Map<Long, Course> courseMap = batchLoadCourses(studentIds);
        Map<String, AlertRecord> latestAlerts = batchLoadLatestAlerts(studentIds);

        int count = 0;
        for (Student student : students) {
            // 归档旧预警快照并更新状态
            archiveOldAlerts(student.getId());

            Map<Long, ScoreInfo> scoreMap = allScores.getOrDefault(student.getId(), Collections.emptyMap());
            Map<Long, HomeworkInfo> homeworkMap = allHomeworks.getOrDefault(student.getId(), Collections.emptyMap());
            Map<Long, ClassPerformance> performanceMap = allPerformances.getOrDefault(student.getId(), Collections.emptyMap());
            Map<Long, KnowledgeMastery> masteryMap = allMasteries.getOrDefault(student.getId(), Collections.emptyMap());
            Map<Long, HistoryRisk> riskMap = allRisks.getOrDefault(student.getId(), Collections.emptyMap());
            Map<Long, List<StudyDuration>> studyDurationMap = allStudyDurations.getOrDefault(student.getId(), Collections.emptyMap());

            count += generateAlertsForStudent(student, scoreMap, homeworkMap,
                    performanceMap, masteryMap, riskMap, studyDurationMap, courseMap, latestAlerts);
        }
        return count;
    }

    /**
     * 为所有学生生成预警
     */
    @Transactional
    public int generateAllWarnings() {
        List<Student> students = studentMapper.selectList(null);
        if (students.isEmpty()) return 0;
        return generateWarningsForStudents(students);
    }

    // ==================== 每日全量快照（方案A） ====================

    /**
     * 生成"每日全量快照"：对全体学生、每门课各写一条当天快照。
     *
     * <p>方案A要点：快照不再依赖"当天是否触发预警"——等级为 GREEN 的学生同样记录，
     * 因此每个学生的风险趋势都是连续可画的；一人一课一天只保留一条，
     * 重复执行会先清理当天旧记录，不会像旧逻辑那样按预警条数翻倍。
     */
    @Transactional
    public int generateDailySnapshots() {
        return snapshotForStudents(studentMapper.selectList(null));
    }

    /** 为指定教师负责的学生生成当天全量快照 */
    @Transactional
    public int generateSnapshotsForTeacher(Long teacherId) {
        return snapshotForStudents(getStudentsByTeacherId(teacherId));
    }

    /**
     * 为给定学生列表生成"一人一课一天一条"的当天快照（不依赖是否触发预警）
     *
     * @return 写入的快照条数（= 学生选课数，跳过无选课/课程不存在的情况）
     */
    @Transactional
    public int snapshotForStudents(List<Student> students) {
        if (CollectionUtils.isEmpty(students)) return 0;

        Set<Long> studentIds = students.stream().map(Student::getId).collect(Collectors.toSet());

        // 批量预加载，避免 N+1
        Map<Long, Map<Long, ScoreInfo>> allScores = batchLoadScores(studentIds);
        Map<Long, Map<Long, HomeworkInfo>> allHomeworks = batchLoadHomeworks(studentIds);
        Map<Long, Map<Long, ClassPerformance>> allPerformances = batchLoadPerformances(studentIds);
        Map<Long, Map<Long, KnowledgeMastery>> allMasteries = batchLoadMasteries(studentIds);
        Map<Long, Map<Long, HistoryRisk>> allRisks = batchLoadRisks(studentIds);
        Map<Long, Map<Long, List<StudyDuration>>> allStudyDurations = batchLoadStudyDurations(studentIds);
        Map<Long, Course> courseMap = batchLoadCourses(studentIds);
        Map<String, AlertRecord> todayAlerts = batchLoadTodayAlerts(studentIds);

        LocalDate today = LocalDate.now();

        // 先整体清理这批学生"今天"的旧快照，保证本方法可重复执行
        alertSnapshotMapper.delete(new LambdaQueryWrapper<AlertSnapshot>()
                .in(AlertSnapshot::getStudentId, studentIds)
                .eq(AlertSnapshot::getSnapshotDate, today));

        int count = 0;
        for (Student student : students) {
            List<Long> courseIds = getStudentCourseIds(student.getId());
            if (courseIds.isEmpty()) continue;

            Map<Long, ScoreInfo> scoreMap = allScores.getOrDefault(student.getId(), Collections.emptyMap());
            Map<Long, HomeworkInfo> homeworkMap = allHomeworks.getOrDefault(student.getId(), Collections.emptyMap());
            Map<Long, ClassPerformance> performanceMap = allPerformances.getOrDefault(student.getId(), Collections.emptyMap());
            Map<Long, KnowledgeMastery> masteryMap = allMasteries.getOrDefault(student.getId(), Collections.emptyMap());
            Map<Long, HistoryRisk> riskMap = allRisks.getOrDefault(student.getId(), Collections.emptyMap());
            Map<Long, List<StudyDuration>> studyDurationMap =
                    allStudyDurations.getOrDefault(student.getId(), Collections.emptyMap());

            for (Long courseId : courseIds) {
                Course course = courseMap.get(courseId);
                if (course == null) continue;

                boolean isFreshman = isFreshmanSystem(student.getGrade(), course);
                AlertRuleConfig config = loadRuleConfig(isFreshman ? SYSTEM_FRESHMAN : SYSTEM_SENIOR);

                ScoreInfo score = scoreMap.get(courseId);
                HomeworkInfo homework = homeworkMap.get(courseId);
                ClassPerformance performance = performanceMap.get(courseId);
                KnowledgeMastery mastery = masteryMap.get(courseId);
                HistoryRisk historyRisk = riskMap.get(courseId);
                List<StudyDuration> studyDurations = studyDurationMap.getOrDefault(courseId, Collections.emptyList());

                RiskScore riskScore = calculateRiskScores(score, homework, performance, mastery,
                        historyRisk, studyDurations, course, config, isFreshman);
                String alertLevel = determineAlertLevel(riskScore.totalScore, config);
                List<String> alertTypes =
                        determineAlertTypes(riskScore, score, homework, performance, mastery, config);

                // 当天若已生成预警，则把快照与其关联（便于溯源）
                AlertRecord todayAlert = todayAlerts.get(student.getId() + "_" + courseId);

                alertSnapshotMapper.insert(buildSnapshot(student, courseId, alertLevel, alertTypes, riskScore,
                        score, homework, performance, mastery, studyDurations,
                        todayAlert != null ? todayAlert.getId() : null, today));
                count++;
            }
        }

        log.info("[每日快照] 写入 {} 条（覆盖学生 {} 人，日期 {}）", count, students.size(), today);
        return count;
    }

    /** 加载"今天"生成的预警（非累积类型优先），用于给当日快照回填 relatedAlertId */
    private Map<String, AlertRecord> batchLoadTodayAlerts(Set<Long> studentIds) {
        if (studentIds.isEmpty()) return Collections.emptyMap();
        List<AlertRecord> todayAlerts = alertRecordMapper.selectList(
                new LambdaQueryWrapper<AlertRecord>()
                        .in(AlertRecord::getStudentId, studentIds)
                        .ge(AlertRecord::getCreateTime, LocalDate.now().atStartOfDay())
                        .orderByDesc(AlertRecord::getCreateTime));
        Map<String, AlertRecord> result = new HashMap<>();
        for (AlertRecord a : todayAlerts) {
            if (TYPE_CUMULATIVE.equals(a.getAlertType())) continue;
            String key = a.getStudentId() + "_" + (a.getCourseId() != null ? a.getCourseId() : "0");
            result.putIfAbsent(key, a);
        }
        return result;
    }

    // ==================== 规则配置加载 ====================

    /**
     * 从DB加载(并缓存)规则配置, 找不到时返回基于硬编码的兜底配置
     */
    private AlertRuleConfig loadRuleConfig(String systemType) {
        // 查缓存
        AlertRuleConfig cached = ruleConfigCache.get(systemType);
        if (cached != null) return cached;

        // 查DB(优先匹配course_id=NULL的默认规则)
        AlertRuleConfig config = alertRuleConfigMapper.selectOne(
                new LambdaQueryWrapper<AlertRuleConfig>()
                        .eq(AlertRuleConfig::getSystemType, systemType)
                        .isNull(AlertRuleConfig::getCourseId)
                        .eq(AlertRuleConfig::getIsActive, true)
        );

        if (config == null) {
            config = buildDefaultConfig(systemType);
        }

        ruleConfigCache.put(systemType, config);
        return config;
    }

    /** 兜底: 硬编码默认配置(v3全维度, 确保DB无数据时也能运行) */
    private AlertRuleConfig buildDefaultConfig(String systemType) {
        AlertRuleConfig c = new AlertRuleConfig();
        c.setSystemType(systemType);

        // ===== 预警等级阈值 =====
        c.setThresholdRed(BigDecimal.valueOf(40.0));
        c.setThresholdOrange(BigDecimal.valueOf(30.0));
        c.setThresholdYellow(BigDecimal.valueOf(15.0));

        // ===== 类型触发阈值 =====
        c.setHomeworkSubmitRateThreshold(BigDecimal.valueOf(70.0));
        c.setKnowledgeCorrectRateThreshold(BigDecimal.valueOf(50.0));
        c.setAttendanceRateThreshold(BigDecimal.valueOf(80.0));
        c.setScoreDropThreshold(BigDecimal.valueOf(15.0));

        // ===== 无数据默认风险分 =====
        c.setDefaultAcademicRisk(BigDecimal.valueOf(50.0));
        c.setDefaultHomeworkRisk(BigDecimal.valueOf(30.0));
        c.setDefaultAttendanceRisk(BigDecimal.valueOf(20.0));
        c.setDefaultKnowledgeRisk(BigDecimal.valueOf(25.0));
        c.setDefaultHistoryRisk(BigDecimal.valueOf(15.0));
        c.setDefaultHistoryFreshmanRisk(BigDecimal.valueOf(0.0));

        // ===== 5维权重(按体系) =====
        if (SYSTEM_FRESHMAN.equals(systemType)) {
            c.setWeightAcademic(BigDecimal.valueOf(0.350));
            c.setWeightHomework(BigDecimal.valueOf(0.250));
            c.setWeightKnowledge(BigDecimal.valueOf(0.200));
            c.setWeightAttendance(BigDecimal.valueOf(0.150));
            c.setWeightHistory(BigDecimal.valueOf(0.050));
        } else {
            c.setWeightAcademic(BigDecimal.valueOf(0.300));
            c.setWeightHomework(BigDecimal.valueOf(0.200));
            c.setWeightKnowledge(BigDecimal.valueOf(0.150));
            c.setWeightAttendance(BigDecimal.valueOf(0.100));
            c.setWeightHistory(BigDecimal.valueOf(0.250));
        }

        // ===== v3: 作业维度子权重 =====
        c.setHomeworkSubmitWeight(BigDecimal.valueOf(40.0));
        c.setHomeworkOntimeWeight(BigDecimal.valueOf(30.0));
        c.setHomeworkAvgScoreWeight(BigDecimal.valueOf(30.0));

        // ===== v3: 出勤维度子权重 =====
        c.setAttendanceAbsentWeight(BigDecimal.valueOf(50.0));
        c.setAttendanceLateWeight(BigDecimal.valueOf(30.0));
        c.setAttendanceQuizWeight(BigDecimal.valueOf(20.0));

        // ===== v3: 知识点维度子权重 =====
        c.setKnowledgeCorrectWeight(BigDecimal.valueOf(50.0));
        c.setKnowledgeWeakRateWeight(BigDecimal.valueOf(30.0));
        c.setKnowledgeBasicRateWeight(BigDecimal.valueOf(20.0));

        // ===== v3: 历史风险加减分 =====
        c.setHistoryFailedBonus(BigDecimal.valueOf(15.0));
        c.setHistoryUnstableBonus(BigDecimal.valueOf(20.0));
        c.setHistoryNormalBonus(BigDecimal.valueOf(10.0));

        // ===== v3: 学习时长风险参数 =====
        c.setStudyLookbackWeeks(4);
        c.setStudyDeclineRatio(BigDecimal.valueOf(0.50));
        c.setStudyDeclineBonus(BigDecimal.valueOf(10.0));
        c.setStudyDefaultRisk(BigDecimal.valueOf(10.0));

        // ===== v3: 预测成绩参数 =====
        c.setPredictedPassScore(BigDecimal.valueOf(60.0));
        c.setPredictedSubmitThreshold(BigDecimal.valueOf(70.0));
        c.setPredictedAttendanceThreshold(BigDecimal.valueOf(80.0));

        return c;
    }

    // ==================== 单学生预警生成 ====================

    private int generateAlertsForStudent(Student student,
                                         Map<Long, ScoreInfo> scoreMap,
                                         Map<Long, HomeworkInfo> homeworkMap,
                                         Map<Long, ClassPerformance> performanceMap,
                                         Map<Long, KnowledgeMastery> masteryMap,
                                         Map<Long, HistoryRisk> riskMap,
                                         Map<Long, List<StudyDuration>> studyDurationMap,
                                         Map<Long, Course> courseMap,
                                         Map<String, AlertRecord> latestAlerts) {
        if (student == null) return 0;

        List<Long> courseIds = getStudentCourseIds(student.getId());
        if (courseIds.isEmpty()) return 0;

        int alertCount = 0;

        for (Long courseId : courseIds) {
            Course course = courseMap.get(courseId);
            if (course == null) continue;

            boolean isFreshman = isFreshmanSystem(student.getGrade(), course);
            AlertRuleConfig config = loadRuleConfig(isFreshman ? SYSTEM_FRESHMAN : SYSTEM_SENIOR);

            ScoreInfo score = scoreMap.get(courseId);
            HomeworkInfo homework = homeworkMap.get(courseId);
            ClassPerformance performance = performanceMap.get(courseId);
            KnowledgeMastery mastery = masteryMap.get(courseId);
            HistoryRisk historyRisk = riskMap.get(courseId);
            List<StudyDuration> studyDurations = studyDurationMap.getOrDefault(courseId, Collections.emptyList());

            RiskScore riskScore = calculateRiskScores(score, homework, performance, mastery, historyRisk,
                    studyDurations, course, config, isFreshman);

            String alertLevel = determineAlertLevel(riskScore.totalScore, config);
            if (LEVEL_GREEN.equals(alertLevel)) continue;

            List<String> alertTypes = determineAlertTypes(riskScore, score, homework, performance, mastery, config);
            if (alertTypes.isEmpty()) continue;

            // ======== 去重检查: 避免重复生成 ========
            String key = student.getId() + "_" + courseId;
            AlertRecord lastAlert = latestAlerts.get(key);
            if (shouldSkipGeneration(lastAlert, alertLevel, alertTypes)) {
                log.debug("[预警去重] 学生 {} 课程 {} 跳过(最近状态: {})",
                        student.getId(), courseId, lastAlert != null ? lastAlert.getStatus() : "无");
                continue;
            }

            List<AlertRecord> alertsToInsert = new ArrayList<>();
            for (String alertType : alertTypes) {
                alertsToInsert.add(createAlertRecord(student, courseId, course.getCourseName(),
                        alertLevel, alertType, riskScore));
            }

            if (alertTypes.size() >= 2) {
                alertsToInsert.add(createCumulativeAlert(student, courseId, course.getCourseName(),
                        alertLevel, riskScore, alertTypes.size()));
            }

            if (!alertsToInsert.isEmpty()) {
                alertRecordMapper.insertBatch(alertsToInsert);
                alertCount += alertsToInsert.size();

                // 方案A: 快照按"一人一课一天一条"写入，不再按预警条数重复写
                Long relatedAlertId = alertsToInsert.stream()
                        .map(AlertRecord::getId)
                        .filter(Objects::nonNull)
                        .findFirst().orElse(null);
                saveDailySnapshot(buildSnapshot(student, courseId, alertLevel, alertTypes, riskScore,
                        score, homework, performance, mastery, studyDurations, relatedAlertId, LocalDate.now()));

                // 非累积类型预警才发通知
                for (AlertRecord alert : alertsToInsert) {
                    if (!TYPE_CUMULATIVE.equals(alert.getAlertType())) {
                        notificationService.notifyAlertGenerated(alert);
                    }
                }
            }
        }
        return alertCount;
    }

    // ==================== 风险分计算 ====================

    private RiskScore calculateRiskScores(ScoreInfo score, HomeworkInfo homework,
                                          ClassPerformance performance, KnowledgeMastery mastery,
                                          HistoryRisk historyRisk, List<StudyDuration> studyDurations,
                                          Course course, AlertRuleConfig config, boolean isFreshman) {
        RiskScore result = new RiskScore();

        // 1. 学业风险分(使用course表权重)
        result.academicScore = calculateAcademicRiskScore(score, course);

        // 2. 作业风险分
        result.homeworkScore = calculateHomeworkRiskScore(homework, config);

        // 3. 出勤风险分
        result.attendanceScore = calculateAttendanceRiskScore(performance, config);

        // 4. 知识点风险分(已修复B2)
        result.knowledgeScore = calculateKnowledgeRiskScore(mastery, config);

        // 5. 历史风险分
        result.historyScore = calculateHistoryRiskScore(historyRisk, isFreshman, config);

        // 6. 学习时长风险分(第6维)
        result.studyDurationScore = calculateStudyDurationRiskScore(studyDurations, config);

        // 7. 综合风险分(使用DB配置的5维权重)
        double wAca = config.getWeightAcademic().doubleValue();
        double wHw = config.getWeightHomework().doubleValue();
        double wKn = config.getWeightKnowledge().doubleValue();
        double wAtt = config.getWeightAttendance().doubleValue();
        double wHist = config.getWeightHistory().doubleValue();

        result.totalScore = result.academicScore * wAca
                + result.homeworkScore * wHw
                + result.knowledgeScore * wKn
                + result.attendanceScore * wAtt
                + result.historyScore * wHist;

        // 8. 预测成绩(v3: 参数从配置读取)
        result.predictedScore = predictFinalScore(score, homework, performance, config);

        return result;
    }

    // ---------- 学业风险分 ----------
    private double calculateAcademicRiskScore(ScoreInfo score, Course course) {
        if (score == null) return 50.0;
        double academicScore = calculateAcademicComponent(score, course);
        return Math.max(0, Math.min(100, 100 - academicScore));
    }

    /**
     * 计算学业成绩(使用course表配置的权重, 兜底20/30/50)
     */
    private double calculateAcademicComponent(ScoreInfo score, Course course) {
        double us = (score.getUsualScore() != null) ? score.getUsualScore() : 0;
        double ms = (score.getMidScore() != null) ? score.getMidScore() : 0;
        double fs = (score.getFinalScore() != null) ? score.getFinalScore() : 0;

        // 优先从course表读取权重
        double wUsual, wMid, wFinal;
        if (course != null && course.getUsualRatio() != null && course.getMidRatio() != null
                && course.getFinalRatio() != null
                && (course.getUsualRatio() + course.getMidRatio() + course.getFinalRatio()) == 100) {
            wUsual = course.getUsualRatio() / 100.0;
            wMid = course.getMidRatio() / 100.0;
            wFinal = course.getFinalRatio() / 100.0;
        } else {
            // 兜底(与v1一致)
            wUsual = 0.20;
            wMid = 0.30;
            wFinal = 0.50;
        }

        // 期中权重为0(如课程1和2都是mid_ratio=0), 跳过期中
        if (wMid == 0) {
            if (score.getFinalScore() == null) {
                return us; // 只有平时成绩
            } else {
                return us * wUsual + fs * wFinal;
            }
        }

        if (score.getFinalScore() == null) {
            return us * 0.4 + ms * 0.6;
        } else {
            return us * wUsual + ms * wMid + fs * wFinal;
        }
    }

    // ---------- 作业风险分 ----------
    private double calculateHomeworkRiskScore(HomeworkInfo homework, AlertRuleConfig config) {
        double defaultRisk = config.getDefaultHomeworkRisk() != null
                ? config.getDefaultHomeworkRisk().doubleValue() : 30.0;

        if (homework == null || homework.getTotalHomework() == null || homework.getTotalHomework() == 0) {
            return defaultRisk;
        }

        double submitRate = homework.getSubmitCount() * 100.0 / homework.getTotalHomework();
        double ontimeRate = 100;
        int late = homework.getLateSubmitCount() != null ? homework.getLateSubmitCount() : 0;
        if (homework.getTotalHomework() > 0) {
            ontimeRate = Math.max(0, (homework.getSubmitCount() - late) * 100.0 / homework.getTotalHomework());
        }
        double avgScoreRate = (homework.getAvgScore() != null) ? homework.getAvgScore() : 0;

        // v3: 子权重从配置读取
        double wSubmit = getConfigDouble(config.getHomeworkSubmitWeight(), 40.0);
        double wOntime = getConfigDouble(config.getHomeworkOntimeWeight(), 30.0);
        double wAvgScore = getConfigDouble(config.getHomeworkAvgScoreWeight(), 30.0);

        double risk = (1 - submitRate / 100) * wSubmit
                + (1 - ontimeRate / 100) * wOntime
                + (1 - avgScoreRate / 100) * wAvgScore;

        return Math.max(0, Math.min(100, risk));
    }

    // ---------- 出勤风险分 ----------
    private double calculateAttendanceRiskScore(ClassPerformance performance, AlertRuleConfig config) {
        double defaultRisk = config.getDefaultAttendanceRisk() != null
                ? config.getDefaultAttendanceRisk().doubleValue() : 20.0;

        if (performance == null || performance.getTotalClassTimes() == null || performance.getTotalClassTimes() == 0) {
            return defaultRisk;
        }

        int totalClass = performance.getTotalClassTimes();
        int absent = performance.getAbsentCount() != null ? performance.getAbsentCount() : 0;
        int late = performance.getLateCount() != null ? performance.getLateCount() : 0;
        double quiz = performance.getQuizScore() != null ? performance.getQuizScore() : 0;

        // v3: 子权重从配置读取
        double wAbsent = getConfigDouble(config.getAttendanceAbsentWeight(), 50.0);
        double wLate = getConfigDouble(config.getAttendanceLateWeight(), 30.0);
        double wQuiz = getConfigDouble(config.getAttendanceQuizWeight(), 20.0);

        double risk = (absent * 1.0 / totalClass) * wAbsent
                + (late * 1.0 / totalClass) * wLate
                + (1 - quiz / 100) * wQuiz;

        return Math.max(0, Math.min(100, risk));
    }

    // ---------- 知识点风险分 (B2修复) ----------
    private double calculateKnowledgeRiskScore(KnowledgeMastery mastery, AlertRuleConfig config) {
        double defaultRisk = config.getDefaultKnowledgeRisk() != null
                ? config.getDefaultKnowledgeRisk().doubleValue() : 25.0;

        if (mastery == null) return defaultRisk;

        // B2修复: 正确率 = (总题目数 - 错题数) / 总题目数, 不再用basicCorrect
        double correctRate = 0;
        if (mastery.getTotalQuestion() != null && mastery.getTotalQuestion() > 0) {
            int errorCount = mastery.getErrorCount() != null ? mastery.getErrorCount() : 0;
            int totalCorrect = mastery.getTotalQuestion() - errorCount;
            correctRate = totalCorrect * 100.0 / mastery.getTotalQuestion();
        }

        double basicRate = 0;
        if (mastery.getBasicTotal() != null && mastery.getBasicTotal() > 0) {
            int basicCorrect = mastery.getBasicCorrect() != null ? mastery.getBasicCorrect() : 0;
            basicRate = basicCorrect * 100.0 / mastery.getBasicTotal();
        }

        double weakRate = 0;
        if (mastery.getTotalKnowledge() != null && mastery.getTotalKnowledge() > 0
                && mastery.getWeakKnowledgeCount() != null) {
            weakRate = mastery.getWeakKnowledgeCount() * 100.0 / mastery.getTotalKnowledge();
        }

        // v3: 子权重从配置读取
        double wCorrect = getConfigDouble(config.getKnowledgeCorrectWeight(), 50.0);
        double wWeakRate = getConfigDouble(config.getKnowledgeWeakRateWeight(), 30.0);
        double wBasicRate = getConfigDouble(config.getKnowledgeBasicRateWeight(), 20.0);

        double risk = (1 - correctRate / 100) * wCorrect
                + weakRate * (wWeakRate / 100.0) // weakRate已是0-100, 需转换为比例系数
                + (1 - basicRate / 100) * wBasicRate;

        return Math.max(0, Math.min(100, risk));
    }

    // ---------- 历史风险分 ----------
    private double calculateHistoryRiskScore(HistoryRisk historyRisk, boolean isFreshman, AlertRuleConfig config) {
        if (isFreshman) {
            return config.getDefaultHistoryFreshmanRisk() != null
                    ? config.getDefaultHistoryFreshmanRisk().doubleValue() : 0.0;
        }
        double defaultRisk = config.getDefaultHistoryRisk() != null
                ? config.getDefaultHistoryRisk().doubleValue() : 15.0;

        if (historyRisk == null) return defaultRisk;

        // v3: 加减分值从配置读取
        double failedBonus = getConfigDouble(config.getHistoryFailedBonus(), 15.0);
        double unstableBonus = getConfigDouble(config.getHistoryUnstableBonus(), 20.0);
        double normalBonus = getConfigDouble(config.getHistoryNormalBonus(), 10.0);

        double risk = 0;
        if ("是".equals(historyRisk.getLastTermFailed())) {
            risk += failedBonus;
        }
        if ("不稳定".equals(historyRisk.getStudyStable())) {
            risk += unstableBonus;
        } else if ("一般".equals(historyRisk.getStudyStable())) {
            risk += normalBonus;
        }
        return Math.max(0, Math.min(100, risk));
    }

    // ---------- 学习时长风险分 (第6维, v3: 参数从配置读取) ----------
    private double calculateStudyDurationRiskScore(List<StudyDuration> durations, AlertRuleConfig config) {
        double defaultRisk = getConfigDouble(config.getStudyDefaultRisk(), 10.0);

        if (CollectionUtils.isEmpty(durations)) {
            return defaultRisk;
        }

        // 取近N周平均学习时长(N从配置读取)
        List<StudyDuration> sorted = durations.stream()
                .sorted(Comparator.comparing(StudyDuration::getWeekNumber).reversed())
                .toList();

        int lookbackWeeks = config.getStudyLookbackWeeks() != null
                ? Math.min(config.getStudyLookbackWeeks(), sorted.size())
                : Math.min(4, sorted.size());
        double avgMinutes = 0;
        for (int i = 0; i < lookbackWeeks; i++) {
            avgMinutes += sorted.get(i).getTotalMinutes();
        }
        avgMinutes /= lookbackWeeks;

        // 时长越短风险越高，使用动态配置阈值
        List<Integer> thresholds = warningProperties.getStudyDurationThresholds();
        double risk;
        if (thresholds.isEmpty()) {
            risk = 100.0;
        } else {
            double step = 100.0 / thresholds.size();
            int segmentIdx = thresholds.size();
            for (int i = thresholds.size() - 1; i >= 0; i--) {
                if (avgMinutes < thresholds.get(i)) {
                    segmentIdx = i;
                }
            }
            risk = 100.0 - segmentIdx * step;
        }

        // v3: 下滑趋势检测参数从配置读取
        int lookback = config.getStudyLookbackWeeks() != null ? config.getStudyLookbackWeeks() : 4;
        if (sorted.size() >= lookback) {
            double latest = sorted.get(0).getTotalMinutes();
            double oldest = sorted.get(lookback - 1).getTotalMinutes();
            double declineRatio = getConfigDouble(config.getStudyDeclineRatio(), 0.5);
            if (oldest > 0 && latest / oldest < declineRatio) {
                double declineBonus = getConfigDouble(config.getStudyDeclineBonus(), 10.0);
                risk = Math.min(100, risk + declineBonus);
            }
        }

        return risk;
    }

    // ---------- 预测期末成绩 (v3: 参数从配置读取) ----------
    private double predictFinalScore(ScoreInfo score, HomeworkInfo homework,
                                     ClassPerformance performance, AlertRuleConfig config) {
        double defaultScore = getConfigDouble(config.getPredictedPassScore(), 60.0);

        if (score == null) return defaultScore;

        double predicted;
        double us = (score.getUsualScore() != null) ? score.getUsualScore() : 0;
        double ms = (score.getMidScore() != null) ? score.getMidScore() : 0;
        double fs = (score.getFinalScore() != null) ? score.getFinalScore() : 0;

        if (score.getFinalScore() != null) {
            return fs;
        } else if (score.getMidScore() != null) {
            predicted = us * 0.4 + ms * 0.6;
        } else if (score.getUsualScore() != null) {
            predicted = us;
        } else {
            predicted = defaultScore;
        }

        double submitThreshold = getConfigDouble(config.getPredictedSubmitThreshold(), 70.0);
        double attThreshold = getConfigDouble(config.getPredictedAttendanceThreshold(), 80.0);

        if (homework != null && homework.getTotalHomework() != null && homework.getTotalHomework() > 0) {
            double submitRate = homework.getSubmitCount() * 100.0 / homework.getTotalHomework();
            if (submitRate < submitThreshold) {
                predicted -= (submitThreshold - submitRate) * warningProperties.getHomeworkSubmitAdjustFactor();
            }
        }

        if (performance != null && performance.getTotalClassTimes() != null && performance.getTotalClassTimes() > 0) {
            int absent = performance.getAbsentCount() != null ? performance.getAbsentCount() : 0;
            double attRate = (performance.getTotalClassTimes() - absent) * 100.0 / performance.getTotalClassTimes();
            if (attRate < attThreshold) {
                predicted -= (attThreshold - attRate) * warningProperties.getAttendanceAdjustFactor();
            }
        }

        return Math.max(0, Math.min(100, predicted));
    }

    // ---------- 预警等级判定(使用DB阈值) ----------
    private String determineAlertLevel(double totalRiskScore, AlertRuleConfig config) {
        double red = config.getThresholdRed().doubleValue();
        double orange = config.getThresholdOrange().doubleValue();
        double yellow = config.getThresholdYellow().doubleValue();

        if (totalRiskScore >= red) return LEVEL_RED;
        if (totalRiskScore >= orange) return LEVEL_ORANGE;
        if (totalRiskScore >= yellow) return LEVEL_YELLOW;
        return LEVEL_GREEN;
    }

    // ---------- 预警类型判定 (v3: 及格线从配置读取) ----------
    private List<String> determineAlertTypes(RiskScore riskScore,
                                             ScoreInfo score, HomeworkInfo homework,
                                             ClassPerformance performance, KnowledgeMastery mastery,
                                             AlertRuleConfig config) {
        List<String> types = new ArrayList<>();

        double hwThreshold = config.getHomeworkSubmitRateThreshold() != null
                ? config.getHomeworkSubmitRateThreshold().doubleValue() : 70.0;
        double knThreshold = config.getKnowledgeCorrectRateThreshold() != null
                ? config.getKnowledgeCorrectRateThreshold().doubleValue() : 50.0;
        double attThreshold = config.getAttendanceRateThreshold() != null
                ? config.getAttendanceRateThreshold().doubleValue() : 80.0;
        double dropThreshold = config.getScoreDropThreshold() != null
                ? config.getScoreDropThreshold().doubleValue() : 15.0;
        double passScore = getConfigDouble(config.getPredictedPassScore(), 60.0);

        // 1. 挂科风险
        if (riskScore.predictedScore < passScore) {
            types.add(TYPE_FAILURE);
        }

        // 2. 作业欠交
        if (homework != null && homework.getTotalHomework() != null && homework.getTotalHomework() > 0) {
            double submitRate = homework.getSubmitCount() * 100.0 / homework.getTotalHomework();
            if (submitRate < hwThreshold) {
                types.add(TYPE_HOMEWORK);
            }
        }

        // 3. 知识点断层
        if (mastery != null && mastery.getTotalQuestion() != null && mastery.getTotalQuestion() > 0) {
            int errorCount = mastery.getErrorCount() != null ? mastery.getErrorCount() : 0;
            int totalCorrect = mastery.getTotalQuestion() - errorCount;
            double correctRate = totalCorrect * 100.0 / mastery.getTotalQuestion();
            if (correctRate < knThreshold) {
                types.add(TYPE_KNOWLEDGE);
            }
        }

        // 4. 学习倦怠(出勤率低)
        if (performance != null && performance.getTotalClassTimes() != null
                && performance.getTotalClassTimes() > 0) {
            int absent = performance.getAbsentCount() != null ? performance.getAbsentCount() : 0;
            double attRate = (performance.getTotalClassTimes() - absent) * 100.0
                    / performance.getTotalClassTimes();
            if (attRate < attThreshold) {
                types.add(TYPE_ABSENTEEISM);
            }
        }

        // 5. 成绩骤降(修复B4: 期中成绩远低于平时成绩)
        if (score != null && score.getUsualScore() != null && score.getMidScore() != null
                && score.getUsualScore() > 0) {
            double drop = score.getUsualScore() - score.getMidScore();
            if (drop > dropThreshold) {
                types.add(TYPE_DROP);
            }
        }

        return types;
    }

    // ==================== 创建预警记录 ====================

    private AlertRecord createAlertRecord(Student student, Long courseId, String courseName,
                                          String alertLevel, String alertType, RiskScore riskScore) {
        AlertRecord alert = new AlertRecord();
        alert.setStudentId(student.getId());
        alert.setCourseId(courseId);
        alert.setAlertLevel(alertLevel);
        alert.setAlertType(alertType);
        alert.setRiskScore(bd(riskScore.totalScore));
        alert.setPredictedScore(bd(riskScore.predictedScore));
        alert.setAcademicRiskScore(bd(riskScore.academicScore));
        alert.setHomeworkRiskScore(bd(riskScore.homeworkScore));
        alert.setAttendanceRiskScore(bd(riskScore.attendanceScore));
        alert.setKnowledgeRiskScore(bd(riskScore.knowledgeScore));
        alert.setHistoryRiskScore(bd(riskScore.historyScore));
        alert.setStatus(STATUS_ACTIVE);
        alert.setAlertReason(generateAlertReason(alertType, riskScore));
        alert.setCreateTime(LocalDateTime.now());
        alert.setExpireTime(LocalDateTime.now().plusDays(warningProperties.getAlertExpireDays()));
        alert.setNotified(false);
        return alert;
    }

    private AlertRecord createCumulativeAlert(Student student, Long courseId, String courseName,
                                               String alertLevel, RiskScore riskScore, int typeCount) {
        AlertRecord alert = new AlertRecord();
        alert.setStudentId(student.getId());
        alert.setCourseId(courseId);
        // 累积风险等级升级
        String cumulativeLevel = LEVEL_RED.equals(alertLevel) ? LEVEL_RED
                : LEVEL_ORANGE.equals(alertLevel) ? LEVEL_RED : LEVEL_ORANGE;
        alert.setAlertLevel(cumulativeLevel);
        alert.setAlertType(TYPE_CUMULATIVE);
        alert.setRiskScore(bd(riskScore.totalScore + 10));
        alert.setPredictedScore(bd(riskScore.predictedScore));
        alert.setAcademicRiskScore(bd(riskScore.academicScore));
        alert.setHomeworkRiskScore(bd(riskScore.homeworkScore));
        alert.setAttendanceRiskScore(bd(riskScore.attendanceScore));
        alert.setKnowledgeRiskScore(bd(riskScore.knowledgeScore));
        alert.setHistoryRiskScore(bd(riskScore.historyScore));
        alert.setStatus(STATUS_ACTIVE);
        alert.setAlertReason(String.format("多维度风险叠加：同时存在%d种风险问题，需要重点关注和干预", typeCount));
        alert.setCreateTime(LocalDateTime.now());
        alert.setExpireTime(LocalDateTime.now().plusDays(warningProperties.getAlertExpireDays()));
        alert.setNotified(false);
        return alert;
    }

    private String generateAlertReason(String alertType, RiskScore riskScore) {
        switch (alertType) {
            case TYPE_FAILURE:
                return String.format("预测期末成绩%.1f分，存在挂科风险。综合风险分%.1f，建议加强复习和辅导。",
                        riskScore.predictedScore, riskScore.totalScore);
            case TYPE_HOMEWORK:
                return String.format("作业提交率较低，综合风险分%.1f。建议督促完成作业并了解原因。", riskScore.totalScore);
            case TYPE_KNOWLEDGE:
                return String.format("知识点掌握率偏低，综合风险分%.1f。建议针对性练习和辅导。", riskScore.totalScore);
            case TYPE_ABSENTEEISM:
                return String.format("课堂出勤率较低，综合风险分%.1f。建议关注学生出勤情况。", riskScore.totalScore);
            case TYPE_DROP:
                return String.format("成绩出现明显下降趋势，综合风险分%.1f。建议分析原因并给予关注。", riskScore.totalScore);
            default:
                return String.format("综合风险分%.1f，存在学习风险，建议持续关注。", riskScore.totalScore);
        }
    }

    // ==================== 去重检查(方案C+轻度B: 避免无限重复生成) ====================

    /**
     * 批量加载每个学生+课程对的最新一条预警(用于去重判断)
     * 返回 Map<"studentId_courseId", AlertRecord>
     */
    private Map<String, AlertRecord> batchLoadLatestAlerts(Set<Long> studentIds) {
        if (studentIds.isEmpty()) return Collections.emptyMap();
        List<AlertRecord> all = alertRecordMapper.selectList(
                new LambdaQueryWrapper<AlertRecord>()
                        .in(AlertRecord::getStudentId, studentIds)
                        .orderByDesc(AlertRecord::getCreateTime)
        );
        // 按 studentId_courseId 分组, 取每组第一条(最新的)
        Map<String, AlertRecord> result = new HashMap<>();
        for (AlertRecord a : all) {
            String key = a.getStudentId() + "_" + (a.getCourseId() != null ? a.getCourseId() : "0");
            result.putIfAbsent(key, a); // 已按时间倒序, 第一次放入就是最新的
        }
        return result;
    }

    /**
     * 去重规则: 判断是否应该跳过生成
     * 返回true=跳过, false=正常生成
     */
    boolean shouldSkipGeneration(AlertRecord lastAlert, String currentLevel, List<String> currentTypes) {
        if (lastAlert == null) return false; // 首次生成, 不跳过

        String status = lastAlert.getStatus();

        // DISMISSED + 同类型 → 永久跳过
        if ("DISMISSED".equals(status)) {
            if (lastAlert.getAlertType() != null && currentTypes.contains(lastAlert.getAlertType())) {
                return true;
            }
        }

        // HANDLED + N天内 → 跳过(教师还在干预中)
        if ("HANDLED".equals(status)) {
            if (lastAlert.getHandleTime() != null
                    && lastAlert.getHandleTime().plusDays(warningProperties.getHandledSkipDays()).isAfter(LocalDateTime.now())) {
                return true;
            }
            // 超时 → 需要对比风险, 此处不跳过, 由CLOSED逻辑覆盖
        }

        // CLOSED + 同一学期 + 风险未恶化 → 跳过
        if ("CLOSED".equals(status)) {
            // 判断是否同一学期: 生成时间在配置的月数内视为同学期
            if (lastAlert.getCreateTime() != null
                    && lastAlert.getCreateTime().plusMonths(warningProperties.getClosedSemesterMonths()).isAfter(LocalDateTime.now())) {
                // 对比风险分: 当前等级是否比上次更严重
                String lastLevel = lastAlert.getAlertLevel();
                if (!isLevelWorsened(lastLevel, currentLevel)) {
                    return true;
                }
            }
        }

        // 同一天内不重复生成 → 修复"重复点击生成预警导致预警条数翻倍"
        // 说明: 当天已生成过、且风险等级没有恶化(相同或更好)时直接跳过;
        //       这里不区分 ACTIVE/ARCHIVED, 因为同一次生成会先把旧 ACTIVE 归档,
        //       重复执行时看到的可能是当天刚被归档的那条。
        if (lastAlert.getCreateTime() != null
                && lastAlert.getCreateTime().toLocalDate().isEqual(LocalDate.now())
                && !isLevelWorsened(lastAlert.getAlertLevel(), currentLevel)) {
            return true;
        }

        return false;
    }

    /** 风险是否恶化: 按等级枚举序数比较 */
    private boolean isLevelWorsened(String oldLevel, String newLevel) {
        int old = levelOrder(oldLevel);
        int now = levelOrder(newLevel);
        return now > old; // 等级枚举值越大越严重(RED=3 > ORANGE=2 > YELLOW=1)
    }

    private int levelOrder(String level) {
        return switch (level) {
            case "RED" -> 3;
            case "ORANGE" -> 2;
            case "YELLOW" -> 1;
            default -> 0;
        };
    }

    // ==================== 归档旧预警(D1修复: 不再物理删除, 改为归档) ====================

    void archiveOldAlerts(Long studentId) {
        // 1. 查出所有ACTIVE预警
        List<AlertRecord> activeAlerts = alertRecordMapper.selectList(
                new LambdaQueryWrapper<AlertRecord>()
                        .eq(AlertRecord::getStudentId, studentId)
                        .eq(AlertRecord::getStatus, STATUS_ACTIVE)
        );

        if (activeAlerts.isEmpty()) return;

        // 2. 将状态改为ARCHIVED(保留历史数据)
        //    但"当天生成"的预警仍然有效, 不能归档: 否则同一天重复执行生成时,
        //    会先把还在待处理(ACTIVE)的预警归档掉, 再被"同日去重"跳过 → 待处理预警凭空消失。
        LocalDate today = LocalDate.now();
        for (AlertRecord alert : activeAlerts) {
            if (alert.getCreateTime() != null && alert.getCreateTime().toLocalDate().isEqual(today)) {
                continue;
            }
            alert.setStatus(STATUS_ARCHIVED);
            alertRecordMapper.updateById(alert);
        }
    }

    /**
     * 构造快照对象（不落库）。
     *
     * <p>方案A：快照是"每日全量档案"，等级为 GREEN、未触发预警时同样记录，
     * 由 isGeneratedAlert 标记当天是否真的触发了预警。
     */
    private AlertSnapshot buildSnapshot(Student student, Long courseId, String alertLevel,
                                        List<String> alertTypes, RiskScore riskScore,
                                        ScoreInfo score, HomeworkInfo homework,
                                        ClassPerformance performance, KnowledgeMastery mastery,
                                        List<StudyDuration> studyDurations,
                                        Long relatedAlertId, LocalDate snapshotDate) {
        AlertSnapshot snap = new AlertSnapshot();
        snap.setStudentId(student.getId());
        snap.setCourseId(courseId);
        snap.setSnapshotDate(snapshotDate);
        snap.setSnapshotWeek(snapshotDate.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR));
        snap.setAlertLevel(alertLevel);
        snap.setRiskScore(bd(riskScore.totalScore));
        snap.setAcademicRiskScore(bd(riskScore.academicScore));
        snap.setHomeworkRiskScore(bd(riskScore.homeworkScore));
        snap.setAttendanceRiskScore(bd(riskScore.attendanceScore));
        snap.setKnowledgeRiskScore(bd(riskScore.knowledgeScore));
        snap.setHistoryRiskScore(bd(riskScore.historyScore));
        snap.setStudyDurationRiskScore(bd(riskScore.studyDurationScore));
        snap.setPredictedScore(bd(riskScore.predictedScore));
        snap.setAlertTypes(CollectionUtils.isEmpty(alertTypes) ? null : String.join(",", alertTypes));
        boolean triggeredAlert = relatedAlertId != null
                || (!LEVEL_GREEN.equals(alertLevel) && !CollectionUtils.isEmpty(alertTypes));
        snap.setIsGeneratedAlert(triggeredAlert);
        snap.setRelatedAlertId(relatedAlertId);

        if (score != null) {
            snap.setUsualScore(score.getUsualScore());
            snap.setMidScore(score.getMidScore());
            snap.setFinalScore(score.getFinalScore());
        }
        if (homework != null && homework.getTotalHomework() != null && homework.getTotalHomework() > 0) {
            double sr = homework.getSubmitCount() * 100.0 / homework.getTotalHomework();
            snap.setHomeworkSubmitRate(BigDecimal.valueOf(sr).setScale(1, RoundingMode.HALF_UP));
        }
        if (performance != null && performance.getTotalClassTimes() != null && performance.getTotalClassTimes() > 0) {
            int absent = performance.getAbsentCount() != null ? performance.getAbsentCount() : 0;
            double ar = (performance.getTotalClassTimes() - absent) * 100.0 / performance.getTotalClassTimes();
            snap.setAttendanceRate(BigDecimal.valueOf(ar).setScale(1, RoundingMode.HALF_UP));
        }
        if (mastery != null && mastery.getTotalQuestion() != null && mastery.getTotalQuestion() > 0) {
            int ec = mastery.getErrorCount() != null ? mastery.getErrorCount() : 0;
            double cr = (mastery.getTotalQuestion() - ec) * 100.0 / mastery.getTotalQuestion();
            snap.setKnowledgeCorrectRate(BigDecimal.valueOf(cr).setScale(1, RoundingMode.HALF_UP));
        }
        if (!CollectionUtils.isEmpty(studyDurations)) {
            int sum = studyDurations.stream().mapToInt(StudyDuration::getTotalMinutes).sum();
            snap.setStudyTotalMinutes(sum / studyDurations.size());
        }
        snap.setCreateTime(LocalDateTime.now());
        return snap;
    }

    /**
     * 写入"一人一课一天一条"快照：先删除该(学生,课程,日期)的旧记录再插入。
     * 因此同一天重复执行（定时任务 + 手动生成 + 预警触发生成）不会产生重复行。
     */
    private void saveDailySnapshot(AlertSnapshot snap) {
        alertSnapshotMapper.delete(new LambdaQueryWrapper<AlertSnapshot>()
                .eq(AlertSnapshot::getStudentId, snap.getStudentId())
                .eq(snap.getCourseId() != null, AlertSnapshot::getCourseId, snap.getCourseId())
                .isNull(snap.getCourseId() == null, AlertSnapshot::getCourseId)
                .eq(AlertSnapshot::getSnapshotDate, snap.getSnapshotDate()));
        alertSnapshotMapper.insert(snap);
    }

    // ==================== 新生/老生体系判断 (B3修复) ====================

    /**
     * 判断是否使用新生体系
     * 规则:
     *  - 年级为"大一上" → 新生体系(纯新生, 无任何历史数据)
     *  - 课程无前序课程 → 新生体系(该门课无前置, 按新课对待)
     *  - 课程有前序课程 → 老生体系(有历史成绩可参考)
     * 注: "大一下"学生已有大一上学期数据, 不强制走新生体系
     */
    private boolean isFreshmanSystem(String grade, Course course) {
        // 只有"大一上"无条件走新生(无历史数据)
        if ("大一上".equals(grade)) {
            return true;
        }
        // 其余年级: 看课程是否有前序课程
        if (course == null || course.getPrerequisiteCourseId() == null) {
            return true; // 无前置课程 → 新生体系
        }
        return false; // 有前置课程 → 老生体系
    }

    // ==================== 数据加载 ====================

    private List<Student> getStudentsByTeacherId(Long teacherId) {
        List<TeacherClass> teacherClasses = teacherClassMapper.selectList(
                new LambdaQueryWrapper<TeacherClass>()
                        .eq(TeacherClass::getTeacherId, teacherId)
        );
        if (teacherClasses.isEmpty()) return Collections.emptyList();

        List<String> classNames = teacherClasses.stream()
                .map(TeacherClass::getClassName).distinct().toList();
        if (classNames.isEmpty()) return Collections.emptyList();

        List<StudentIdRange> studentRanges = teacherClasses.stream()
                .filter(tc -> tc.getStudentIdStart() != null && tc.getStudentIdEnd() != null)
                .map(tc -> new StudentIdRange(tc.getStudentIdStart(), tc.getStudentIdEnd()))
                .toList();

        List<Student> students = studentMapper.selectList(
                new LambdaQueryWrapper<Student>().in(Student::getClassName, classNames)
        );
        if (students.isEmpty()) return Collections.emptyList();

        if (!studentRanges.isEmpty()) {
            students = students.stream()
                    .filter(s -> isStudentInAnyRange(s.getId(), studentRanges))
                    .collect(Collectors.toList());
        }
        return students;
    }

    private record StudentIdRange(Long start, Long end) {}

    private boolean isStudentInAnyRange(Long studentId, List<StudentIdRange> ranges) {
        return ranges.stream().anyMatch(r -> studentId >= r.start() && studentId <= r.end());
    }

    private List<Long> getStudentCourseIds(Long studentId) {
        List<StudentCourse> studentCourses = studentCourseMapper.selectList(
                new LambdaQueryWrapper<StudentCourse>().eq(StudentCourse::getStudentId, studentId)
        );
        if (CollectionUtils.isEmpty(studentCourses)) {
            // 无关联时跳过(不再fallback到全部课程)
            return Collections.emptyList();
        }
        return studentCourses.stream().map(StudentCourse::getCourseId).distinct().toList();
    }

    private Map<Long, Course> batchLoadCourses(Set<Long> studentIds) {
        if (studentIds.isEmpty()) return Collections.emptyMap();
        List<StudentCourse> studentCourses = studentCourseMapper.selectList(
                new LambdaQueryWrapper<StudentCourse>().in(StudentCourse::getStudentId, studentIds)
        );
        if (studentCourses.isEmpty()) return Collections.emptyMap();

        Set<Long> courseIds = studentCourses.stream().map(StudentCourse::getCourseId).collect(Collectors.toSet());
        List<Course> courses = courseMapper.selectList(
                new LambdaQueryWrapper<Course>().in(Course::getId, courseIds)
        );
        return courses.stream().collect(Collectors.toMap(Course::getId, c -> c));
    }

    private Map<Long, Map<Long, ScoreInfo>> batchLoadScores(Set<Long> studentIds) {
        if (studentIds.isEmpty()) return Collections.emptyMap();
        List<ScoreInfo> list = scoreInfoMapper.selectList(
                new LambdaQueryWrapper<ScoreInfo>().in(ScoreInfo::getStudentId, studentIds));
        return list.stream().collect(Collectors.groupingBy(
                ScoreInfo::getStudentId,
                Collectors.toMap(ScoreInfo::getCourseId, s -> s, (a, b) -> a)));
    }

    private Map<Long, Map<Long, HomeworkInfo>> batchLoadHomeworks(Set<Long> studentIds) {
        if (studentIds.isEmpty()) return Collections.emptyMap();
        List<HomeworkInfo> list = homeworkInfoMapper.selectList(
                new LambdaQueryWrapper<HomeworkInfo>().in(HomeworkInfo::getStudentId, studentIds));
        return list.stream().collect(Collectors.groupingBy(
                HomeworkInfo::getStudentId,
                Collectors.toMap(HomeworkInfo::getCourseId, h -> h, (a, b) -> a)));
    }

    private Map<Long, Map<Long, ClassPerformance>> batchLoadPerformances(Set<Long> studentIds) {
        if (studentIds.isEmpty()) return Collections.emptyMap();
        List<ClassPerformance> list = classPerformanceMapper.selectList(
                new LambdaQueryWrapper<ClassPerformance>().in(ClassPerformance::getStudentId, studentIds));
        return list.stream().collect(Collectors.groupingBy(
                ClassPerformance::getStudentId,
                Collectors.toMap(ClassPerformance::getCourseId, p -> p, (a, b) -> a)));
    }

    private Map<Long, Map<Long, KnowledgeMastery>> batchLoadMasteries(Set<Long> studentIds) {
        if (studentIds.isEmpty()) return Collections.emptyMap();
        List<KnowledgeMastery> list = knowledgeMasteryMapper.selectList(
                new LambdaQueryWrapper<KnowledgeMastery>().in(KnowledgeMastery::getStudentId, studentIds));
        return list.stream().collect(Collectors.groupingBy(
                KnowledgeMastery::getStudentId,
                Collectors.toMap(KnowledgeMastery::getCourseId, m -> m, (a, b) -> a)));
    }

    private Map<Long, Map<Long, HistoryRisk>> batchLoadRisks(Set<Long> studentIds) {
        if (studentIds.isEmpty()) return Collections.emptyMap();
        List<HistoryRisk> list = historyRiskMapper.selectList(
                new LambdaQueryWrapper<HistoryRisk>().in(HistoryRisk::getStudentId, studentIds));
        return list.stream().collect(Collectors.groupingBy(
                HistoryRisk::getStudentId,
                Collectors.toMap(HistoryRisk::getCourseId, r -> r, (a, b) -> a)));
    }

    /** 批量加载学习时长数据 */
    private Map<Long, Map<Long, List<StudyDuration>>> batchLoadStudyDurations(Set<Long> studentIds) {
        if (studentIds.isEmpty()) return Collections.emptyMap();
        List<StudyDuration> list = studyDurationMapper.selectList(
                new LambdaQueryWrapper<StudyDuration>().in(StudyDuration::getStudentId, studentIds)
        );
        if (list.isEmpty()) return Collections.emptyMap();

        // 按 studentId → courseId → List 组织
        Map<Long, Map<Long, List<StudyDuration>>> result = new HashMap<>();
        for (StudyDuration sd : list) {
            result.computeIfAbsent(sd.getStudentId(), k -> new HashMap<>())
                    .computeIfAbsent(sd.getCourseId(), k -> new ArrayList<>())
                    .add(sd);
        }
        return result;
    }

    private BigDecimal bd(double val) {
        return BigDecimal.valueOf(val).setScale(1, RoundingMode.HALF_UP);
    }

    /** v3: 安全获取配置中的double值，null时返回默认值 */
    private double getConfigDouble(BigDecimal val, double defaultVal) {
        return val != null ? val.doubleValue() : defaultVal;
    }

    // ==================== 内部类 ====================
    private static class RiskScore {
        double totalScore;
        double academicScore;
        double homeworkScore;
        double attendanceScore;
        double knowledgeScore;
        double historyScore;
        double studyDurationScore;
        double predictedScore;
    }
}
