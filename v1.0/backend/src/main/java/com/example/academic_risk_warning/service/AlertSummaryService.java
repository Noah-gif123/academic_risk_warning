package com.example.academic_risk_warning.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.academic_risk_warning.entity.*;
import com.example.academic_risk_warning.mapper.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

/**
 * 预警汇总服务: 每日/每周生成摘要通知
 */
@Service
public class AlertSummaryService {

    private static final Logger log = LoggerFactory.getLogger(AlertSummaryService.class);

    private final AlertRecordMapper alertRecordMapper;
    private final StudentMapper studentMapper;
    private final TeacherClassMapper teacherClassMapper;
    private final TeacherMapper teacherMapper;
    private final NotificationService notificationService;

    public AlertSummaryService(AlertRecordMapper alertRecordMapper,
                               StudentMapper studentMapper,
                               TeacherClassMapper teacherClassMapper,
                               TeacherMapper teacherMapper,
                               NotificationService notificationService) {
        this.alertRecordMapper = alertRecordMapper;
        this.studentMapper = studentMapper;
        this.teacherClassMapper = teacherClassMapper;
        this.teacherMapper = teacherMapper;
        this.notificationService = notificationService;
    }

    /** 为所有教师生成每日/每周汇总（定时任务用） */
    public void generateAllSummaries(String period) {
        List<Teacher> teachers = teacherMapper.selectList(null);
        for (Teacher t : teachers) {
            try {
                generateSummaryForTeacher(t.getId(), period);
            } catch (Exception e) {
                log.error("教师 {} 汇总失败: {}", t.getTeacherNo(), e.getMessage());
            }
        }
    }

    /** 为单个教师生成汇总，返回 true 表示成功生成了通知 */
    public boolean generateSummaryForTeacher(Long teacherId, String period) {
        try {
            String summary = buildSummary(teacherId, period);
            if (summary == null || summary.isEmpty()) return false;
            String title = "DAILY".equals(period)
                    ? "【每日预警汇总】" + LocalDate.now()
                    : "【本周预警周报】" + getWeekRange();
            notificationService.create(teacherId, "TEACHER", title, summary,
                    null, "DAILY".equals(period) ? "DAILY_SUMMARY" : "WEEKLY_SUMMARY");
            return true;
        } catch (Exception e) {
            log.error("教师汇总失败: {}", e.getMessage());
            return false;
        }
    }

    /** 查询历史汇总 */
    public List<Notification> getSummaries(Long teacherId, String period, int limit) {
        String type = "DAILY".equals(period) ? "DAILY_SUMMARY" : "WEEKLY_SUMMARY";
        Page<Notification> p = new Page<>(1, limit);
        Page<Notification> result = notificationService.getMapper().selectPage(p,
                new LambdaQueryWrapper<Notification>()
                        .eq(Notification::getRecipientId, teacherId)
                        .eq(Notification::getRecipientType, "TEACHER")
                        .eq(Notification::getType, type)
                        .orderByDesc(Notification::getCreateTime));
        return result.getRecords();
    }

    // ==================== 摘要生成 ====================

