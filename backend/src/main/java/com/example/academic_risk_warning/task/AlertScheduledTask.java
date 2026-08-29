package com.example.academic_risk_warning.task;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.academic_risk_warning.config.WarningSystemProperties;
import com.example.academic_risk_warning.entity.AlertRecord;
import com.example.academic_risk_warning.entity.Student;
import com.example.academic_risk_warning.entity.Teacher;
import com.example.academic_risk_warning.entity.TeacherClass;
import com.example.academic_risk_warning.mapper.AlertRecordMapper;
import com.example.academic_risk_warning.mapper.StudentMapper;
import com.example.academic_risk_warning.mapper.TeacherClassMapper;
import com.example.academic_risk_warning.mapper.TeacherMapper;
import com.example.academic_risk_warning.service.NotificationService;
import com.example.academic_risk_warning.service.WarningGenerationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 预警定时任务
 */
@Component
public class AlertScheduledTask {

    private static final Logger log = LoggerFactory.getLogger(AlertScheduledTask.class);

    private final WarningGenerationService warningGenerationService;
    private final AlertRecordMapper alertRecordMapper;
    private final TeacherMapper teacherMapper;
    private final NotificationService notificationService;
    private final StudentMapper studentMapper;
    private final TeacherClassMapper teacherClassMapper;
    private final WarningSystemProperties warningProperties;

    public AlertScheduledTask(WarningGenerationService warningGenerationService,
                              AlertRecordMapper alertRecordMapper,
                              TeacherMapper teacherMapper,
                              NotificationService notificationService,
                              StudentMapper studentMapper,
                              TeacherClassMapper teacherClassMapper,
                              WarningSystemProperties warningProperties) {
        this.warningGenerationService = warningGenerationService;
        this.alertRecordMapper = alertRecordMapper;
        this.teacherMapper = teacherMapper;
        this.notificationService = notificationService;
        this.studentMapper = studentMapper;
        this.teacherClassMapper = teacherClassMapper;
        this.warningProperties = warningProperties;
    }

    /**
     * 每天凌晨2点执行全量预警生成
     */
    @Scheduled(cron = "0 0 2 * * ?")
    public void generateDailyAlerts() {
        log.info("【定时任务】开始执行每日预警生成...");
        long startTime = System.currentTimeMillis();
        try {
            List<Teacher> teachers = teacherMapper.selectList(null);
            int totalAlerts = 0;
            for (Teacher teacher : teachers) {
                try {
                    int count = warningGenerationService.generateWarningsForTeacher(teacher.getId());
                    totalAlerts += count;
                    log.info("教师 {} 生成预警 {} 条", teacher.getTeacherNo(), count);
                } catch (Exception e) {
                    log.error("教师 {} 生成预警失败: {}", teacher.getTeacherNo(), e.getMessage());
                }
            }
            long elapsed = System.currentTimeMillis() - startTime;
            log.info("【定时任务】每日预警生成完成，共生成 {} 条预警，耗时 {} ms", totalAlerts, elapsed);
        } catch (Exception e) {
            log.error("【定时任务】每日预警生成失败: {}", e.getMessage(), e);
        }
    }

    /** 每周一早8点周度分析 */
    @Scheduled(cron = "0 0 8 ? * MON")
    public void generateWeeklyAlerts() {
        log.info("【定时任务】开始执行周度预警分析...");
        try {
            int count = warningGenerationService.generateAllWarnings();
            log.info("【定时任务】周度预警分析完成，共生成 {} 条预警", count);
        } catch (Exception e) {
            log.error("【定时任务】周度预警分析失败: {}", e.getMessage(), e);
        }
    }

    /** 每小时清理过期预警 */
    @Scheduled(cron = "0 0 * * * ?")
    public void cleanupExpiredAlerts() {
        try {
            int count = alertRecordMapper.delete(
                    new LambdaQueryWrapper<AlertRecord>()
                            .eq(AlertRecord::getStatus, "DISMISSED")
                            .lt(AlertRecord::getExpireTime, LocalDateTime.now()));
            if (count > 0) log.info("【定时任务】清理过期预警 {} 条", count);
        } catch (Exception e) {
            log.error("【定时任务】清理过期预警失败: {}", e.getMessage(), e);
        }
    }

