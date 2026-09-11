package com.example.academic_risk_warning.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.academic_risk_warning.common.PageResult;
import com.example.academic_risk_warning.entity.*;
import com.example.academic_risk_warning.mapper.*;
import com.example.academic_risk_warning.vo.AlertRecordVO;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 预警查询与管理服务 v2
 * 新增: 状态机校验、操作日志、干预链接、闭环操作(CLOSE/REOPEN)
 */
@Service
public class AlertQueryService {

    /** 风险画像数据范围：本课程 / 综合预警 / 任意课程 / 无数据 */
    private static final String SCOPE_NONE = "NONE";
    private static final String SCOPE_COURSE = "COURSE";
    private static final String SCOPE_COMPREHENSIVE = "COMPREHENSIVE";
    private static final String SCOPE_ANY_COURSE = "ANY_COURSE";

    /** 趋势数据来源：预警快照 / 预警记录 */
    private static final String TREND_SOURCE_SNAPSHOT = "SNAPSHOT";
    private static final String TREND_SOURCE_ALERT = "ALERT_RECORD";

    /** 风险画像取数来源：有效预警 / 每日快照 / 无数据 */
    private static final String SOURCE_ALERT = "ALERT";
    private static final String SOURCE_SNAPSHOT = TREND_SOURCE_SNAPSHOT;
    private static final String SOURCE_NONE = "NONE";

    /** 已撤销/已闭环的预警不再代表"当前风险"，风险画像与班级均值都将其排除 */
    private static final List<String> INVALID_RADAR_STATUSES = List.of("DISMISSED", "CLOSED");

    /** 雷达图维度中文名（用于缺失维度提示） */
    private static final Map<String, String> DIMENSION_LABELS = Map.of(
            "academic", "学业成绩",
            "homework", "作业完成",
            "attendance", "出勤表现",
            "knowledge", "知识掌握",
            "history", "历史风险");

    private final AlertRecordMapper alertRecordMapper;
    private final StudentMapper studentMapper;
    private final TeacherClassMapper teacherClassMapper;
    private final AlertOperationLogMapper alertOperationLogMapper;
    private final InterventionRecordMapper interventionRecordMapper;
    private final AlertSnapshotMapper alertSnapshotMapper;
    private final NotificationService notificationService;
    private final ExerciseMapper exerciseMapper;
    private final ExerciseRecommendationMapper recommendMapper;
    private final ExerciseKnowledgePointMapper exerciseKpMapper;
    private final CourseKnowledgePointMapper kpMapper;
    private final StudentWeakPointMapper weakPointMapper;
    private final ExerciseService exerciseService;

    public AlertQueryService(AlertRecordMapper alertRecordMapper,
                             StudentMapper studentMapper,
                             TeacherClassMapper teacherClassMapper,
                             AlertOperationLogMapper alertOperationLogMapper,
                             InterventionRecordMapper interventionRecordMapper,
                             AlertSnapshotMapper alertSnapshotMapper,
                             NotificationService notificationService,
                             ExerciseMapper exerciseMapper,
                             ExerciseRecommendationMapper recommendMapper,
                             ExerciseKnowledgePointMapper exerciseKpMapper,
                             CourseKnowledgePointMapper kpMapper,
                             StudentWeakPointMapper weakPointMapper,
                             ExerciseService exerciseService) {
        this.alertRecordMapper = alertRecordMapper;
        this.studentMapper = studentMapper;
        this.teacherClassMapper = teacherClassMapper;
        this.alertOperationLogMapper = alertOperationLogMapper;
        this.interventionRecordMapper = interventionRecordMapper;
        this.alertSnapshotMapper = alertSnapshotMapper;
        this.notificationService = notificationService;
        this.exerciseMapper = exerciseMapper;
        this.recommendMapper = recommendMapper;
        this.exerciseKpMapper = exerciseKpMapper;
        this.kpMapper = kpMapper;
        this.weakPointMapper = weakPointMapper;
        this.exerciseService = exerciseService;
    }

    // ==================== 状态机 ====================
    // 允许的流转: fromStatus → [toStatus列表]
    private static final Map<String, List<String>> ALLOWED_TRANSITIONS = new LinkedHashMap<>();

    static {
        ALLOWED_TRANSITIONS.put("ACTIVE",       List.of("ACKNOWLEDGED", "HANDLED", "DISMISSED"));
        ALLOWED_TRANSITIONS.put("ACKNOWLEDGED", List.of("HANDLED", "DISMISSED"));
        ALLOWED_TRANSITIONS.put("HANDLED",      List.of("CLOSED", "ACTIVE", "DISMISSED"));
        ALLOWED_TRANSITIONS.put("DISMISSED",    List.of("ACTIVE"));
        ALLOWED_TRANSITIONS.put("CLOSED",       List.of("ACTIVE"));
        ALLOWED_TRANSITIONS.put("ARCHIVED",     List.of());
    }

    private boolean canTransition(String from, String to) {
        List<String> allowed = ALLOWED_TRANSITIONS.getOrDefault(from, Collections.emptyList());
        return allowed.contains(to);
    }

    // ==================== 查询 ====================

    /** 教师端预警列表 */
    public List<AlertRecordVO> getAlertsByTeacherId(Long teacherId, String status, Long courseId) {
        return getAlertsByTeacherIdPaged(teacherId, status, courseId, 1, Integer.MAX_VALUE).getRecords();
    }

    /** 教师端预警列表(支持分页) */
    public PageResult<AlertRecordVO> getAlertsByTeacherIdPaged(Long teacherId, String status, Long courseId, int page, int size) {
        List<TeacherClass> teacherClasses = teacherClassMapper.selectList(
                new LambdaQueryWrapper<TeacherClass>().eq(TeacherClass::getTeacherId, teacherId));
        if (teacherClasses.isEmpty()) return PageResult.empty();

        List<String> classNames = teacherClasses.stream()
                .map(TeacherClass::getClassName).filter(Objects::nonNull).distinct().toList();
        if (classNames.isEmpty()) return PageResult.empty();

        List<StudentIdRange> studentRanges = teacherClasses.stream()
                .filter(tc -> tc.getStudentIdStart() != null && tc.getStudentIdEnd() != null)
                .map(tc -> new StudentIdRange(tc.getStudentIdStart(), tc.getStudentIdEnd())).toList();

        List<Student> students = studentMapper.selectList(
                new LambdaQueryWrapper<Student>().in(Student::getClassName, classNames));
        if (students.isEmpty()) return PageResult.empty();

        if (!studentRanges.isEmpty()) {
            students = students.stream()
                    .filter(s -> isStudentInAnyRange(s.getId(), studentRanges)).toList();
            if (students.isEmpty()) return PageResult.empty();
        }

        List<Long> studentIds = students.stream().map(Student::getId).toList();

        LambdaQueryWrapper<AlertRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(AlertRecord::getStudentId, studentIds)
                .ne(AlertRecord::getStatus, "ARCHIVED")
                .orderByDesc(AlertRecord::getCreateTime);
        if (status != null && !status.isEmpty()) {
            wrapper.eq(AlertRecord::getStatus, status);
        }
        if (courseId != null) {
            wrapper.eq(AlertRecord::getCourseId, courseId);
        }

        Page<AlertRecord> p = new Page<>(page, size);
        Page<AlertRecord> resultPage = alertRecordMapper.selectPage(p, wrapper);
        List<AlertRecordVO> records = convertToVOList(resultPage.getRecords(), students);
        return new PageResult<>(resultPage.getTotal(), page, size, records);
    }