    private String buildSummary(Long teacherId, String period) {
        List<Long> studentIds = getStudentIds(teacherId);
        if (studentIds.isEmpty()) return null;

        LocalDateTime start, end;
        if ("DAILY".equals(period)) {
            start = LocalDate.now().atStartOfDay();
            end = LocalDateTime.now();
        } else {
            start = LocalDate.now().with(DayOfWeek.MONDAY).atStartOfDay();
            end = LocalDate.now().with(DayOfWeek.SUNDAY).plusDays(1).atStartOfDay();
            if (end.isAfter(LocalDateTime.now())) end = LocalDateTime.now();
        }

        // 本期预警
        List<AlertRecord> periodAlerts = alertRecordMapper.selectList(
                new LambdaQueryWrapper<AlertRecord>()
                        .in(AlertRecord::getStudentId, studentIds)
                        .between(AlertRecord::getCreateTime, start, end));

        if (periodAlerts.isEmpty()) {
            return "DAILY".equals(period)
                    ? "【每日预警汇总】" + LocalDate.now() + "\n━━━━━━━━━━\n✅ 今日无新增预警，一切正常"
                    : "【本周预警周报】" + getWeekRange() + "\n━━━━━━━━━━\n✅ 本周无新增预警，一切正常";
        }

        // 统计
        long red = count(periodAlerts, "RED"), orange = count(periodAlerts, "ORANGE"), yellow = count(periodAlerts, "YELLOW");
        long total = periodAlerts.size();
        long active = countByStatus(periodAlerts, "ACTIVE");
        long handled = countByStatus(periodAlerts, "HANDLED") + countByStatus(periodAlerts, "CLOSED");
        double handleRate = total > 0 ? (handled * 100.0 / total) : 0;

        // Top 3
        StringBuilder top3 = new StringBuilder();
        periodAlerts.stream()
                .filter(a -> a.getRiskScore() != null)
                .sorted((a, b) -> b.getRiskScore().compareTo(a.getRiskScore()))
                .limit(3)
                .forEach(a -> {
                    Student s = studentMapper.selectById(a.getStudentId());
                    String name = s != null ? s.getStudentName() : "学生" + a.getStudentId();
                    top3.append(String.format("  %s - %.1f分(%s)\n", name, a.getRiskScore(), a.getAlertLevel()));
                });

        // 班级分布
        Map<String, int[]> classDist = new LinkedHashMap<>();
        for (AlertRecord a : periodAlerts) {
            Student s = studentMapper.selectById(a.getStudentId());
            String cls = s != null ? s.getClassName() : "未知";
            int[] arr = classDist.computeIfAbsent(cls, k -> new int[3]);
            if ("RED".equals(a.getAlertLevel())) arr[0]++;
            else if ("ORANGE".equals(a.getAlertLevel())) arr[1]++;
            else arr[2]++;
        }

        // 上周对比
        LocalDateTime lastStart = start.minusDays(7);
        LocalDateTime lastEnd = start;
        List<AlertRecord> lastAlerts = alertRecordMapper.selectList(
                new LambdaQueryWrapper<AlertRecord>()
                        .in(AlertRecord::getStudentId, studentIds)
                        .between(AlertRecord::getCreateTime, lastStart, lastEnd));
        long lastRed = count(lastAlerts, "RED"), lastOrange = count(lastAlerts, "ORANGE"), lastYellow = count(lastAlerts, "YELLOW");

        String trend = buildTrend(lastRed, red, lastOrange, orange, lastYellow, yellow);

        // 拼接
        StringBuilder sb = new StringBuilder();
        if ("DAILY".equals(period)) {
            sb.append("【每日预警汇总】").append(LocalDate.now()).append("\n");
            sb.append("━━━━━━━━━━━━━━━━━━━\n");
            sb.append(String.format("📊 今日新增: %d条（🔴%d 🟠%d 🟡%d）\n", total, red, orange, yellow));
            sb.append(String.format("⚠️ 待处理: %d条\n", active));
            sb.append(String.format("✅ 已处理: %d条 (%.0f%%)\n", handled, handleRate));
        } else {
            sb.append("【本周预警周报】").append(getWeekRange()).append("\n");
            sb.append("━━━━━━━━━━━━━━━━━━━\n");
            sb.append(String.format("📊 本周新增: %d条（🔴%d 🟠%d 🟡%d）\n", total, red, orange, yellow));
            if (!trend.isEmpty()) sb.append(trend).append("\n");
            sb.append(String.format("✅ 处理率: %.1f%%\n", handleRate));
            sb.append(String.format("⚠️ 未处理: %d条\n", active));
        }

        if (top3.length() > 0) {
            sb.append("\n📈 高风险学生:\n").append(top3);
        }

        if (!classDist.isEmpty()) {
            sb.append("\n🏫 班级分布:\n");
            classDist.forEach((cls, arr) ->
                    sb.append(String.format("  %s: %d条(🔴%d🟠%d🟡%d)\n", cls, arr[0] + arr[1] + arr[2], arr[0], arr[1], arr[2])));
        }

        return sb.toString();
    }

    private long count(List<AlertRecord> list, String level) {
        return list.stream().filter(a -> level.equals(a.getAlertLevel())).count();
    }

    private long countByStatus(List<AlertRecord> list, String status) {
        return list.stream().filter(a -> status.equals(a.getStatus())).count();
    }

    private String buildTrend(long lastR, long nowR, long lastO, long nowO, long lastY, long nowY) {
        StringBuilder sb = new StringBuilder("📈 较上周: ");
        boolean changed = false;
        if (nowR != lastR) { sb.append("🔴").append(trendArrow(lastR, nowR)).append(Math.abs(nowR - lastR)).append(" "); changed = true; }
        if (nowO != lastO) { sb.append("🟠").append(trendArrow(lastO, nowO)).append(Math.abs(nowO - lastO)).append(" "); changed = true; }
        if (nowY != lastY) { sb.append("🟡").append(trendArrow(lastY, nowY)).append(Math.abs(nowY - lastY)); changed = true; }
        return changed ? sb.toString() : "";
    }

    private String trendArrow(long last, long now) {
        return now > last ? "↑" : "↓";
    }

    private String getWeekRange() {
        LocalDate mon = LocalDate.now().with(DayOfWeek.MONDAY);
        LocalDate sun = LocalDate.now().with(DayOfWeek.SUNDAY);
        return mon + " - " + sun;
    }

    private List<Long> getStudentIds(Long teacherId) {
        List<TeacherClass> tcList = teacherClassMapper.selectList(
                new LambdaQueryWrapper<TeacherClass>().eq(TeacherClass::getTeacherId, teacherId));
        if (tcList.isEmpty()) return Collections.emptyList();
        List<String> classNames = tcList.stream().map(TeacherClass::getClassName).distinct().toList();
        List<Student> students = studentMapper.selectList(
                new LambdaQueryWrapper<Student>().in(Student::getClassName, classNames));
        List<Long> ids = new ArrayList<>();
        for (Student s : students) {
            boolean inRange = tcList.stream().anyMatch(tc ->
                    tc.getStudentIdStart() != null && tc.getStudentIdEnd() != null
                            && s.getId() >= tc.getStudentIdStart() && s.getId() <= tc.getStudentIdEnd());
            if (tcList.stream().allMatch(tc -> tc.getStudentIdStart() == null) || inRange) {
                ids.add(s.getId());
            }
        }
        return ids;
    }
}