    /** 每5分钟更新通知状态 */
    @Scheduled(cron = "0 */5 * * * ?")
    public void updateNotificationStatus() {
        try {
            int count = alertRecordMapper.update(null,
                    new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<AlertRecord>()
                            .eq(AlertRecord::getNotified, false)
                            .set(AlertRecord::getNotified, true)
                            .last("LIMIT 100"));
            if (count > 0) log.info("【定时任务】更新通知状态 {} 条", count);
        } catch (Exception e) {
            log.error("【定时任务】更新通知状态失败: {}", e.getMessage(), e);
        }
    }

    // ==================== 超时升级 ====================

    /** 每天凌晨1点检查超时预警 */
    @Scheduled(cron = "0 0 1 * * ?")
    @Transactional
    public void escalateTimeouts() {
        log.info("【超时升级】开始检查超时预警...");
        LocalDateTime now = LocalDateTime.now();
        int escalated = 0;

        try {
            // 1. ACTIVE > N天 → 通知教师
            int days1 = warningProperties.getEscalateActiveFirstDays();
            List<AlertRecord> active2d = alertRecordMapper.selectList(
                    new LambdaQueryWrapper<AlertRecord>()
                            .eq(AlertRecord::getStatus, "ACTIVE")
                            .lt(AlertRecord::getCreateTime, now.minusDays(days1)));
            for (AlertRecord a : active2d) {
                Long teacherId = findTeacher(a.getStudentId());
                if (teacherId == null) continue;
                Student s = studentMapper.selectById(a.getStudentId());
                String name = s != null ? s.getStudentName() : "学生" + a.getStudentId();
                notificationService.create(teacherId, "TEACHER",
                        "⏰ 催办：" + name + "的预警已超" + days1 + "天未处理",
                        name + "的" + levelText(a.getAlertLevel()) + "预警生成于" + fmt(a.getCreateTime()) + "，已超过" + days1 + "天未处理，请尽快处理。",
                        a.getId(), "TIMEOUT");
            }

            // 2. ACTIVE > N天 → 再催教师(不升级)
            int days2 = warningProperties.getEscalateActiveSecondDays();
            List<AlertRecord> active5d = alertRecordMapper.selectList(
                    new LambdaQueryWrapper<AlertRecord>()
                            .eq(AlertRecord::getStatus, "ACTIVE")
                            .lt(AlertRecord::getCreateTime, now.minusDays(days2)));
            for (AlertRecord a : active5d) {
                Long teacherId = findTeacher(a.getStudentId());
                if (teacherId == null) continue;
                Student s = studentMapper.selectById(a.getStudentId());
                String name = s != null ? s.getStudentName() : "学生" + a.getStudentId();
                notificationService.create(teacherId, "TEACHER",
                        "🚨 严重提醒：" + name + "的预警已超" + days2 + "天未处理",
                        name + "的" + levelText(a.getAlertLevel()) + "预警已超过" + days2 + "天未处理，请立即处理！",
                        a.getId(), "TIMEOUT");
            }

            // 3. ACKNOWLEDGED > N天 → 催教师
            int daysAcked = warningProperties.getEscalateAckedDays();
            List<AlertRecord> acked3d = alertRecordMapper.selectList(
                    new LambdaQueryWrapper<AlertRecord>()
                            .eq(AlertRecord::getStatus, "ACKNOWLEDGED")
                            .lt(AlertRecord::getAckTime, now.minusDays(daysAcked)));
            for (AlertRecord a : acked3d) {
                Long teacherId = findTeacher(a.getStudentId());
                if (teacherId == null) continue;
                Student s = studentMapper.selectById(a.getStudentId());
                String name = s != null ? s.getStudentName() : "学生" + a.getStudentId();
                notificationService.create(teacherId, "TEACHER",
                        "⏰ 催办：" + name + "已确认预警，请及时处理",
                        name + "已于" + daysAcked + "天前确认收到预警，请尽快进行处理。",
                        a.getId(), "TIMEOUT");
            }

            // 4. HANDLED > N天 → 催学生回应
            int daysHandled = warningProperties.getEscalateHandledFirstDays();
            List<AlertRecord> handled3d = alertRecordMapper.selectList(
                    new LambdaQueryWrapper<AlertRecord>()
                            .eq(AlertRecord::getStatus, "HANDLED")
                            .lt(AlertRecord::getHandleTime, now.minusDays(daysHandled)));
            for (AlertRecord a : handled3d) {
                // 已回应过则跳过
                if (a.getTeacherNote() != null && a.getTeacherNote().contains("【学生回应】")) continue;
                Student s = studentMapper.selectById(a.getStudentId());
                String name = s != null ? s.getStudentName() : "同学";
                notificationService.create(a.getStudentId(), "STUDENT",
                        "⏰ 老师已处理你的预警，请回应",
                        "老师已在" + daysHandled + "天前处理了你的学业预警，请登录系统查看处理意见并回应。",
                        a.getId(), "TIMEOUT");
            }

            // 5. HANDLED > N天 + 学生未回应 → 升级等级 + 通知双方
            int daysUpgrade = warningProperties.getEscalateHandledUpgradeDays();
            List<AlertRecord> handled5d = alertRecordMapper.selectList(
                    new LambdaQueryWrapper<AlertRecord>()
                            .eq(AlertRecord::getStatus, "HANDLED")
                            .lt(AlertRecord::getHandleTime, now.minusDays(daysUpgrade)));
            for (AlertRecord a : handled5d) {
                // 已回应过 → 跳过
                if (a.getTeacherNote() != null && a.getTeacherNote().contains("【学生回应】")) continue;
                // 已经最高等级 → 跳过
                String newLevel = levelUp(a.getAlertLevel());
                if (newLevel == null || newLevel.equals(a.getAlertLevel())) continue;

                a.setAlertLevel(newLevel);
                alertRecordMapper.updateById(a);
                escalated++;

                Student s = studentMapper.selectById(a.getStudentId());
                String name = s != null ? s.getStudentName() : "学生" + a.getStudentId();
                Long teacherId = findTeacher(a.getStudentId());

                // 通知学生
                notificationService.create(a.getStudentId(), "STUDENT",
                        "⚠️ 预警升级：因未回应，风险等级已提升为" + levelText(newLevel),
                        "由于你超过" + daysUpgrade + "天未回应老师的处理意见，你的学业预警等级已从" + levelText(a.getAlertLevel())
                                + "自动升级为" + levelText(newLevel) + "。请尽快登录查看并回应。",
                        a.getId(), "ALERT_UPGRADE");

                // 通知教师
                if (teacherId != null) {
                    notificationService.create(teacherId, "TEACHER",
                            "⚠️ " + name + "的预警因未回应自动升级为" + levelText(newLevel),
                            name + "超过" + daysUpgrade + "天未回应处理意见，预警已自动从" + levelText(a.getAlertLevel())
                                    + "升级为" + levelText(newLevel) + "。",
                            a.getId(), "ALERT_UPGRADE");
                }
            }

            log.info("【超时升级】完成，催办 {} 条，升级 {} 条",
                    active2d.size() + active5d.size() + acked3d.size() + handled3d.size() + handled5d.size(),
                    escalated);
        } catch (Exception e) {
            log.error("【超时升级】失败: {}", e.getMessage(), e);
        }
    }

    private Long findTeacher(Long studentId) {
        Student s = studentMapper.selectById(studentId);
        if (s == null) return null;
        List<TeacherClass> list = teacherClassMapper.selectList(
                new LambdaQueryWrapper<TeacherClass>().eq(TeacherClass::getClassName, s.getClassName()));
        for (TeacherClass tc : list) {
            if (tc.getStudentIdStart() != null && tc.getStudentIdEnd() != null
                    && studentId >= tc.getStudentIdStart() && studentId <= tc.getStudentIdEnd()) {
                return tc.getTeacherId();
            }
        }
        return list.isEmpty() ? null : list.get(0).getTeacherId();
    }

    private String levelText(String l) {
        return switch (l) {
            case "RED" -> "红色（高风险）";
            case "ORANGE" -> "橙色（中风险）";
            case "YELLOW" -> "黄色（低风险）";
            default -> l;
        };
    }

    private String levelUp(String l) {
        return switch (l) {
            case "YELLOW" -> "ORANGE";
            case "ORANGE" -> "RED";
            default -> null; // RED不再升级
        };
    }

    private String fmt(LocalDateTime t) {
        return t != null ? t.toLocalDate().toString() : "";
    }
}