    /** 学生端预警列表 */
    public List<AlertRecordVO> getAlertsByStudentId(Long studentId, String status) {
        LambdaQueryWrapper<AlertRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AlertRecord::getStudentId, studentId)
                .ne(AlertRecord::getStatus, "ARCHIVED")
                .orderByDesc(AlertRecord::getCreateTime);
        if (status != null && !status.isEmpty()) {
            wrapper.eq(AlertRecord::getStatus, status);
        }
        Student student = studentMapper.selectById(studentId);
        return convertToVOList(alertRecordMapper.selectList(wrapper),
                student != null ? List.of(student) : Collections.emptyList());
    }

    /** 查询单条预警详情(含关联干预记录) */
    public AlertRecordVO getAlertDetail(Long alertId) {
        AlertRecord alert = alertRecordMapper.selectById(alertId);
        if (alert == null) return null;
        Student student = studentMapper.selectById(alert.getStudentId());
        List<AlertRecordVO> vos = convertToVOList(List.of(alert),
                student != null ? List.of(student) : Collections.emptyList());
        AlertRecordVO vo = vos.isEmpty() ? null : vos.get(0);
        if (vo != null) {
            // 加载关联干预记录
            List<InterventionRecord> interventions = interventionRecordMapper.selectList(
                    new LambdaQueryWrapper<InterventionRecord>()
                            .eq(InterventionRecord::getAlertId, alertId)
                            .orderByDesc(InterventionRecord::getCreateTime));
            // 简单包装: 把干预类型名列表放入extra
            if (!CollectionUtils.isEmpty(interventions)) {
                vo.setInterventionTypes(interventions.stream()
                        .map(i -> getInterventionTypeName(i.getInterventionType())).toList());
            }
            // 加载操作日志
            List<AlertOperationLog> logs = alertOperationLogMapper.selectList(
                    new LambdaQueryWrapper<AlertOperationLog>()
                            .eq(AlertOperationLog::getAlertId, alertId)
                            .orderByAsc(AlertOperationLog::getCreateTime));
            if (!CollectionUtils.isEmpty(logs)) {
                vo.setOperationLogs(logs.stream().map(log -> {
                    AlertRecordVO.OpLog ol = new AlertRecordVO.OpLog();
                    ol.setOperatorType(log.getOperatorType());
                    ol.setOperationName(getOperationName(log.getOperation()));
                    ol.setRemark(log.getRemark());
                    ol.setCreateTime(log.getCreateTime());
                    return ol;
                }).toList());
            }
        }
        return vo;
    }

    /** 预警统计 */
    public Map<String, Object> getAlertStatistics(Long teacherId, Long courseId) {
        List<AlertRecordVO> alerts = getAlertsByTeacherId(teacherId, null, courseId);
        Map<String, Object> stats = new HashMap<>();
        stats.put("total", alerts.size());
        stats.put("redCount", alerts.stream().filter(a -> "RED".equals(a.getAlertLevel())).count());
        stats.put("orangeCount", alerts.stream().filter(a -> "ORANGE".equals(a.getAlertLevel())).count());
        stats.put("yellowCount", alerts.stream().filter(a -> "YELLOW".equals(a.getAlertLevel())).count());
        stats.put("activeCount", alerts.stream().filter(a -> "ACTIVE".equals(a.getStatus())).count());
        stats.put("acknowledgedCount", alerts.stream().filter(a -> "ACKNOWLEDGED".equals(a.getStatus())).count());
        stats.put("handledCount", alerts.stream().filter(a -> "HANDLED".equals(a.getStatus())).count());
        stats.put("closedCount", alerts.stream().filter(a -> "CLOSED".equals(a.getStatus())).count());

        Map<String, Long> typeCount = new HashMap<>();
        alerts.forEach(a -> typeCount.merge(a.getAlertType(), 1L, Long::sum));
        stats.put("typeCount", typeCount);
        stats.put("studentsWithAlerts", alerts.stream().map(AlertRecordVO::getStudentId).distinct().count());
        return stats;
    }

    // ==================== 操作(带状态机校验+日志) ====================

    /** 学生确认预警 */
    @Transactional
    public boolean acknowledgeAlert(Long alertId, Long studentId) {
        AlertRecord alert = alertRecordMapper.selectById(alertId);
        if (alert == null || !alert.getStudentId().equals(studentId)) return false;
        if (!canTransition(alert.getStatus(), "ACKNOWLEDGED")) return false;

        String oldStatus = alert.getStatus();
        alert.setStatus("ACKNOWLEDGED");
        alert.setAckTime(LocalDateTime.now());
        boolean ok = alertRecordMapper.updateById(alert) > 0;
        if (ok) logOperation(alertId, studentId, "STUDENT", "ACKNOWLEDGE", oldStatus, "ACKNOWLEDGED", null);
        return ok;
    }

    /** 教师处理预警(带干预记录，支持同步发送练习题) */
    @Transactional
    public boolean handleAlert(Long alertId, Long teacherId, String note,
                               String interventionType, String interventionDesc,
                               List<Long> exerciseIds) {
        AlertRecord alert = alertRecordMapper.selectById(alertId);
        if (alert == null) return false;
        if (!canTransition(alert.getStatus(), "HANDLED")) return false;

        String oldStatus = alert.getStatus();
        alert.setStatus("HANDLED");
        alert.setTeacherNote(note);
        alert.setHandleTime(LocalDateTime.now());
        boolean ok = alertRecordMapper.updateById(alert) > 0;
        if (ok) {
            logOperation(alertId, teacherId, "TEACHER", "HANDLE", oldStatus, "HANDLED", note);
            // 自动创建干预记录
            if (interventionType != null && !interventionType.isEmpty()) {
                InterventionRecord ir = new InterventionRecord();
                ir.setAlertId(alertId);
                ir.setStudentId(alert.getStudentId());
                ir.setTeacherId(teacherId);
                ir.setInterventionType(interventionType);
                ir.setDescription(interventionDesc);
                ir.setStatus("EXECUTING");
                ir.setRiskScoreBefore(alert.getRiskScore());
                ir.setCreateTime(LocalDateTime.now());
                ir.setUpdateTime(LocalDateTime.now());
                interventionRecordMapper.insert(ir);
            }
            // 批量创建题目推荐记录(教师选择了练习题)
            int exerciseCount = 0;
            if (exerciseIds != null && !exerciseIds.isEmpty()) {
                Long studentId = alert.getStudentId();
                List<Exercise> exercises = exerciseMapper.selectBatchIds(exerciseIds);
                // 过滤有效的练习题
                List<Exercise> validExercises = exercises.stream()
                        .filter(e -> e != null && Integer.valueOf(1).equals(e.getIsActive()))
                        .toList();
                if (!validExercises.isEmpty()) {
                    // 批量预加载所有 exercise_knowledge_point（消除 N+1）
                    List<Long> validExerciseIds = validExercises.stream().map(Exercise::getId).toList();
                    List<ExerciseKnowledgePoint> allEkps = exerciseKpMapper.selectList(
                            new LambdaQueryWrapper<ExerciseKnowledgePoint>()
                                    .in(ExerciseKnowledgePoint::getExerciseId, validExerciseIds));
                    // 构建 exerciseId → [kpIds] 映射
                    Map<Long, List<Long>> exerciseKpMap = new HashMap<>();
                    Set<Long> allKpIds = new HashSet<>();
                    for (ExerciseKnowledgePoint ekp : allEkps) {
                        exerciseKpMap.computeIfAbsent(ekp.getExerciseId(), k -> new ArrayList<>()).add(ekp.getKpId());
                        allKpIds.add(ekp.getKpId());
                    }
                    // 批量预加载所有知识点名称（消除 N+1）
                    Map<Long, String> kpNameMap = new HashMap<>();
                    if (!allKpIds.isEmpty()) {
                        List<CourseKnowledgePoint> allKps = kpMapper.selectList(
                                new LambdaQueryWrapper<CourseKnowledgePoint>().in(CourseKnowledgePoint::getId, allKpIds));
                        for (CourseKnowledgePoint kp : allKps) {
                            kpNameMap.put(kp.getId(), kp.getName());
                        }
                    }
                    // 创建推荐记录
                    for (Exercise exercise : validExercises) {
                        ExerciseRecommendation rec = new ExerciseRecommendation();
                        rec.setExerciseId(exercise.getId());
                        rec.setStudentId(studentId);
                        rec.setAlertId(alertId);
                        // 从预加载缓存获取知识点名称
                        List<Long> relKpIds = exerciseKpMap.getOrDefault(exercise.getId(), Collections.emptyList());
                        String kpName = relKpIds.stream()
                                .map(kpid -> kpNameMap.getOrDefault(kpid, ""))
                                .filter(s -> !s.isEmpty())
                                .collect(Collectors.joining("、"));
                        rec.setKnowledgePoint(!kpName.isEmpty() ? kpName :
                                (exercise.getTitle() != null ? exercise.getTitle() : "综合练习"));
                        rec.setStatus("PENDING");
                        rec.setRecommendTime(LocalDateTime.now());
                        recommendMapper.insert(rec);
                    }
                    exerciseCount = validExercises.size();
                }
            }
            // 通知学生
            if (exerciseCount > 0) {
                notificationService.notifyAlertHandledWithExercises(
                        alertId, alert.getStudentId(), teacherId, note, exerciseCount);
            } else {
                notificationService.notifyAlertHandled(alertId, alert.getStudentId(), teacherId, note);
            }
            // 自动同步生成薄弱知识点记录（供后续练习推荐匹配使用）
            try {
                exerciseService.generateWeakPoints(alert.getStudentId(), alert.getCourseId(), alertId);
            } catch (Exception ignored) { /* 薄弱点生成失败不影响主流程 */ }
        }
        return ok;
    }

    /** 教师撤销预警 */
    @Transactional
    public boolean dismissAlert(Long alertId, Long teacherId, String reason) {
        AlertRecord alert = alertRecordMapper.selectById(alertId);
        if (alert == null) return false;
        if (!canTransition(alert.getStatus(), "DISMISSED")) return false;

        String oldStatus = alert.getStatus();
        alert.setStatus("DISMISSED");
        alert.setTeacherNote(reason);
        alert.setHandleTime(LocalDateTime.now());
        boolean ok = alertRecordMapper.updateById(alert) > 0;
        if (ok) logOperation(alertId, teacherId, "TEACHER", "DISMISS", oldStatus, "DISMISSED", reason);
        return ok;
    }

    /** 教师关闭预警(闭环完成) */
    @Transactional
    public boolean closeAlert(Long alertId, Long teacherId, String note) {
        AlertRecord alert = alertRecordMapper.selectById(alertId);
        if (alert == null) return false;
        if (!canTransition(alert.getStatus(), "CLOSED")) return false;

        String oldStatus = alert.getStatus();
        alert.setStatus("CLOSED");
        alert.setTeacherNote(note);
        alert.setHandleTime(LocalDateTime.now());
        boolean ok = alertRecordMapper.updateById(alert) > 0;
        if (ok) {
            logOperation(alertId, teacherId, "TEACHER", "CLOSE", oldStatus, "CLOSED", note);
            // 关闭关联的未完成干预记录
            List<InterventionRecord> interventions = interventionRecordMapper.selectList(
                    new LambdaQueryWrapper<InterventionRecord>()
                            .eq(InterventionRecord::getAlertId, alertId)
                            .eq(InterventionRecord::getStatus, "EXECUTING"));
            for (InterventionRecord ir : interventions) {
                ir.setStatus("COMPLETED");
                ir.setUpdateTime(LocalDateTime.now());
                interventionRecordMapper.updateById(ir);
            }
        }
        return ok;
    }

    /** 教师重新打开预警(干预无效/误关闭) */
    @Transactional
    public boolean reopenAlert(Long alertId, Long teacherId, String reason) {
        AlertRecord alert = alertRecordMapper.selectById(alertId);
        if (alert == null) return false;
        if (!canTransition(alert.getStatus(), "ACTIVE")) return false;

        String oldStatus = alert.getStatus();
        alert.setStatus("ACTIVE");
        alert.setHandleTime(LocalDateTime.now());
        boolean ok = alertRecordMapper.updateById(alert) > 0;
        if (ok) logOperation(alertId, teacherId, "TEACHER", "REOPEN", oldStatus, "ACTIVE", reason);
        return ok;
    }

    /** 学生回应干预(已处理预警学生反馈改进情况, 限一次) */
    @Transactional
    public boolean studentRespondToAlert(Long alertId, Long studentId, String response) {
        AlertRecord alert = alertRecordMapper.selectById(alertId);
        if (alert == null || !alert.getStudentId().equals(studentId)) return false;
        // 只有 HANDLED/CLOSED 状态学生才能回应
        if (!"HANDLED".equals(alert.getStatus()) && !"CLOSED".equals(alert.getStatus())) return false;
        // 已经回应过则拒绝
        if (alert.getTeacherNote() != null && alert.getTeacherNote().contains("【学生回应】")) {
            return false;
        }

        // 追加学生回应到 teachersNote
        String currentNote = alert.getTeacherNote() != null ? alert.getTeacherNote() : "";
        alert.setTeacherNote(currentNote + "\n【学生回应】" + response);
        boolean ok = alertRecordMapper.updateById(alert) > 0;
        if (ok) {
            logOperation(alertId, studentId, "STUDENT", "RESPOND", alert.getStatus(), alert.getStatus(),
                    "学生回应: " + response);
            // 通知教师
            Long teacherId = alert.getHandleTime() != null ? findTeacherForAlert(alertId) : null;
            if (teacherId != null) {
                notificationService.notifyStudentResponded(alertId, studentId, teacherId, response);
            }
        }
        return ok;
    }

    /** 查询某预警的操作日志 */
    public List<AlertOperationLog> getOperationLogs(Long alertId) {
        return alertOperationLogMapper.selectList(
                new LambdaQueryWrapper<AlertOperationLog>()
                        .eq(AlertOperationLog::getAlertId, alertId)
                        .orderByAsc(AlertOperationLog::getCreateTime));
    }

    // ==================== 批量操作 ====================

    /** 批量处理预警(批量操作不附带练习题) */
    @Transactional
    public int batchHandle(List<Long> alertIds, Long teacherId, String note,
                           String interventionType, String interventionDesc) {
        int count = 0;
        for (Long id : alertIds) {
            if (handleAlert(id, teacherId, note, interventionType, interventionDesc, null)) count++;
        }
        return count;
    }

    /** 批量撤销预警 */
    @Transactional
    public int batchDismiss(List<Long> alertIds, Long teacherId, String reason) {
        int count = 0;
        for (Long id : alertIds) {
            if (dismissAlert(id, teacherId, reason)) count++;
        }
        return count;
    }

    /** 批量关闭预警 */
    @Transactional
    public int batchClose(List<Long> alertIds, Long teacherId, String note) {
        int count = 0;
        for (Long id : alertIds) {
            if (closeAlert(id, teacherId, note)) count++;
        }
        return count;
    }

    // ==================== 练习题推荐（处理预警时选择题目） ====================

    /**
     * 根据预警ID获取可选练习题列表
     * 逻辑：预警 → 学生薄弱知识点 → 匹配题库中对应知识点的题目
     */
    public List<Map<String, Object>> getAvailableExercisesForAlert(Long alertId) {
        AlertRecord alert = alertRecordMapper.selectById(alertId);
        if (alert == null || alert.getCourseId() == null) return Collections.emptyList();

        Long studentId = alert.getStudentId();
        Long courseId = alert.getCourseId();

        // 1. 获取学生在该课程下的薄弱知识点 kp_id 列表
        List<StudentWeakPoint> weakPoints = weakPointMapper.selectList(
                new LambdaQueryWrapper<StudentWeakPoint>()
                        .eq(StudentWeakPoint::getStudentId, studentId)
                        .eq(StudentWeakPoint::getCourseId, courseId)
                        .orderByDesc(StudentWeakPoint::getErrorRate));

        List<Long> kpIds = weakPoints.stream()
                .map(StudentWeakPoint::getKpId).distinct().toList();
        if (kpIds.isEmpty()) return Collections.emptyList();

        // 2. 构建 kp_id → 薄弱点信息 映射
        Map<Long, StudentWeakPoint> wpMap = new LinkedHashMap<>();
        for (StudentWeakPoint wp : weakPoints) {
            wpMap.putIfAbsent(wp.getKpId(), wp);
        }

        // 3. 通过 exercise_knowledge_point 关联表查匹配的 exercise_id
        List<ExerciseKnowledgePoint> ekps = exerciseKpMapper.selectList(
                new LambdaQueryWrapper<ExerciseKnowledgePoint>()
                        .in(ExerciseKnowledgePoint::getKpId, kpIds));
        Set<Long> exerciseIds = ekps.stream()
                .map(ExerciseKnowledgePoint::getExerciseId).collect(Collectors.toSet());
        if (exerciseIds.isEmpty()) return Collections.emptyList();

        // 4. 查题目详情（仅已审核通过的）
        List<Exercise> exercises = exerciseMapper.selectList(
                new LambdaQueryWrapper<Exercise>()
                        .in(Exercise::getId, exerciseIds)
                        .eq(Exercise::getCourseId, courseId)
                        .eq(Exercise::getIsActive, 1)
                        .eq(Exercise::getAuditStatus, "APPROVED")
                        .orderByAsc(Exercise::getDifficulty));

        // 5. 批量预加载知识点关联和名称（消除 N+1）
        List<Long> resultExerciseIds = exercises.stream().map(Exercise::getId).toList();
        Map<Long, List<Long>> exerciseKpIdMap = new HashMap<>(); // exerciseId → [kpIds]
        Map<Long, String> kpNameCache = new HashMap<>();
        if (!resultExerciseIds.isEmpty()) {
            List<ExerciseKnowledgePoint> allRelEkps = exerciseKpMapper.selectList(
                    new LambdaQueryWrapper<ExerciseKnowledgePoint>().in(ExerciseKnowledgePoint::getExerciseId, resultExerciseIds));
            Set<Long> allRelKpIds = new HashSet<>();
            for (ExerciseKnowledgePoint ekp : allRelEkps) {
                exerciseKpIdMap.computeIfAbsent(ekp.getExerciseId(), k -> new ArrayList<>()).add(ekp.getKpId());
                allRelKpIds.add(ekp.getKpId());
            }
            // 批量加载所有知识点名称
            if (!allRelKpIds.isEmpty()) {
                List<CourseKnowledgePoint> allKps = kpMapper.selectList(
                        new LambdaQueryWrapper<CourseKnowledgePoint>().in(CourseKnowledgePoint::getId, allRelKpIds));
                for (CourseKnowledgePoint kp : allKps) {
                    kpNameCache.put(kp.getId(), kp.getName());
                }
            }
        }

        List<Map<String, Object>> result = new ArrayList<>();
        for (Exercise e : exercises) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", e.getId());
            m.put("title", e.getTitle());
            m.put("description", e.getDescription());
            m.put("difficulty", e.getDifficulty());
            m.put("fileUrl", e.getFileUrl());
            m.put("courseId", e.getCourseId());

            List<Long> relKpIds = exerciseKpIdMap.getOrDefault(e.getId(), Collections.emptyList());
            List<String> kpNames = relKpIds.stream()
                    .map(kpid -> kpNameCache.getOrDefault(kpid, ""))
                    .filter(s -> !s.isEmpty())
                    .toList();
            m.put("kpIds", relKpIds);
            m.put("knowledgePointNames", String.join("、", kpNames));

            // 标记哪些是学生的薄弱知识点(用于前端高亮)
            List<String> matchedWeakPoints = new ArrayList<>();
            for (Long kpId : relKpIds) {
                if (wpMap.containsKey(kpId)) {
                    String name = kpNameCache.get(kpId);
                    matchedWeakPoints.add(name != null ? name : "薄弱点" + kpId);
                }
            }
            m.put("matchedWeakPoints", matchedWeakPoints);
            m.put("matchCount", matchedWeakPoints.size());

            result.add(m);
        }

        // 按匹配薄弱点数量降序排列(最相关优先)
        result.sort((a, b) -> {
            int cmp = Integer.compare((int) b.get("matchCount"), (int) a.get("matchCount"));
            if (cmp != 0) return cmp;
            // 次按难度排序：EASY < MEDIUM < HARD
            return difficultyOrder((String) a.get("difficulty"))
                    - difficultyOrder((String) b.get("difficulty"));
        });

        return result;
    }

    private int difficultyOrder(String d) {
        return switch (d) {
            case "EASY" -> 1;
            case "MEDIUM" -> 2;
            case "HARD" -> 3;
            default -> 4;
        };
    }

    // ==================== 可视化API ====================

    /** 教师仪表盘摘要 */
    public Map<String, Object> getDashboard(Long teacherId, Long courseId) {
        List<AlertRecordVO> alerts = getAlertsByTeacherId(teacherId, null, courseId);
        Map<String, Object> dash = new LinkedHashMap<>();

        long total = alerts.size();
        long active = alerts.stream().filter(a -> "ACTIVE".equals(a.getStatus())).count();
        long handled = alerts.stream().filter(a -> "HANDLED".equals(a.getStatus())).count();
        long closed = alerts.stream().filter(a -> "CLOSED".equals(a.getStatus())).count();
        long dismissed = alerts.stream().filter(a -> "DISMISSED".equals(a.getStatus())).count();
        long red = alerts.stream().filter(a -> "RED".equals(a.getAlertLevel())).count();
        long orange = alerts.stream().filter(a -> "ORANGE".equals(a.getAlertLevel())).count();
        long yellow = alerts.stream().filter(a -> "YELLOW".equals(a.getAlertLevel())).count();
        long studentsAtRisk = alerts.stream().map(AlertRecordVO::getStudentId).distinct().count();

        // 处理率
        double procRate = total > 0 ? ((handled + closed) * 100.0 / total) : 0;

        // 本周新增
        LocalDateTime weekAgo = LocalDateTime.now().minusDays(7);
        long weekNew = alerts.stream()
                .filter(a -> a.getCreateTime() != null && a.getCreateTime().isAfter(weekAgo)).count();

        // 平均响应时间(分钟)
        double avgResp = alerts.stream()
                .filter(a -> a.getHandleTime() != null && a.getCreateTime() != null)
                .mapToLong(a -> java.time.Duration.between(a.getCreateTime(), a.getHandleTime()).toMinutes())
                .average().orElse(0);

        dash.put("total", total);
        dash.put("activeCount", active);
        dash.put("handledCount", handled);
        dash.put("closedCount", closed);
        dash.put("dismissedCount", dismissed);
        dash.put("processingRate", Math.round(procRate * 10) / 10.0);
        dash.put("avgResponseMinutes", Math.round(avgResp));
        dash.put("redCount", red);
        dash.put("orangeCount", orange);
        dash.put("yellowCount", yellow);
        dash.put("studentsAtRisk", studentsAtRisk);
        dash.put("weekNewCount", weekNew);
        dash.put("levelDistribution", Map.of("RED", red, "ORANGE", orange, "YELLOW", yellow));
        return dash;
    }

    /** Top N 风险学生排行 */
    public List<Map<String, Object>> getTopRiskStudents(Long teacherId, int limit, Long courseId) {
        List<AlertRecordVO> alerts = getAlertsByTeacherId(teacherId, null, courseId);
        // 去重: 每个学生取最新一条
        Map<Long, AlertRecordVO> latestMap = new LinkedHashMap<>();
        for (AlertRecordVO a : alerts) {
            latestMap.putIfAbsent(a.getStudentId(), a);
        }
        return latestMap.values().stream()
                .filter(a -> a.getRiskScore() != null)
                .sorted((a, b) -> b.getRiskScore().compareTo(a.getRiskScore()))
                .limit(limit)
                .map(a -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("studentId", a.getStudentId());
                    m.put("studentName", a.getStudentName());
                    m.put("studentNo", a.getStudentNo());
                    m.put("riskScore", a.getRiskScore());
                    m.put("alertLevel", a.getAlertLevel()); // 原始值用于颜色判断
                    m.put("alertLevelName", a.getAlertLevelName());
                    m.put("alertType", a.getAlertTypeName());
                    return m;
                }).toList();
    }

    /**
     * 学生风险画像雷达数据(5维分 + 班级均值 + 数据口径说明)
     *
     * <p>取数优先级：**仍有效的预警**（已撤销/已闭环不算）→ 没有预警时退回**每日快照**。
     * 方案A之后每个选了课的学生每天都有快照，因此"学情数据齐全但没触发过预警"的学生
     * 也能看到真实五维画像，而不是被填成 0；返回的 {@code source} 标明数字来自 ALERT 还是 SNAPSHOT。
     */
    public Map<String, Object> getStudentRadar(Long studentId, Long courseId) {
        Map<String, Object> radar = new LinkedHashMap<>();

        RadarSource src = pickRadarSource(studentId, courseId);
        String scope = src.scope();
        String source = src.alert() != null ? SOURCE_ALERT
                : (src.snapshot() != null ? SOURCE_SNAPSHOT : SOURCE_NONE);

        Map<String, Object> student = new LinkedHashMap<>();
        List<String> missingDimensions = new ArrayList<>();
        if (src.alert() != null) {
            AlertRecord alert = src.alert();
            putDimension(student, "academic", alert.getAcademicRiskScore(), missingDimensions);
            putDimension(student, "homework", alert.getHomeworkRiskScore(), missingDimensions);
            putDimension(student, "attendance", alert.getAttendanceRiskScore(), missingDimensions);
            putDimension(student, "knowledge", alert.getKnowledgeRiskScore(), missingDimensions);
            putDimension(student, "history", alert.getHistoryRiskScore(), missingDimensions);
            student.put("riskScore", alert.getRiskScore());
            student.put("alertLevel", alert.getAlertLevel());
            student.put("status", alert.getStatus());
            student.put("hasData", true);
        } else if (src.snapshot() != null) {
            AlertSnapshot snap = src.snapshot();
            putDimension(student, "academic", snap.getAcademicRiskScore(), missingDimensions);
            putDimension(student, "homework", snap.getHomeworkRiskScore(), missingDimensions);
            putDimension(student, "attendance", snap.getAttendanceRiskScore(), missingDimensions);
            putDimension(student, "knowledge", snap.getKnowledgeRiskScore(), missingDimensions);
            putDimension(student, "history", snap.getHistoryRiskScore(), missingDimensions);
            student.put("riskScore", snap.getRiskScore());
            student.put("alertLevel", snap.getAlertLevel());
            student.put("status", SOURCE_SNAPSHOT);
            student.put("dataDate", snap.getSnapshotDate());
            student.put("hasData", true);
        } else {
            for (String k : new String[]{"academic", "homework", "attendance", "knowledge", "history"}) {
                student.put(k, 0.0);
            }
            student.put("hasData", false);
        }
        radar.put("student", student);
        radar.put("scope", scope);
        radar.put("source", source);
        radar.put("scopeText", scopeText(scope, source,
                src.snapshot() != null ? src.snapshot().getSnapshotDate() : null));
        radar.put("missingDimensions", missingDimensions);
        radar.put("missingDimensionTexts",
                missingDimensions.stream().map(DIMENSION_LABELS::get).toList());

        // 班级均值: 优先"每人最新一条有效预警"，班级没有预警时退回"每人最新快照"
        Student stu = studentMapper.selectById(studentId);
        if (stu != null && stu.getClassName() != null) {
            List<Student> classmates = studentMapper.selectList(
                    new LambdaQueryWrapper<Student>().eq(Student::getClassName, stu.getClassName()));
            if (classmates.size() > 1) {
                List<Long> cids = classmates.stream().map(Student::getId).toList();

                // 班级均值与"学生本人"保持同源：个人走快照时班级也用快照，避免两套口径混在一张图里
                Map<String, Object> avg = new LinkedHashMap<>();
                String classSource;
                String classScope = courseId != null ? SCOPE_COURSE : SCOPE_COMPREHENSIVE;
                if (SOURCE_SNAPSHOT.equals(source)) {
                    List<AlertSnapshot> classSnaps = latestSnapshotByStudent(cids, courseId);
                    if (classSnaps.isEmpty() && courseId != null) {
                        classSnaps = latestSnapshotByStudent(cids, null);
                        classScope = SCOPE_COMPREHENSIVE;
                    }
                    if (classSnaps.isEmpty()) {
                        classSnaps = latestSnapshotByStudentAny(cids);
                        classScope = SCOPE_ANY_COURSE;
                    }
                    classSource = classSnaps.isEmpty() ? SOURCE_NONE : SOURCE_SNAPSHOT;
                    avg.put("academic", avgValue(classSnaps, AlertSnapshot::getAcademicRiskScore));
                    avg.put("homework", avgValue(classSnaps, AlertSnapshot::getHomeworkRiskScore));
                    avg.put("attendance", avgValue(classSnaps, AlertSnapshot::getAttendanceRiskScore));
                    avg.put("knowledge", avgValue(classSnaps, AlertSnapshot::getKnowledgeRiskScore));
                    avg.put("history", avgValue(classSnaps, AlertSnapshot::getHistoryRiskScore));
                } else {
                    // 个人走预警 → 班级均值优先用预警；班级完全没有预警时才退回快照
                    List<AlertRecord> classAlerts = latestAlertByStudent(cids, courseId);
                    if (classAlerts.isEmpty() && courseId != null) {
                        classAlerts = latestAlertByStudent(cids, null);
                        classScope = SCOPE_COMPREHENSIVE;
                    }
                    if (classAlerts.isEmpty()) {
                        classAlerts = latestAlertByStudentAny(cids);
                        classScope = SCOPE_ANY_COURSE;
                    }
                    if (!classAlerts.isEmpty()) {
                        classSource = SOURCE_ALERT;
                        avg.put("academic", avgValue(classAlerts, AlertRecord::getAcademicRiskScore));
                        avg.put("homework", avgValue(classAlerts, AlertRecord::getHomeworkRiskScore));
                        avg.put("attendance", avgValue(classAlerts, AlertRecord::getAttendanceRiskScore));
                        avg.put("knowledge", avgValue(classAlerts, AlertRecord::getKnowledgeRiskScore));
                        avg.put("history", avgValue(classAlerts, AlertRecord::getHistoryRiskScore));
                    } else {
                        List<AlertSnapshot> classSnaps = latestSnapshotByStudent(cids, courseId);
                        if (classSnaps.isEmpty() && courseId != null) {
                            classSnaps = latestSnapshotByStudent(cids, null);
                            classScope = SCOPE_COMPREHENSIVE;
                        }
                        if (classSnaps.isEmpty()) {
                            classSnaps = latestSnapshotByStudentAny(cids);
                            classScope = SCOPE_ANY_COURSE;
                        }
                        classSource = classSnaps.isEmpty() ? SOURCE_NONE : SOURCE_SNAPSHOT;
                        avg.put("academic", avgValue(classSnaps, AlertSnapshot::getAcademicRiskScore));
                        avg.put("homework", avgValue(classSnaps, AlertSnapshot::getHomeworkRiskScore));
                        avg.put("attendance", avgValue(classSnaps, AlertSnapshot::getAttendanceRiskScore));
                        avg.put("knowledge", avgValue(classSnaps, AlertSnapshot::getKnowledgeRiskScore));
                        avg.put("history", avgValue(classSnaps, AlertSnapshot::getHistoryRiskScore));
                    }
                }

                // 若均值全为 0, 说明班级暂无数据, 不返回 classAvg
                boolean anyNonZero = avg.values().stream()
                        .anyMatch(v -> v instanceof Number && ((Number) v).doubleValue() > 0);
                if (anyNonZero) {
                    radar.put("classAvg", avg);
                    radar.put("classAvgSource", classSource);
                    radar.put("classAvgScope", classScope);
                    radar.put("classAvgScopeText", scopeText(classScope, classSource, null));
                }
            }
        }

        return radar;
    }

    /** 雷达取数来源：有效预警 → 每日快照 → 空 */
    private record RadarSource(AlertRecord alert, AlertSnapshot snapshot, String scope) {}

    private RadarSource pickRadarSource(Long studentId, Long courseId) {
        // 1) 有效预警三级兜底: 指定课程 → 综合预警(course_id IS NULL) → 任意课程
        if (courseId != null) {
            AlertRecord alert = latestAlert(studentId, courseId, false);
            if (alert != null) return new RadarSource(alert, null, SCOPE_COURSE);
        }
        if (courseId != null) {
            AlertRecord alert = latestAlert(studentId, null, true);
            if (alert != null) return new RadarSource(alert, null, SCOPE_COMPREHENSIVE);
        }
        AlertRecord anyAlert = latestAlert(studentId, null, false);
        if (anyAlert != null) return new RadarSource(anyAlert, null, SCOPE_ANY_COURSE);

        // 2) 没有有效预警 → 退回每日快照(方案A: 每个选了课的学生每天一条)
        if (courseId != null) {
            AlertSnapshot snap = latestSnapshot(studentId, courseId, false);
            if (snap != null) return new RadarSource(null, snap, SCOPE_COURSE);
        }
        if (courseId != null) {
            AlertSnapshot snap = latestSnapshot(studentId, null, true);
            if (snap != null) return new RadarSource(null, snap, SCOPE_COMPREHENSIVE);
        }
        AlertSnapshot anySnap = latestSnapshot(studentId, null, false);
        if (anySnap != null) return new RadarSource(null, anySnap, SCOPE_ANY_COURSE);

        return new RadarSource(null, null, SCOPE_NONE);
    }

    /** 雷达图维度取值：为空时记 0 分并登记缺失，便于前端提示"该维度暂无数据" */
    private void putDimension(Map<String, Object> target, String key, BigDecimal value, List<String> missing) {
        if (value == null) {
            target.put(key, 0.0);
            missing.add(key);
        } else {
            target.put(key, value.doubleValue());
        }
    }

    /** 数据口径文案：说明这张画像来自哪个范围、以及是预警还是快照 */
    private String scopeText(String scope, String source, LocalDate snapshotDate) {
        boolean fromSnapshot = SOURCE_SNAPSHOT.equals(source);
        String base = switch (scope) {
            case SCOPE_COURSE -> fromSnapshot ? "本课程最新每日快照" : "本课程最新有效预警";
            case SCOPE_COMPREHENSIVE -> fromSnapshot ? "综合快照（未分课程）" : "综合预警（未分课程）";
            case SCOPE_ANY_COURSE -> fromSnapshot ? "该课程暂无数据，展示最近一次其他课程快照"
                                                  : "该课程暂无有效预警，展示最近一次其他课程预警";
            default -> "暂无预警与快照数据";
        };
        return fromSnapshot && snapshotDate != null ? base + "（" + snapshotDate + "）" : base;
    }

    /** 通用均值计算：预警与快照共用 */
    private <T> double avgValue(List<T> list, java.util.function.Function<T, BigDecimal> getter) {
        return list.stream()
                .map(getter)
                .filter(Objects::nonNull)
                .mapToDouble(BigDecimal::doubleValue)
                .average().orElse(0);
    }

    /**
     * 取指定学生集合在某一课程（或综合预警）下的最新一条预警，按学生去重
     */
    /** 取单个学生最新一条有效预警; courseId 为空表示"任意课程", onlyComprehensive=true 时只查综合预警 */
    private AlertRecord latestAlert(Long studentId, Long courseId, boolean onlyComprehensive) {
        LambdaQueryWrapper<AlertRecord> wrapper = new LambdaQueryWrapper<AlertRecord>()
                .eq(AlertRecord::getStudentId, studentId)
                .notIn(AlertRecord::getStatus, INVALID_RADAR_STATUSES)
                .orderByDesc(AlertRecord::getCreateTime);
        if (onlyComprehensive) {
            wrapper.isNull(AlertRecord::getCourseId);
        } else if (courseId != null) {
            wrapper.eq(AlertRecord::getCourseId, courseId);
        }
        return alertRecordMapper.selectOne(wrapper.last("LIMIT 1"));
    }

    /**
     * 取指定学生集合在某一课程（或综合预警）下的最新一条有效预警，按学生去重。
     * courseId 为空时只匹配综合预警(course_id IS NULL)
     */
    private List<AlertRecord> latestAlertByStudent(List<Long> studentIds, Long courseId) {
        if (studentIds == null || studentIds.isEmpty()) return List.of();
        Page<AlertRecord> page = new Page<>(1, 500);
        Page<AlertRecord> result = alertRecordMapper.selectPage(page,
                new LambdaQueryWrapper<AlertRecord>()
                        .in(AlertRecord::getStudentId, studentIds)
                        .notIn(AlertRecord::getStatus, INVALID_RADAR_STATUSES)
                        .eq(courseId != null, AlertRecord::getCourseId, courseId)
                        .isNull(courseId == null, AlertRecord::getCourseId)
                        .orderByDesc(AlertRecord::getCreateTime));
        return dedupeByStudent(result.getRecords());
    }

    /** 取指定学生集合各自最新一条有效预警(任意课程), 按学生去重 */
    private List<AlertRecord> latestAlertByStudentAny(List<Long> studentIds) {
        if (studentIds == null || studentIds.isEmpty()) return List.of();
        Page<AlertRecord> page = new Page<>(1, 500);
        Page<AlertRecord> result = alertRecordMapper.selectPage(page,
                new LambdaQueryWrapper<AlertRecord>()
                        .in(AlertRecord::getStudentId, studentIds)
                        .notIn(AlertRecord::getStatus, INVALID_RADAR_STATUSES)
                        .orderByDesc(AlertRecord::getCreateTime));
        return dedupeByStudent(result.getRecords());
    }

    private List<AlertRecord> dedupeByStudent(List<AlertRecord> records) {
        Map<Long, AlertRecord> latest = new LinkedHashMap<>();
        for (AlertRecord a : records) {
            latest.putIfAbsent(a.getStudentId(), a);
        }
        return List.copyOf(latest.values());
    }

    // ==================== 快照取数（雷达/班级均值兜底用） ====================

    /** 取单个学生最新一条快照; courseId 为空表示"任意课程", onlyComprehensive=true 时只查综合快照 */
    private AlertSnapshot latestSnapshot(Long studentId, Long courseId, boolean onlyComprehensive) {
        LambdaQueryWrapper<AlertSnapshot> wrapper = new LambdaQueryWrapper<AlertSnapshot>()
                .eq(AlertSnapshot::getStudentId, studentId)
                .orderByDesc(AlertSnapshot::getSnapshotDate)
                .orderByDesc(AlertSnapshot::getId);
        if (onlyComprehensive) {
            wrapper.isNull(AlertSnapshot::getCourseId);
        } else if (courseId != null) {
            wrapper.eq(AlertSnapshot::getCourseId, courseId);
        }
        return alertSnapshotMapper.selectOne(wrapper.last("LIMIT 1"));
    }

    /** 取指定学生集合在某一课程（或综合快照）下的最新快照，按学生去重 */
    private List<AlertSnapshot> latestSnapshotByStudent(List<Long> studentIds, Long courseId) {
        if (studentIds == null || studentIds.isEmpty()) return List.of();
        Page<AlertSnapshot> page = new Page<>(1, 500);
        Page<AlertSnapshot> result = alertSnapshotMapper.selectPage(page,
                new LambdaQueryWrapper<AlertSnapshot>()
                        .in(AlertSnapshot::getStudentId, studentIds)
                        .eq(courseId != null, AlertSnapshot::getCourseId, courseId)
                        .isNull(courseId == null, AlertSnapshot::getCourseId)
                        .orderByDesc(AlertSnapshot::getSnapshotDate)
                        .orderByDesc(AlertSnapshot::getId));
        return dedupeSnapshotsByStudent(result.getRecords());
    }

    /** 取指定学生集合各自最新快照(任意课程), 按学生去重 */
    private List<AlertSnapshot> latestSnapshotByStudentAny(List<Long> studentIds) {
        if (studentIds == null || studentIds.isEmpty()) return List.of();
        Page<AlertSnapshot> page = new Page<>(1, 500);
        Page<AlertSnapshot> result = alertSnapshotMapper.selectPage(page,
                new LambdaQueryWrapper<AlertSnapshot>()
                        .in(AlertSnapshot::getStudentId, studentIds)
                        .orderByDesc(AlertSnapshot::getSnapshotDate)
                        .orderByDesc(AlertSnapshot::getId));
        return dedupeSnapshotsByStudent(result.getRecords());
    }

    /** 已按 snapshot_date 倒序，每个学生保留第一条（最新） */
    private List<AlertSnapshot> dedupeSnapshotsByStudent(List<AlertSnapshot> records) {
        Map<Long, AlertSnapshot> latest = new LinkedHashMap<>();
        for (AlertSnapshot s : records) {
            latest.putIfAbsent(s.getStudentId(), s);
        }
        return List.copyOf(latest.values());
    }

    /**
     * 学生风险趋势：快照优先，无快照时退回预警记录（按天去重）。
     *
     * @return {points: [...], source: SNAPSHOT|ALERT_RECORD|NONE, sourceText: 中文说明}
     */
    public Map<String, Object> getStudentRiskTrend(Long studentId, Long courseId) {
        List<Map<String, Object>> points = trendFromSnapshots(studentId, courseId);
        String source = TREND_SOURCE_SNAPSHOT;
        if (points.isEmpty() && courseId != null) {
            points = trendFromSnapshots(studentId, null);
        }
        if (points.isEmpty()) {
            points = trendFromSnapshotsAny(studentId);
        }

        // 无任何快照时 fallback 到 alert_record 历史
        if (points.isEmpty()) {
            source = TREND_SOURCE_ALERT;
            points = trendFromAlertRecords(studentId, courseId);
            if (points.isEmpty() && courseId != null) {
                points = trendFromAlertRecords(studentId, null);
            }
            if (points.isEmpty()) {
                points = trendFromAlertRecordsAny(studentId);
            }
        }
        if (points.isEmpty()) {
            source = SCOPE_NONE;
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("points", points);
        result.put("source", source);
        result.put("sourceText", switch (source) {
            case TREND_SOURCE_SNAPSHOT -> "预警快照（按日）";
            case TREND_SOURCE_ALERT -> "预警记录（暂无每日快照，按天去重）";
            default -> "暂无趋势数据";
        });
        return result;
    }

    /** 快照: courseId 为空表示只查综合预警(course_id IS NULL) */
    private List<Map<String, Object>> trendFromSnapshots(Long studentId, Long courseId) {
        List<AlertSnapshot> snapshots = alertSnapshotMapper.selectList(
                new LambdaQueryWrapper<AlertSnapshot>()
                        .eq(AlertSnapshot::getStudentId, studentId)
                        .eq(courseId != null, AlertSnapshot::getCourseId, courseId)
                        .isNull(courseId == null, AlertSnapshot::getCourseId)
                        .orderByAsc(AlertSnapshot::getSnapshotDate));
        return snapshots.stream().map(s -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("date", s.getSnapshotDate().toString());
            m.put("riskScore", s.getRiskScore());
            m.put("alertLevel", s.getAlertLevel());
            m.put("predictedScore", s.getPredictedScore());
            return m;
        }).toList();
    }

    /** 快照: 任意课程 */
    private List<Map<String, Object>> trendFromSnapshotsAny(Long studentId) {
        List<AlertSnapshot> snapshots = alertSnapshotMapper.selectList(
                new LambdaQueryWrapper<AlertSnapshot>()
                        .eq(AlertSnapshot::getStudentId, studentId)
                        .orderByAsc(AlertSnapshot::getSnapshotDate));
        return snapshots.stream().map(s -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("date", s.getSnapshotDate().toString());
            m.put("riskScore", s.getRiskScore());
            m.put("alertLevel", s.getAlertLevel());
            m.put("predictedScore", s.getPredictedScore());
            return m;
        }).toList();
    }

    /** 预警历史: courseId 为空表示只查综合预警(course_id IS NULL) */
    private List<Map<String, Object>> trendFromAlertRecords(Long studentId, Long courseId) {
        return toTrend(alertRecordMapper.selectPage(new Page<>(1, 50),
                new LambdaQueryWrapper<AlertRecord>()
                        .eq(AlertRecord::getStudentId, studentId)
                        .eq(courseId != null, AlertRecord::getCourseId, courseId)
                        .isNull(courseId == null, AlertRecord::getCourseId)
                        .orderByDesc(AlertRecord::getCreateTime)).getRecords());
    }

    /** 预警历史: 任意课程 */
    private List<Map<String, Object>> trendFromAlertRecordsAny(Long studentId) {
        return toTrend(alertRecordMapper.selectPage(new Page<>(1, 50),
                new LambdaQueryWrapper<AlertRecord>()
                        .eq(AlertRecord::getStudentId, studentId)
                        .orderByDesc(AlertRecord::getCreateTime)).getRecords());
    }

    /** 将预警记录按天去重后转趋势点 */
    private List<Map<String, Object>> toTrend(List<AlertRecord> records) {
        Map<LocalDate, AlertRecord> byDate = new LinkedHashMap<>();
        for (AlertRecord a : records) {
            if (a.getCreateTime() == null || a.getRiskScore() == null) continue;
            LocalDate d = a.getCreateTime().toLocalDate();
            byDate.putIfAbsent(d, a);
        }
        return byDate.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(e -> {
                    AlertRecord a = e.getValue();
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("date", e.getKey().toString());
                    m.put("riskScore", a.getRiskScore());
                    m.put("alertLevel", a.getAlertLevel());
                    m.put("predictedScore", a.getPredictedScore());
                    return m;
                }).toList();
    }

    /** 班级风险热力图 */
    public Map<String, Object> getClassHeatmap(Long teacherId) {
        List<AlertRecordVO> alerts = getAlertsByTeacherId(teacherId, null, null);

        // 批量加载学生信息以消除 N+1
        Set<Long> allStudentIds = alerts.stream().map(AlertRecordVO::getStudentId).collect(Collectors.toSet());
        Map<Long, Student> studentMap = new HashMap<>();
        if (!allStudentIds.isEmpty()) {
            List<Student> allStudents = studentMapper.selectList(
                    new LambdaQueryWrapper<Student>().in(Student::getId, allStudentIds));
            for (Student s : allStudents) {
                studentMap.put(s.getId(), s);
            }
        }

        // 分组: className → level → count
        Map<String, Map<String, Long>> heatmap = new LinkedHashMap<>();
        for (AlertRecordVO a : alerts) {
            Student student = studentMap.get(a.getStudentId());
            String className = student != null ? student.getClassName() : "未知";
            heatmap.computeIfAbsent(className, k -> new LinkedHashMap<>())
                    .merge(a.getAlertLevel(), 1L, Long::sum);
        }
        Map<String, Object> result = new LinkedHashMap<>();
        heatmap.forEach((cls, levels) -> result.put(cls, levels));
        return result;
    }

    // ==================== 私有辅助 ====================

    private void logOperation(Long alertId, Long operatorId, String operatorType,
                              String operation, String from, String to, String remark) {
        AlertOperationLog log = new AlertOperationLog();
        log.setAlertId(alertId);
        log.setOperatorId(operatorId);
        log.setOperatorType(operatorType);
        log.setOperation(operation);
        log.setFromStatus(from);
        log.setToStatus(to);
        log.setRemark(remark);
        log.setCreateTime(LocalDateTime.now());
        alertOperationLogMapper.insert(log);
    }

    /** 从操作日志或干预记录中找处理该预警的教师 */
    private Long findTeacherForAlert(Long alertId) {
        // 优先从干预记录找
        var interventions = interventionRecordMapper.selectList(
                new LambdaQueryWrapper<InterventionRecord>()
                        .eq(InterventionRecord::getAlertId, alertId)
                        .last("LIMIT 1"));
        if (!interventions.isEmpty()) return interventions.get(0).getTeacherId();
        // 从操作日志中找HANDLE操作的教师
        var logs = alertOperationLogMapper.selectList(
                new LambdaQueryWrapper<AlertOperationLog>()
                        .eq(AlertOperationLog::getAlertId, alertId)
                        .eq(AlertOperationLog::getOperation, "HANDLE")
                        .last("LIMIT 1"));
        return logs.isEmpty() ? null : logs.get(0).getOperatorId();
    }

    private record StudentIdRange(Long start, Long end) {}

    private boolean isStudentInAnyRange(Long studentId, List<StudentIdRange> ranges) {
        return ranges.stream().anyMatch(r -> studentId >= r.start() && studentId <= r.end());
    }

    private List<AlertRecordVO> convertToVOList(List<AlertRecord> alerts, List<Student> students) {
        if (CollectionUtils.isEmpty(alerts)) return Collections.emptyList();
        Map<Long, Student> studentMap = new HashMap<>();
        for (Student s : students) studentMap.put(s.getId(), s);

        return alerts.stream().map(alert -> {
            AlertRecordVO vo = new AlertRecordVO();
            BeanUtils.copyProperties(alert, vo);
            Student s = studentMap.get(alert.getStudentId());
            if (s != null) {
                vo.setStudentName(s.getStudentName());
                vo.setStudentNo(s.getStudentNo());
            }
            vo.setAlertLevelName(getLevelName(alert.getAlertLevel()));
            vo.setAlertTypeName(getTypeName(alert.getAlertType()));
            vo.setStatusName(getStatusName(alert.getStatus()));
            return vo;
        }).toList();
    }

    private String getLevelName(String l) {
        return switch (l) {
            case "RED" -> "红色（高风险）";
            case "ORANGE" -> "橙色（中风险）";
            case "YELLOW" -> "黄色（低风险）";
            case "GREEN" -> "绿色（正常）";
            default -> l == null ? "未知" : l;
        };
    }

    private String getTypeName(String t) {
        return switch (t) {
            case "FAILURE" -> "挂科风险";
            case "HOMEWORK" -> "作业欠交";
            case "KNOWLEDGE" -> "知识点断层";
            case "ABSENTEEISM" -> "学习倦怠";
            case "DROP" -> "成绩骤降";
            case "CUMULATIVE" -> "累积风险";
            default -> t == null ? "未知" : t;
        };
    }

    private String getStatusName(String s) {
        return switch (s) {
            case "ACTIVE" -> "待处理";
            case "ACKNOWLEDGED" -> "已确认";
            case "HANDLED" -> "已处理";
            case "DISMISSED" -> "已撤销";
            case "CLOSED" -> "已闭环";
            default -> s == null ? "未知" : s;
        };
    }

    private String getInterventionTypeName(String t) {
        return switch (t) {
            case "TALK" -> "约谈";
            case "TUTOR" -> "辅导";
            case "PARENT" -> "联系家长";
            case "SUPPLEMENT" -> "补课补习";
            case "PLAN" -> "学习计划";
            default -> "其他";
        };
    }

    private String getOperationName(String op) {
        return switch (op) {
            case "GENERATE" -> "系统生成";
            case "ACKNOWLEDGE" -> "学生确认";
            case "HANDLE" -> "教师处理";
            case "DISMISS" -> "教师撤销";
            case "CLOSE" -> "闭环完成";
            case "REOPEN" -> "重新打开";
            case "ARCHIVE" -> "系统归档";
            default -> op;
        };
    }
}
