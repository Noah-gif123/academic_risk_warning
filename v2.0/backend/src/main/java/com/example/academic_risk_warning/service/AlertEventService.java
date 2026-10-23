package com.example.academic_risk_warning.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.example.academic_risk_warning.common.BizException;
import com.example.academic_risk_warning.entity.AlertFlowLog;
import com.example.academic_risk_warning.entity.AlertRecord;
import com.example.academic_risk_warning.entity.Counselor;
import com.example.academic_risk_warning.entity.CounselorClass;
import com.example.academic_risk_warning.entity.Course;
import com.example.academic_risk_warning.entity.CourseOffering;
import com.example.academic_risk_warning.entity.Enrollment;
import com.example.academic_risk_warning.entity.InterventionRecord;
import com.example.academic_risk_warning.entity.Notification;
import com.example.academic_risk_warning.entity.NotificationReceipt;
import com.example.academic_risk_warning.entity.RiskState;
import com.example.academic_risk_warning.entity.Student;
import com.example.academic_risk_warning.entity.Teacher;
import com.example.academic_risk_warning.entity.TeachingClass;
import com.example.academic_risk_warning.entity.TeachingClassAdminClass;
import com.example.academic_risk_warning.mapper.AlertFlowLogMapper;
import com.example.academic_risk_warning.mapper.AlertRecordMapper;
import com.example.academic_risk_warning.mapper.CounselorClassMapper;
import com.example.academic_risk_warning.mapper.CounselorMapper;
import com.example.academic_risk_warning.mapper.CourseMapper;
import com.example.academic_risk_warning.mapper.CourseOfferingMapper;
import com.example.academic_risk_warning.mapper.EnrollmentMapper;
import com.example.academic_risk_warning.mapper.InterventionRecordMapper;
import com.example.academic_risk_warning.mapper.NotificationMapper;
import com.example.academic_risk_warning.mapper.NotificationReceiptMapper;
import com.example.academic_risk_warning.mapper.RiskStateMapper;
import com.example.academic_risk_warning.mapper.StudentMapper;
import com.example.academic_risk_warning.mapper.TeacherMapper;
import com.example.academic_risk_warning.mapper.TeachingClassAdminClassMapper;
import com.example.academic_risk_warning.mapper.TeachingClassMapper;
import com.example.academic_risk_warning.security.LoginUser;
import com.example.academic_risk_warning.vo.AlertRowVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 预警事件层（第二阶段第 2 步）。
 *
 * <p>判定完成后调用 {@link #sync(RiskState, RiskState)}：对比判定前后等级，</p>
 * <ul>
 *   <li>由绿转黄/橙/红 → <b>生成</b>预警（一人一课只保留一条当前预警）；</li>
 *   <li>等级升高 → <b>升级</b>并记录升级前等级；降低 → 记录降级；</li>
 *   <li>回到绿色/风险消失 → <b>自动关闭</b>；</li>
 * </ul>
 * <p>每次变化写 alert_flow_log；生成与升级按聚合键发通知（教师与辅导员一条汇总，学生本人一条）。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AlertEventService {

    private static final String[] LEVELS = { "GREEN", "YELLOW", "ORANGE", "RED" };
    private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("yyyyMMdd");
    /** 人工关闭/忽略后，同等级风险再次判定时的静默天数（避免重复判定刷出重复预警） */
    private static final int REPEAT_ALERT_COOLDOWN_DAYS = 7;

    private final AlertRecordMapper alertMapper;
    private final AlertFlowLogMapper flowMapper;
    private final RiskStateMapper riskStateMapper;
    private final EnrollmentMapper enrollmentMapper;
    private final StudentMapper studentMapper;
    private final TeacherMapper teacherMapper;
    private final CounselorMapper counselorMapper;
    private final CounselorClassMapper counselorClassMapper;
    private final TeachingClassMapper classMapper;
    private final TeachingClassAdminClassMapper tcacMapper;
    private final CourseOfferingMapper offeringMapper;
    private final CourseMapper courseMapper;
    private final NotificationMapper notificationMapper;
    private final NotificationReceiptMapper receiptMapper;
    private final InterventionRecordMapper interventionMapper;
    private final AuditService auditService;

    // ============================================================ 判定后同步预警事件
    /**
     * 判定前后状态对比，决定生成/升级/降级/关闭。
     *
     * @param before 判定前的风险状态（首次判定为 null）
     * @param after  判定后已落库的风险状态
     */
    @Transactional
    public void sync(RiskState before, RiskState after) {
        if (after == null || after.getEnrollmentId() == null) {
            return;
        }
        String oldLevel = before == null ? null : before.getRiskLevel();
        String newLevel = after.getRiskLevel() == null ? "GREEN" : after.getRiskLevel();
        AlertRecord current = currentAlert(after.getEnrollmentId());
        boolean risky = rank(newLevel) >= rank("YELLOW");

        if (!risky) {
            if (current != null) {
                closeInternal(current, "风险已消除（等级回到绿色），系统自动关闭", null, "SYSTEM");
            }
            return;
        }
        if (current == null) {
            // 人工关闭/忽略后同等级仍在冷却期内，不重复生成，避免重复判定刷出一堆重复预警
            AlertRecord recent = recentManualClose(after.getEnrollmentId(), newLevel);
            if (recent != null) {
                log.info("跳过重复告警：enrollment={} level={} 最近一次人工关闭于 {}",
                        after.getEnrollmentId(), newLevel, recent.getCloseTime());
                return;
            }
            createAlert(after, newLevel);
            return;
        }
        int cmp = rank(newLevel) - rank(current.getAlertLevel());
        if (cmp > 0) {
            upgradeAlert(current, after, newLevel);
        } else if (cmp < 0) {
            downgradeAlert(current, after, newLevel);
        } else {
            // 同等级：只刷新分数与证据，不产生事件，避免时间线被刷屏
            current.setRiskScore(after.getRiskScore());
            current.setConfidence(after.getConfidence());
            current.setAlertReason(reasonOf(after));
            current.setEvidenceJson(evidenceOf(after));
            current.setUpdateTime(LocalDateTime.now());
            alertMapper.updateById(current);
        }
    }

    // ============================================================ 生成 / 升级 / 降级 / 关闭
    /** 同等级、人工关闭/忽略且仍在冷却期内的最近一条预警（无则返回 null） */
    private AlertRecord recentManualClose(Long enrollmentId, String level) {
        AlertRecord recent = alertMapper.selectOne(Wrappers.<AlertRecord>lambdaQuery()
                .eq(AlertRecord::getEnrollmentId, enrollmentId)
                .eq(AlertRecord::getIsVoid, 0)
                .in(AlertRecord::getStatus, "CLOSED", "DISMISSED")
                .eq(AlertRecord::getAlertLevel, level)
                .isNotNull(AlertRecord::getCloseBy)
                .isNotNull(AlertRecord::getCloseTime)
                .orderByDesc(AlertRecord::getCloseTime)
                .last("limit 1"));
        if (recent == null || recent.getCloseTime() == null) {
            return null;
        }
        return recent.getCloseTime().isAfter(LocalDateTime.now().minusDays(REPEAT_ALERT_COOLDOWN_DAYS))
                ? recent : null;
    }

    private void createAlert(RiskState s, String level) {
        LocalDateTime now = LocalDateTime.now();
        AlertRecord a = new AlertRecord();
        a.setStudentId(s.getStudentId());
        a.setEnrollmentId(s.getEnrollmentId());
        a.setOfferingId(s.getOfferingId());
        a.setTeachingClassId(s.getTeachingClassId());
        a.setCourseId(courseIdOf(s.getOfferingId()));
        a.setTermId(s.getTermId());
        a.setAlertLevel(level);
        a.setTriggerChannel(s.getActiveChannel() == null ? "B" : s.getActiveChannel());
        a.setTriggerRule(ruleOf(s));
        a.setRiskScore(s.getRiskScore());
        a.setConfidence(s.getConfidence());
        a.setOutputMode(s.getOutputMode());
        a.setAlertReason(reasonOf(s));
        a.setEvidenceJson(evidenceOf(s));
        a.setChannelSnapshotJson("{\"a\":\"" + nvl(s.getChannelALevel()) + "\",\"b\":" + s.getChannelBScore()
                + ",\"c\":null}");
        a.setStatus("ACTIVE");
        a.setIsCurrent(1);
        a.setEscalated(0);
        a.setNotified(1);
        a.setFirstNotifyTime(now);
        a.setExpireTime(now.plusDays(5));
        a.setEngineVersion(s.getEngineVersion());
        a.setIsVoid(0);
        a.setCreateTime(now);
        a.setUpdateTime(now);
        alertMapper.insert(a);

        writeFlow(a, s.getStudentId(), s.getTermId(), "CREATED", null, level, null, "ACTIVE",
                "生成预警：" + a.getAlertReason(), null, "SYSTEM");
        notifyTeacherSide(a, s, "ALERT_NEW", "新预警");
        notifyStudent(a, s, "ALERT_NEW", "新预警");
        writeFlow(a, s.getStudentId(), s.getTermId(), "NOTIFIED", level, level, "ACTIVE", "ACTIVE",
                "已通知任课教师、辅导员与学生", null, "SYSTEM");
        auditService.record("ALERT", a.getId(), "CREATE", null, jsonLevel(null, a.getAlertLevel()),
                "生成预警：" + a.getAlertReason());
    }

    private void upgradeAlert(AlertRecord a, RiskState s, String level) {
        LocalDateTime now = LocalDateTime.now();
        String from = a.getAlertLevel();
        a.setPreviousLevel(a.getPreviousLevel() == null ? from : a.getPreviousLevel());
        a.setAlertLevel(level);
        a.setEscalated(1);
        a.setEscalatedAt(now);
        a.setTriggerChannel(s.getActiveChannel() == null ? a.getTriggerChannel() : s.getActiveChannel());
        a.setTriggerRule(ruleOf(s));
        a.setRiskScore(s.getRiskScore());
        a.setConfidence(s.getConfidence());
        a.setOutputMode(s.getOutputMode());
        a.setAlertReason(reasonOf(s));
        a.setEvidenceJson(evidenceOf(s));
        a.setNotified(1);
        a.setExpireTime(now.plusDays(5));
        a.setEngineVersion(s.getEngineVersion());
        a.setUpdateTime(now);
        alertMapper.updateById(a);

        writeFlow(a, s.getStudentId(), s.getTermId(), "LEVEL_UP", from, level, a.getStatus(), a.getStatus(),
                "预警升级：" + from + " → " + level + "（" + a.getAlertReason() + "）", null, "SYSTEM");
        notifyTeacherSide(a, s, "ALERT_UPGRADE", "预警升级");
        notifyStudent(a, s, "ALERT_UPGRADE", "预警升级");
        auditService.record("ALERT", a.getId(), "LEVEL_UP", jsonLevel(from, null), jsonLevel(null, level), "预警升级：" + from + " → " + level);
    }

    private void downgradeAlert(AlertRecord a, RiskState s, String level) {
        String from = a.getAlertLevel();
        a.setAlertLevel(level);
        a.setRiskScore(s.getRiskScore());
        a.setConfidence(s.getConfidence());
        a.setAlertReason(reasonOf(s));
        a.setEvidenceJson(evidenceOf(s));
        a.setUpdateTime(LocalDateTime.now());
        alertMapper.updateById(a);
        writeFlow(a, s.getStudentId(), s.getTermId(), "LEVEL_DOWN", from, level, a.getStatus(), a.getStatus(),
                "预警降级：" + from + " → " + level, null, "SYSTEM");
    }

    private void closeInternal(AlertRecord a, String note, Long operatorId, String role) {
        LocalDateTime now = LocalDateTime.now();
        a.setStatus("CLOSED");
        a.setIsCurrent(null);
        a.setCloseTime(now);
        a.setCloseNote(note);
        a.setUpdateTime(now);
        alertMapper.updateById(a);
        // is_current 是唯一键的一部分，MP 默认忽略 null 字段，这里显式写 NULL 释放唯一位
        alertMapper.update(null, Wrappers.<AlertRecord>lambdaUpdate()
                .set(AlertRecord::getIsCurrent, null).eq(AlertRecord::getId, a.getId()));
        writeFlow(a, a.getStudentId(), a.getTermId(), "CLOSED", a.getAlertLevel(), a.getAlertLevel(),
                "ACTIVE", "CLOSED", note, operatorId, role);
        if (operatorId != null) {
            auditService.record("ALERT", a.getId(), "CLOSE", jsonLevel(a.getAlertLevel(), null),
                    "{\"status\":\"CLOSED\"}", "关闭预警：" + note);
        }
    }

    // ============================================================ 教师处置
    /** 认领：ACTIVE → HANDLING */
    @Transactional
    public AlertRowVO claim(LoginUser user, Long alertId) {
        AlertRecord a = requireMyAlert(user, alertId);
        if (!"ACTIVE".equals(a.getStatus())) {
            throw new BizException("该预警当前状态为 " + a.getStatus() + "，不能认领");
        }
        LocalDateTime now = LocalDateTime.now();
        a.setStatus("HANDLING");
        a.setHandleBy(user.getRefId());
        a.setHandleRole("TEACHER");
        a.setHandleTime(now);
        a.setUpdateTime(now);
        alertMapper.updateById(a);
        writeFlow(a, a.getStudentId(), a.getTermId(), "ACKNOWLEDGED", a.getAlertLevel(), a.getAlertLevel(),
                "ACTIVE", "HANDLING", "教师认领并开始处理", user.getRefId(), "TEACHER");
        return enrich(toRow(a));
    }

    /** 处理并记录：写 intervention_record（stage=TEACHER），预警保持 HANDLING */
    @Transactional
    public AlertRowVO handle(LoginUser user, Long alertId, String actionType, String content, String result) {
        AlertRecord a = requireMyAlert(user, alertId);
        if ("CLOSED".equals(a.getStatus()) || "DISMISSED".equals(a.getStatus())) {
            throw new BizException("预警已结束，不能再记录处理");
        }
        LocalDateTime now = LocalDateTime.now();
        InterventionRecord r = new InterventionRecord();
        r.setStudentId(a.getStudentId());
        r.setTermId(a.getTermId());
        r.setAlertId(a.getId());
        r.setStage("TEACHER");
        r.setOperatorId(user.getRefId());
        r.setOperatorRole("TEACHER");
        r.setActionType(actionType == null || actionType.isEmpty() ? "TALK" : actionType);
        r.setContent(content);
        r.setResult(result);
        r.setEffective("PENDING");
        r.setEffectCheckTime(now.plusDays(14));
        r.setRiskScoreBefore(a.getRiskScore());
        r.setReferStatus("NONE");
        r.setIsVoid(0);
        r.setCreateBy(user.getRefId());
        r.setCreateTime(now);
        r.setUpdateBy(user.getRefId());
        r.setUpdateTime(now);
        interventionMapper.insert(r);

        if ("ACTIVE".equals(a.getStatus())) {
            a.setStatus("HANDLING");
            a.setHandleBy(user.getRefId());
            a.setHandleRole("TEACHER");
            a.setHandleTime(now);
        }
        a.setHandleNote(trim((a.getHandleNote() == null ? "" : a.getHandleNote() + "；") + r.getActionType()));
        a.setUpdateTime(now);
        alertMapper.updateById(a);
        writeFlow(a, a.getStudentId(), a.getTermId(), "HANDLED", a.getAlertLevel(), a.getAlertLevel(),
                "ACTIVE", a.getStatus(), "记录处理：" + r.getActionType() + "（" + nvl(content) + "）",
                user.getRefId(), "TEACHER");
        auditService.record("ALERT", a.getId(), "HANDLE", null, null, "教师记录处理：" + r.getActionType());
        return enrich(toRow(a));
    }

    /** 关闭：填写结论，释放"当前预警"唯一位 */
    @Transactional
    public AlertRowVO close(LoginUser user, Long alertId, String note) {
        AlertRecord a = requireMyAlert(user, alertId);
        if ("CLOSED".equals(a.getStatus())) {
            throw new BizException("该预警已关闭");
        }
        LocalDateTime now = LocalDateTime.now();
        a.setStatus("CLOSED");
        a.setIsCurrent(null);
        a.setCloseBy(user.getRefId());
        a.setCloseTime(now);
        a.setCloseNote(note == null || note.isEmpty() ? "教师关闭" : note);
        a.setUpdateTime(now);
        alertMapper.updateById(a);
        // is_current 是唯一键的一部分，MP 默认忽略 null 字段，这里显式写 NULL 释放唯一位
        alertMapper.update(null, Wrappers.<AlertRecord>lambdaUpdate()
                .set(AlertRecord::getIsCurrent, null).eq(AlertRecord::getId, a.getId()));
        writeFlow(a, a.getStudentId(), a.getTermId(), "CLOSED", a.getAlertLevel(), a.getAlertLevel(),
                "ACTIVE", "CLOSED", a.getCloseNote(), user.getRefId(), "TEACHER");
        auditService.record("ALERT", a.getId(), "CLOSE", jsonLevel(a.getAlertLevel(), null), "{\"status\":\"CLOSED\"}", "关闭预警：" + a.getCloseNote());
        return enrich(toRow(a));
    }

    /** 忽略：必须填原因 */
    @Transactional
    public AlertRowVO dismiss(LoginUser user, Long alertId, String reason) {
        if (reason == null || reason.trim().isEmpty()) {
            throw new BizException("忽略预警必须填写原因");
        }
        AlertRecord a = requireMyAlert(user, alertId);
        LocalDateTime now = LocalDateTime.now();
        a.setStatus("DISMISSED");
        a.setIsCurrent(null);
        a.setDismissReason(reason);
        a.setCloseBy(user.getRefId());
        a.setCloseTime(now);
        a.setUpdateTime(now);
        alertMapper.updateById(a);
        // is_current 是唯一键的一部分，MP 默认忽略 null 字段，这里显式写 NULL 释放唯一位
        alertMapper.update(null, Wrappers.<AlertRecord>lambdaUpdate()
                .set(AlertRecord::getIsCurrent, null).eq(AlertRecord::getId, a.getId()));
        writeFlow(a, a.getStudentId(), a.getTermId(), "DISMISSED", a.getAlertLevel(), a.getAlertLevel(),
                "ACTIVE", "DISMISSED", "忽略预警：" + reason, user.getRefId(), "TEACHER");
        auditService.record("ALERT", a.getId(), "DISMISS", jsonLevel(a.getAlertLevel(), null), "{\"status\":\"DISMISSED\"}", "忽略预警：" + reason);
        return enrich(toRow(a));
    }

    /** 提醒学生：给本人发一条站内消息（同一预警同一天只发一条） */
    @Transactional
    public int remindStudent(LoginUser user, Long alertId, String note) {
        AlertRecord a = requireMyAlert(user, alertId);
        if ("CLOSED".equals(a.getStatus()) || "DISMISSED".equals(a.getStatus())) {
            throw new BizException("预警已结束，不能再提醒学生");
        }
        RiskState s = riskStateMapper.selectOne(Wrappers.<RiskState>lambdaQuery()
                .eq(RiskState::getEnrollmentId, a.getEnrollmentId()).last("limit 1"));
        RiskState snapshot = s == null ? new RiskState() : s;
        if (s == null) {
            snapshot.setEnrollmentId(a.getEnrollmentId());
            snapshot.setStudentId(a.getStudentId());
            snapshot.setTermId(a.getTermId());
            snapshot.setRiskLevel(a.getAlertLevel());
            snapshot.setRiskScore(a.getRiskScore());
            snapshot.setAbsentCount(0);
            snapshot.setAllowedAbsent(0);
            snapshot.setRemainingAbsent(0);
            snapshot.setChannelALevel(null);
        }
        notifyStudent(a, snapshot, "ALERT_REMIND",
                note == null || note.isEmpty() ? "教师提醒" : note);
        writeFlow(a, a.getStudentId(), a.getTermId(), "NOTIFIED", a.getAlertLevel(), a.getAlertLevel(),
                a.getStatus(), a.getStatus(), "教师提醒学生：" + nvl(note), user.getRefId(), "TEACHER");
        return 1;
    }

    // ============================================================ 查询
    /** 教师端预警列表（仅本人教学班），按风险分倒序 */
    public List<AlertRowVO> listForTeacher(LoginUser user, Long offeringId, Long teachingClassId,
                                           String level, String status) {
        if (user == null) {
            throw new BizException("请先登录");
        }
        List<Long> classIds = myClassIds(user);
        if (classIds.isEmpty()) {
            return List.of();
        }
        if (teachingClassId != null) {
            if (!classIds.contains(teachingClassId)) {
                throw new BizException("只能查看自己教学班的预警");
            }
            classIds = List.of(teachingClassId);
        }
        List<AlertRecord> alerts = alertMapper.selectList(Wrappers.<AlertRecord>lambdaQuery()
                .in(AlertRecord::getTeachingClassId, classIds)
                .eq(offeringId != null, AlertRecord::getOfferingId, offeringId)
                .eq(level != null && !level.isEmpty(), AlertRecord::getAlertLevel, level)
                .eq(status != null && !status.isEmpty(), AlertRecord::getStatus, status)
                .eq(AlertRecord::getIsVoid, 0)
                .orderByDesc(AlertRecord::getRiskScore)
                .orderByDesc(AlertRecord::getId));
        return toRows(alerts);
    }

    /** 教师端总览计数（预警侧 + 判定侧元信息） */
    public Map<String, Object> stats(LoginUser user, Long teachingClassId) {
        List<AlertRowVO> rows = listForTeacher(user, null, teachingClassId, null, null);
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("total", rows.size());
        map.put("red", rows.stream().filter((r) -> "RED".equals(r.getAlertLevel())).count());
        map.put("orange", rows.stream().filter((r) -> "ORANGE".equals(r.getAlertLevel())).count());
        map.put("yellow", rows.stream().filter((r) -> "YELLOW".equals(r.getAlertLevel())).count());
        map.put("active", rows.stream().filter((r) -> "ACTIVE".equals(r.getStatus())).count());
        map.put("handling", rows.stream().filter((r) -> "HANDLING".equals(r.getStatus())).count());
        map.put("closed", rows.stream().filter((r) -> "CLOSED".equals(r.getStatus())).count());
        map.put("escalated", rows.stream().filter((r) -> r.getEscalated() != null && r.getEscalated() == 1).count());
        LocalDate today = LocalDate.now();
        map.put("todayNew", rows.stream()
                .filter((r) -> r.getCreateTime() != null && today.equals(r.getCreateTime().toLocalDate()))
                .count());
        map.put("pending", rows.stream().filter((r) -> "ACTIVE".equals(r.getStatus())
                || "HANDLING".equals(r.getStatus()) || "ESCALATED".equals(r.getStatus())).count());

        // 判定侧：最近判定时间 + 引擎版本 + 已判定学生数
        List<Long> classIds = myClassIds(user);
        if (teachingClassId != null) {
            if (!classIds.contains(teachingClassId)) {
                throw new BizException("只能查看自己教学班的预警");
            }
            classIds = List.of(teachingClassId);
        }
        map.put("evaluated", 0L);
        map.put("lastEvalTime", null);
        map.put("engineVersion", null);
        if (!classIds.isEmpty()) {
            map.put("evaluated", riskStateMapper.selectCount(Wrappers.<RiskState>lambdaQuery()
                    .in(RiskState::getTeachingClassId, classIds)
                    .eq(RiskState::getIsVoid, 0)));
            RiskState latest = riskStateMapper.selectOne(Wrappers.<RiskState>lambdaQuery()
                    .in(RiskState::getTeachingClassId, classIds)
                    .eq(RiskState::getIsVoid, 0)
                    .isNotNull(RiskState::getLastEvalTime)
                    .orderByDesc(RiskState::getLastEvalTime)
                    .last("limit 1"));
            if (latest != null) {
                map.put("lastEvalTime", latest.getLastEvalTime());
                map.put("engineVersion", latest.getEngineVersion());
            }
        }
        return map;
    }

    /** 教师端导出（xlsx）：一行一条预警，字段与列表页一致 */
    public byte[] exportForTeacher(LoginUser user, Long teachingClassId, String level, String status) {
        List<AlertRowVO> rows = listForTeacher(user, null, teachingClassId, level, status);
        String[] heads = { "学号", "姓名", "教学班", "课程", "预警等级", "触发通道", "触发规则", "风险分",
                "置信度", "输出模式", "缺课次数", "允许缺课", "剩余额度", "硬性不合格", "期末需考",
                "状态", "处置人", "处置说明", "判定依据", "生成时间" };
        try (XSSFWorkbook wb = new XSSFWorkbook(); ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
            Sheet sheet = wb.createSheet("预警名单");
            CellStyle headStyle = wb.createCellStyle();
            Font font = wb.createFont();
            font.setBold(true);
            headStyle.setFont(font);
            headStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
            headStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            Row head = sheet.createRow(0);
            for (int i = 0; i < heads.length; i++) {
                head.createCell(i).setCellValue(heads[i]);
                head.getCell(i).setCellStyle(headStyle);
                sheet.setColumnWidth(i, (i == 18 ? 40 : 14) * 256);
            }
            int r = 1;
            for (AlertRowVO v : rows) {
                Row row = sheet.createRow(r++);
                int c = 0;
                row.createCell(c++).setCellValue(nvl(v.getStudentNo()));
                row.createCell(c++).setCellValue(nvl(v.getStudentName()));
                row.createCell(c++).setCellValue(nvl(v.getClassCode()));
                row.createCell(c++).setCellValue(nvl(v.getCourseName()));
                row.createCell(c++).setCellValue(levelText(v.getAlertLevel()));
                row.createCell(c++).setCellValue(nvl(v.getTriggerChannel()));
                row.createCell(c++).setCellValue(nvl(v.getTriggerRule()));
                row.createCell(c++).setCellValue(v.getRiskScore() == null ? "" : v.getRiskScore().toPlainString());
                row.createCell(c++).setCellValue(v.getConfidence() == null ? "" : v.getConfidence().toPlainString());
                row.createCell(c++).setCellValue(nvl(v.getOutputMode()));
                row.createCell(c++).setCellValue(nz(v.getAbsentCount()));
                row.createCell(c++).setCellValue(nz(v.getAllowedAbsent()));
                row.createCell(c++).setCellValue(nz(v.getRemainingAbsent()));
                row.createCell(c++).setCellValue(nz(v.getHardFail()) == 1 ? "是" : "否");
                row.createCell(c++).setCellValue(v.getFinalExamNeeded() == null ? ""
                        : (nz(v.getFinalUnreachable()) == 1 ? "不可达" : v.getFinalExamNeeded().toPlainString()));
                row.createCell(c++).setCellValue(statusText(v.getStatus()));
                row.createCell(c++).setCellValue(nvl(v.getHandleRole()));
                row.createCell(c++).setCellValue(nvl(v.getHandleNote()));
                row.createCell(c++).setCellValue(nvl(v.getAlertReason()));
                row.createCell(c).setCellValue(v.getCreateTime() == null ? ""
                        : v.getCreateTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")));
            }
            wb.write(bos);
            return bos.toByteArray();
        } catch (IOException e) {
            throw new BizException("导出失败：" + e.getMessage());
        }
    }

    private static String levelText(String level) {
        if (level == null) {
            return "";
        }
        switch (level) {
            case "RED":
                return "红色";
            case "ORANGE":
                return "橙色";
            case "YELLOW":
                return "黄色";
            case "GREEN":
                return "绿色";
            default:
                return level;
        }
    }

    private static String statusText(String status) {
        if (status == null) {
            return "";
        }
        switch (status) {
            case "ACTIVE":
                return "待处理";
            case "HANDLING":
                return "处理中";
            case "ESCALATED":
                return "已升级";
            case "CLOSED":
                return "已关闭";
            case "DISMISSED":
                return "已忽略";
            default:
                return status;
        }
    }

    /** 当前用户（教师）名下教学班 id */
    private List<Long> myClassIds(LoginUser user) {
        if (user == null) {
            throw new BizException("请先登录");
        }
        return classMapper.selectList(Wrappers.<TeachingClass>lambdaQuery()
                        .eq(user.getRefId() != null, TeachingClass::getTeacherId, user.getRefId()))
                .stream().map(TeachingClass::getId).collect(Collectors.toList());
    }

    /** 学生申诉：写回 student_response，通知任课教师与辅导员，并留痕（第 4 步） */
    @Transactional
    public AlertRowVO studentAppeal(LoginUser user, Long alertId, String reason) {
        if (user == null || user.getRefId() == null) {
            throw new BizException("当前账号没有学生档案");
        }
        AlertRecord a = alertMapper.selectById(alertId);
        if (a == null || (a.getIsVoid() != null && a.getIsVoid() == 1)) {
            throw new BizException("预警不存在");
        }
        if (!user.getRefId().equals(a.getStudentId())) {
            throw new BizException("只能申诉自己的预警");
        }
        if ("CLOSED".equals(a.getStatus()) || "DISMISSED".equals(a.getStatus())) {
            throw new BizException("该预警已结束，无需申诉；如仍有疑问请联系任课教师");
        }
        if (a.getStudentResponse() != null && !a.getStudentResponse().isEmpty()) {
            throw new BizException("已提交过申诉，请等待任课教师核实");
        }
        if (reason == null || reason.trim().isEmpty()) {
            throw new BizException("请填写申诉说明");
        }
        LocalDateTime now = LocalDateTime.now();
        a.setStudentResponse(trim(reason));
        a.setStudentResponseTime(now);
        a.setUpdateTime(now);
        alertMapper.updateById(a);
        writeFlow(a, a.getStudentId(), a.getTermId(), "APPEAL", a.getAlertLevel(), a.getAlertLevel(),
                a.getStatus(), a.getStatus(), "学生提交申诉：" + trim(reason), user.getRefId(), "STUDENT");
        notifyAppeal(a, reason);
        auditService.record("ALERT", a.getId(), "APPEAL", null, "{\"status\":\"APPEALED\"}",
                "学生提交申诉");
        return enrich(toRow(a));
    }

    /** 申诉通知：任课教师 + 辅导员（同一预警只聚合成一条） */
    private void notifyAppeal(AlertRecord a, String reason) {
        TeachingClass tc = classMapper.selectById(a.getTeachingClassId());
        if (tc == null) {
            return;
        }
        Teacher teacher = teacherMapper.selectById(tc.getTeacherId());
        Set<Long> accounts = new LinkedHashSet<>();
        Map<Long, String> roleOf = new LinkedHashMap<>();
        if (teacher != null && teacher.getAccountId() != null) {
            accounts.add(teacher.getAccountId());
            roleOf.put(teacher.getAccountId(), "TEACHER");
        }
        for (Counselor c : counselorsOfClass(tc.getId(), a.getTermId())) {
            if (c.getAccountId() != null) {
                accounts.add(c.getAccountId());
                roleOf.putIfAbsent(c.getAccountId(), "COUNSELOR");
            }
        }
        if (accounts.isEmpty()) {
            return;
        }
        Course course = a.getCourseId() == null ? null : courseMapper.selectById(a.getCourseId());
        Student st = studentMapper.selectById(a.getStudentId());
        String studentText = st == null ? "有学生" : (st.getStudentNo() + " " + st.getStudentName());
        LocalDateTime now = LocalDateTime.now();
        String key = "APPEAL:" + a.getTeachingClassId() + ":" + a.getId();
        Notification n = notificationMapper.selectOne(Wrappers.<Notification>lambdaQuery()
                .eq(Notification::getAggregateKey, key).last("limit 1"));
        if (n == null) {
            n = new Notification();
            n.setBizType("ALERT_APPEAL");
            n.setBizId(a.getId());
            n.setTitle("学生申诉：" + tc.getClassCode() + "（" + nvl(course == null ? "" : course.getCourseName())
                    + "）" + a.getAlertLevel());
            n.setContent(studentText + " 对" + a.getAlertLevel() + "预警结论提出申诉：" + trim(reason)
                    + "。请在「预警监控」中核实并记录处理。");
            n.setPayloadJson("{\"route\":\"/teacher/monitor\",\"alertId\":" + a.getId()
                    + ",\"teachingClassId\":" + a.getTeachingClassId() + "}");
            n.setAggregateKey(key);
            n.setAggregateCount(1);
            n.setAudienceDesc("任课教师 + 辅导员");
            n.setPriority("HIGH");
            n.setExpireTime(now.plusDays(7));
            n.setCreateTime(now);
            n.setUpdateTime(now);
            notificationMapper.insert(n);
        } else {
            n.setAggregateCount((n.getAggregateCount() == null ? 1 : n.getAggregateCount()) + 1);
            n.setContent(studentText + " 再次申诉：" + trim(reason));
            n.setUpdateTime(now);
            notificationMapper.updateById(n);
        }
        sendReceipts(n.getId(), accounts, roleOf, now);
    }

    /** 单条预警及其时间线（教师端详情用） */
    public Map<String, Object> detail(LoginUser user, Long alertId) {
        AlertRecord a = requireMyAlert(user, alertId);
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("alert", enrich(toRow(a)));
        map.put("timeline", flowMapper.selectList(Wrappers.<AlertFlowLog>lambdaQuery()
                .eq(AlertFlowLog::getAlertId, alertId)
                .orderByAsc(AlertFlowLog::getEventTime)
                .orderByAsc(AlertFlowLog::getId)));
        map.put("interventions", interventionMapper.selectList(Wrappers.<InterventionRecord>lambdaQuery()
                .eq(InterventionRecord::getAlertId, alertId)
                .orderByDesc(InterventionRecord::getId)));
        RiskState s = riskStateMapper.selectOne(Wrappers.<RiskState>lambdaQuery()
                .eq(RiskState::getEnrollmentId, a.getEnrollmentId()).last("limit 1"));
        map.put("riskState", s);
        return map;
    }

    // ============================================================ 通知
    /** 教师 + 辅导员：按「教学班 + 等级 + 日期」聚合成一条汇总消息 */
    private void notifyTeacherSide(AlertRecord a, RiskState s, String bizType, String label) {
        TeachingClass tc = classMapper.selectById(a.getTeachingClassId());
        if (tc == null) {
            return;
        }
        Teacher teacher = teacherMapper.selectById(tc.getTeacherId());
        Set<Long> accounts = new LinkedHashSet<>();
        Map<Long, String> roleOf = new LinkedHashMap<>();
        if (teacher != null && teacher.getAccountId() != null) {
            accounts.add(teacher.getAccountId());
            roleOf.put(teacher.getAccountId(), "TEACHER");
        }
        for (Counselor c : counselorsOfClass(tc.getId(), a.getTermId())) {
            if (c.getAccountId() != null) {
                accounts.add(c.getAccountId());
                roleOf.putIfAbsent(c.getAccountId(), "COUNSELOR");
            }
        }
        if (accounts.isEmpty()) {
            return;
        }
        String key = "ALERT:" + a.getTeachingClassId() + ":" + a.getAlertLevel() + ":" + LocalDate.now().format(DAY);
        Notification n = notificationMapper.selectOne(Wrappers.<Notification>lambdaQuery()
                .eq(Notification::getAggregateKey, key).last("limit 1"));
        Student st = studentMapper.selectById(a.getStudentId());
        Course course = a.getCourseId() == null ? null : courseMapper.selectById(a.getCourseId());
        String who = tc.getClassCode() + "（" + nvl(course == null ? "" : course.getCourseName()) + "）";
        String studentText = st == null ? "有学生" : (st.getStudentNo() + " " + st.getStudentName());
        LocalDateTime now = LocalDateTime.now();
        if (n == null) {
            n = new Notification();
            n.setBizType(bizType);
            n.setBizId(a.getId());
            n.setTitle(label + "：" + who + " " + a.getAlertLevel());
            n.setContent(studentText + " 触发" + a.getAlertLevel() + "预警：" + a.getAlertReason()
                    + "。请在「预警监控」中查看并处理。");
            n.setPayloadJson("{\"route\":\"/teacher/monitor\",\"alertId\":" + a.getId()
                    + ",\"teachingClassId\":" + a.getTeachingClassId() + "}");
            n.setAggregateKey(key);
            n.setAggregateCount(1);
            n.setAudienceDesc("任课教师 + 辅导员");
            n.setPriority("RED".equals(a.getAlertLevel()) ? "HIGH" : "NORMAL");
            n.setExpireTime(now.plusDays(5));
            n.setCreateBy(null);
            n.setCreateTime(now);
            n.setUpdateTime(now);
            notificationMapper.insert(n);
        } else {
            n.setAggregateCount((n.getAggregateCount() == null ? 1 : n.getAggregateCount()) + 1);
            n.setContent("本教学班已有 " + n.getAggregateCount() + " 条 " + a.getAlertLevel()
                    + " 预警（最近：" + studentText + "）。请在「预警监控」中查看并处理。");
            n.setUpdateTime(now);
            notificationMapper.updateById(n);
        }
        sendReceipts(n.getId(), accounts, roleOf, now);
    }

    /** 学生本人：一条预警一条消息（同一天同一等级去重） */
    private void notifyStudent(AlertRecord a, RiskState s, String bizType, String label) {
        Enrollment e = enrollmentMapper.selectById(a.getEnrollmentId());
        if (e == null) {
            return;
        }
        Student st = studentMapper.selectById(a.getStudentId());
        if (st == null || st.getAccountId() == null) {
            return;
        }
        Course course = a.getCourseId() == null ? null : courseMapper.selectById(a.getCourseId());
        String key = "ALERT_STU:" + a.getEnrollmentId() + ":" + a.getAlertLevel() + ":" + LocalDate.now().format(DAY);
        Notification n = notificationMapper.selectOne(Wrappers.<Notification>lambdaQuery()
                .eq(Notification::getAggregateKey, key).last("limit 1"));
        if (n != null) {
            return;
        }
        LocalDateTime now = LocalDateTime.now();
        n = new Notification();
        n.setBizType(bizType);
        n.setBizId(a.getId());
        n.setTitle(label + "：《" + nvl(course == null ? "课程" : course.getCourseName()) + "》学情预警");
        n.setContent("你的" + nvl(course == null ? "课程" : course.getCourseName()) + "课程触发了"
                + a.getAlertLevel() + "预警：" + a.getAlertReason()
                + "。到「学情预警」查看依据与改进建议。");
        n.setPayloadJson("{\"route\":\"/student/warnings\",\"alertId\":" + a.getId() + "}");
        n.setAggregateKey(key);
        n.setAggregateCount(1);
        n.setAudienceDesc("学生本人");
        n.setPriority("HIGH");
        n.setExpireTime(now.plusDays(5));
        n.setCreateTime(now);
        n.setUpdateTime(now);
        notificationMapper.insert(n);
        Set<Long> accounts = new LinkedHashSet<>();
        accounts.add(st.getAccountId());
        Map<Long, String> roleOf = new LinkedHashMap<>();
        Long stuAccount = st.getAccountId();
        roleOf.put(stuAccount, "STUDENT");
        sendReceipts(n.getId(), accounts, roleOf, now);
    }

    private void sendReceipts(Long notificationId, Set<Long> accounts, Map<Long, String> roleOf, LocalDateTime now) {
        List<Long> existing = receiptMapper.selectList(Wrappers.<NotificationReceipt>lambdaQuery()
                        .eq(NotificationReceipt::getNotificationId, notificationId))
                .stream().map(NotificationReceipt::getRecipientId).collect(Collectors.toList());
        for (Long accountId : accounts) {
            if (existing.contains(accountId)) {
                continue;
            }
            NotificationReceipt r = new NotificationReceipt();
            r.setNotificationId(notificationId);
            r.setRecipientId(accountId);
            r.setRecipientRole(roleOf.getOrDefault(accountId, "TEACHER"));
            r.setChannel("IN_APP");
            r.setSendStatus("SENT");
            r.setSendTime(now);
            r.setIsRead(0);
            r.setAckRequired(0);
            r.setAckStatus("NONE");
            r.setCreateTime(now);
            r.setUpdateTime(now);
            receiptMapper.insert(r);
        }
    }

    private List<Counselor> counselorsOfClass(Long teachingClassId, Long termId) {
        List<Long> adminClassIds = tcacMapper.selectList(Wrappers.<TeachingClassAdminClass>lambdaQuery()
                        .eq(TeachingClassAdminClass::getTeachingClassId, teachingClassId))
                .stream().map(TeachingClassAdminClass::getAdminClassId).collect(Collectors.toList());
        if (adminClassIds.isEmpty()) {
            return List.of();
        }
        List<Long> counselorIds = counselorClassMapper.selectList(Wrappers.<CounselorClass>lambdaQuery()
                        .in(CounselorClass::getAdminClassId, adminClassIds)
                        .eq(termId != null, CounselorClass::getTermId, termId))
                .stream().map(CounselorClass::getCounselorId).distinct().collect(Collectors.toList());
        if (counselorIds.isEmpty()) {
            return List.of();
        }
        return counselorMapper.selectBatchIds(counselorIds);
    }

    // ============================================================ 内部
    private AlertRecord currentAlert(Long enrollmentId) {
        return alertMapper.selectOne(Wrappers.<AlertRecord>lambdaQuery()
                .eq(AlertRecord::getEnrollmentId, enrollmentId)
                .eq(AlertRecord::getIsCurrent, 1)
                .eq(AlertRecord::getIsVoid, 0)
                .last("limit 1"));
    }

    private AlertRecord requireMyAlert(LoginUser user, Long alertId) {
        AlertRecord a = alertMapper.selectById(alertId);
        if (a == null || (a.getIsVoid() != null && a.getIsVoid() == 1)) {
            throw new BizException("预警不存在");
        }
        if ("ADMIN".equals(user.getRole())) {
            return a;
        }
        TeachingClass tc = classMapper.selectById(a.getTeachingClassId());
        if (tc == null || user.getRefId() == null || !user.getRefId().equals(tc.getTeacherId())) {
            throw new BizException("只能处理自己教学班的预警");
        }
        return a;
    }

    private void writeFlow(AlertRecord a, Long studentId, Long termId, String eventType,
                           String fromLevel, String toLevel, String fromStatus, String toStatus,
                           String detail, Long operatorId, String role) {
        AlertFlowLog f = new AlertFlowLog();
        f.setAlertId(a.getId());
        f.setStudentId(studentId);
        f.setTermId(termId);
        f.setEventType(eventType);
        f.setFromLevel(fromLevel);
        f.setToLevel(toLevel);
        f.setFromStatus(fromStatus);
        f.setToStatus(toStatus);
        f.setDetail(detail);
        f.setOperatorId(operatorId);
        f.setOperatorRole(role);
        f.setEventTime(LocalDateTime.now());
        f.setCreateTime(LocalDateTime.now());
        flowMapper.insert(f);
    }

    private Long courseIdOf(Long offeringId) {
        CourseOffering o = offeringId == null ? null : offeringMapper.selectById(offeringId);
        return o == null ? null : o.getCourseId();
    }

    private String ruleOf(RiskState s) {
        if (s.getHardFail() != null && s.getHardFail() == 1) {
            return "A_HARD_LINE";
        }
        if (s.getFinalUnreachable() != null && s.getFinalUnreachable() == 1) {
            return "A_UNREACHABLE";
        }
        if (s.getRemainingAbsent() != null && s.getRemainingAbsent() <= 1) {
            return "A_NEAR_LINE";
        }
        return "B_RANK";
    }

    private String reasonOf(RiskState s) {
        if (s.getHardFail() != null && s.getHardFail() == 1) {
            return "缺课 " + nz(s.getAbsentCount()) + " 次，已超过允许上限 " + nz(s.getAllowedAbsent())
                    + " 次（缺课超 1/3 直接挂科）";
        }
        if (s.getFinalUnreachable() != null && s.getFinalUnreachable() == 1) {
            return "期末需考 " + (s.getFinalExamNeeded() == null ? "—" : s.getFinalExamNeeded())
                    + " 分，已超过期末满分，实际不可达";
        }
        if (s.getRemainingAbsent() != null && s.getRemainingAbsent() <= 0) {
            return "缺课 " + nz(s.getAbsentCount()) + " 次，剩余可缺为 " + s.getRemainingAbsent()
                    + "，再缺一次即触发 1/3 硬门槛";
        }
        if (s.getRemainingAbsent() != null && s.getRemainingAbsent() == 1) {
            return "缺课 " + nz(s.getAbsentCount()) + " 次，只剩 1 次可缺";
        }
        return "班内相对风险排名靠前（第 " + nz(s.getChannelBRank()) + " / " + nz(s.getChannelBClassSize())
                + "），当前总评偏低";
    }

    private String evidenceOf(RiskState s) {
        return "{\"absent\":" + nz(s.getAbsentCount())
                + ",\"late\":" + nz(s.getLateCount())
                + ",\"leave\":" + nz(s.getLeaveCount())
                + ",\"allowed\":" + nz(s.getAllowedAbsent())
                + ",\"remaining\":" + nz(s.getRemainingAbsent())
                + ",\"finalNeeded\":" + (s.getFinalExamNeeded() == null ? "null" : s.getFinalExamNeeded())
                + ",\"finalUnreachable\":" + nz(s.getFinalUnreachable())
                + ",\"rank\":" + nz(s.getChannelBRank())
                + ",\"classSize\":" + nz(s.getChannelBClassSize())
                + ",\"confidence\":" + (s.getConfidence() == null ? "null" : s.getConfidence()) + "}";
    }

    private List<AlertRowVO> toRows(List<AlertRecord> alerts) {
        if (alerts.isEmpty()) {
            return List.of();
        }
        Map<Long, Student> students = studentMapper.selectBatchIds(alerts.stream()
                        .map(AlertRecord::getStudentId).distinct().collect(Collectors.toList()))
                .stream().collect(Collectors.toMap(Student::getId, (x) -> x, (a, b) -> a));
        Map<Long, TeachingClass> classes = classMapper.selectBatchIds(alerts.stream()
                        .map(AlertRecord::getTeachingClassId).distinct().collect(Collectors.toList()))
                .stream().collect(Collectors.toMap(TeachingClass::getId, (x) -> x, (a, b) -> a));
        Map<Long, Course> courses = courseMapper.selectBatchIds(alerts.stream()
                        .map(AlertRecord::getCourseId).filter((x) -> x != null).distinct().collect(Collectors.toList()))
                .stream().collect(Collectors.toMap(Course::getId, (x) -> x, (a, b) -> a));
        Map<Long, RiskState> states = new LinkedHashMap<>();
        riskStateMapper.selectList(Wrappers.<RiskState>lambdaQuery()
                        .in(RiskState::getEnrollmentId, alerts.stream()
                                .map(AlertRecord::getEnrollmentId).collect(Collectors.toList())))
                .forEach((s) -> states.put(s.getEnrollmentId(), s));
        List<AlertRowVO> out = new ArrayList<>();
        for (AlertRecord a : alerts) {
            AlertRowVO v = toRow(a);
            Student st = students.get(a.getStudentId());
            if (st != null) {
                v.setStudentNo(st.getStudentNo());
                v.setStudentName(st.getStudentName());
            }
            TeachingClass tc = classes.get(a.getTeachingClassId());
            if (tc != null) {
                v.setClassCode(tc.getClassCode());
                v.setClassName(tc.getClassName());
            }
            Course c = courses.get(a.getCourseId());
            if (c != null) {
                v.setCourseName(c.getCourseName());
            }
            RiskState s = states.get(a.getEnrollmentId());
            if (s != null) {
                v.setAbsentCount(s.getAbsentCount());
                v.setAllowedAbsent(s.getAllowedAbsent());
                v.setRemainingAbsent(s.getRemainingAbsent());
                v.setHardFail(s.getHardFail());
                v.setNearFail(s.getNearFail());
                v.setFinalExamNeeded(s.getFinalExamNeeded());
                v.setFinalUnreachable(s.getFinalUnreachable());
                v.setChannelALevel(s.getChannelALevel());
                v.setChannelBScore(s.getChannelBScore());
                v.setChannelBRank(s.getChannelBRank());
                v.setChannelBClassSize(s.getChannelBClassSize());
            }
            out.add(v);
        }
        return out;
    }

    private AlertRowVO toRow(AlertRecord a) {
        AlertRowVO v = new AlertRowVO();
        v.setId(a.getId());
        v.setStudentId(a.getStudentId());
        v.setEnrollmentId(a.getEnrollmentId());
        v.setOfferingId(a.getOfferingId());
        v.setTeachingClassId(a.getTeachingClassId());
        v.setAlertLevel(a.getAlertLevel());
        v.setTriggerChannel(a.getTriggerChannel());
        v.setTriggerRule(a.getTriggerRule());
        v.setRiskScore(a.getRiskScore());
        v.setConfidence(a.getConfidence());
        v.setOutputMode(a.getOutputMode());
        v.setAlertReason(a.getAlertReason());
        v.setStatus(a.getStatus());
        v.setIsCurrent(a.getIsCurrent());
        v.setEscalated(a.getEscalated());
        v.setPreviousLevel(a.getPreviousLevel());
        v.setNotified(a.getNotified());
        v.setFirstNotifyTime(a.getFirstNotifyTime());
        v.setExpireTime(a.getExpireTime());
        v.setHandleBy(a.getHandleBy());
        v.setHandleRole(a.getHandleRole());
        v.setHandleTime(a.getHandleTime());
        v.setHandleNote(a.getHandleNote());
        v.setCloseTime(a.getCloseTime());
        v.setCloseNote(a.getCloseNote());
        v.setStudentResponse(a.getStudentResponse());
        v.setStudentResponseTime(a.getStudentResponseTime());
        v.setEngineVersion(a.getEngineVersion());
        v.setCreateTime(a.getCreateTime());
        return v;
    }

    /** 单条返回时补齐学号姓名、课程名、班级名与三通道关键数字 */
    private AlertRowVO enrich(AlertRowVO v) {
        Student st = studentMapper.selectById(v.getStudentId());
        if (st != null) {
            v.setStudentNo(st.getStudentNo());
            v.setStudentName(st.getStudentName());
        }
        TeachingClass tc = classMapper.selectById(v.getTeachingClassId());
        if (tc != null) {
            v.setClassCode(tc.getClassCode());
            v.setClassName(tc.getClassName());
        }
        if (v.getOfferingId() != null) {
            CourseOffering o = offeringMapper.selectById(v.getOfferingId());
            if (o != null) {
                Course c = courseMapper.selectById(o.getCourseId());
                v.setCourseName(c == null ? null : c.getCourseName());
            }
        }
        RiskState s = riskStateMapper.selectOne(Wrappers.<RiskState>lambdaQuery()
                .eq(RiskState::getEnrollmentId, v.getEnrollmentId()).last("limit 1"));
        if (s != null) {
            v.setAbsentCount(s.getAbsentCount());
            v.setAllowedAbsent(s.getAllowedAbsent());
            v.setRemainingAbsent(s.getRemainingAbsent());
            v.setHardFail(s.getHardFail());
            v.setNearFail(s.getNearFail());
            v.setFinalExamNeeded(s.getFinalExamNeeded());
            v.setFinalUnreachable(s.getFinalUnreachable());
            v.setChannelALevel(s.getChannelALevel());
            v.setChannelBScore(s.getChannelBScore());
            v.setChannelBRank(s.getChannelBRank());
            v.setChannelBClassSize(s.getChannelBClassSize());
        }
        return v;
    }

    private int rank(String level) {
        if (level == null) {
            return 0;
        }
        for (int i = 0; i < LEVELS.length; i++) {
            if (LEVELS[i].equals(level)) {
                return i;
            }
        }
        return 0;
    }

    private static int nz(Integer v) {
        return v == null ? 0 : v;
    }

    private static String nvl(String v) {
        return v == null ? "" : v;
    }

    private static String trim(String v) {
        if (v == null) {
            return null;
        }
        return v.length() <= 500 ? v : v.substring(0, 500);
    }

    /** audit_log 的 before/after 是 JSON 列，这里只放等级变化 */
    private static String jsonLevel(String from, String to) {
        return "{\"from\":" + (from == null ? "null" : "\"" + from + "\"")
                + ",\"to\":" + (to == null ? "null" : "\"" + to + "\"") + "}";
    }

    private static BigDecimal nz(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }
}
