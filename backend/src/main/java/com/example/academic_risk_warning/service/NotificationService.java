package com.example.academic_risk_warning.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.academic_risk_warning.entity.AlertRecord;
import com.example.academic_risk_warning.entity.Notification;
import com.example.academic_risk_warning.entity.Student;
import com.example.academic_risk_warning.entity.Teacher;
import com.example.academic_risk_warning.mapper.NotificationMapper;
import com.example.academic_risk_warning.mapper.StudentMapper;
import com.example.academic_risk_warning.mapper.TeacherClassMapper;
import com.example.academic_risk_warning.mapper.TeacherMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 消息通知服务
 */
@Service
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

    private final NotificationMapper notificationMapper;
    private final StudentMapper studentMapper;
    private final TeacherClassMapper teacherClassMapper;
    private final TeacherMapper teacherMapper;
    private final MailService mailService;

    public NotificationService(NotificationMapper notificationMapper,
                               StudentMapper studentMapper,
                               TeacherClassMapper teacherClassMapper,
                               TeacherMapper teacherMapper,
                               MailService mailService) {
        this.notificationMapper = notificationMapper;
        this.studentMapper = studentMapper;
        this.teacherClassMapper = teacherClassMapper;
        this.teacherMapper = teacherMapper;
        this.mailService = mailService;
    }

    /** 创建通知（含跳转目标），同步发送邮件 */
    public void create(Long recipientId, String recipientType, String title, String content,
                       Long alertId, String type, String targetType, Long targetId) {
        Notification n = new Notification();
        n.setRecipientId(recipientId);
        n.setRecipientType(recipientType);
        n.setTitle(title);
        n.setContent(content);
        n.setAlertId(alertId);
        n.setType(type);
        n.setTargetType(targetType);
        n.setTargetId(targetId);
        n.setChannel("IN_APP");
        n.setIsRead(false);
        n.setSendStatus("SENT");
        n.setSendTime(LocalDateTime.now());
        n.setCreateTime(LocalDateTime.now());
        notificationMapper.insert(n);

        // 同步发送邮件通知
        trySendEmail(recipientId, recipientType, title, content);
    }

    /** 尝试发送邮件（异步不阻塞主流程） */
    private void trySendEmail(Long recipientId, String recipientType, String title, String content) {
        if (!mailService.isEnabled()) return;
        String email = findRecipientEmail(recipientId, recipientType);
        if (email == null || email.isBlank()) {
            log.debug("[Notification] 收件人邮箱未绑定，跳过邮件发送: {}={}", recipientType, recipientId);
            return;
        }
        String htmlBody = buildEmailHtml(title, content);
        mailService.sendHtml(email, title, htmlBody);
    }

    /** 根据收件人ID和类型查找邮箱 */
    private String findRecipientEmail(Long recipientId, String recipientType) {
        if ("STUDENT".equals(recipientType)) {
            Student s = studentMapper.selectById(recipientId);
            return s != null ? s.getEmail() : null;
        } else if ("TEACHER".equals(recipientType)) {
            Teacher t = teacherMapper.selectById(recipientId);
            return t != null ? t.getEmail() : null;
        }
        return null;
    }

    /** 构建邮件 HTML */
    private String buildEmailHtml(String title, String content) {
        return """
            <div style="font-family:Arial,sans-serif;max-width:600px;margin:0 auto;padding:20px;
                        border:1px solid #e0e0e0;border-radius:8px;">
              <div style="background:#1a73e8;color:#fff;padding:16px;border-radius:8px 8px 0 0;">
                <h2 style="margin:0;">学业风险预警系统</h2>
              </div>
              <div style="padding:20px;line-height:1.8;color:#333;">
                <h3 style="margin-top:0;">%s</h3>
                <p>%s</p>
                <p style="color:#999;font-size:12px;margin-top:20px;">
                  此为系统自动发送的邮件，请勿回复。如需查看详情，请登录学业风险预警系统。
                </p>
              </div>
            </div>
            """.formatted(title, content);
    }

    /** 创建通知（无跳转目标，兼容旧调用） */
    public void create(Long recipientId, String recipientType, String title, String content,
                       Long alertId, String type) {
        create(recipientId, recipientType, title, content, alertId, type, null, null);
    }

    /** 预警生成时: 通知相关教师 + 学生 */
    public void notifyAlertGenerated(AlertRecord alert) {
        Student student = studentMapper.selectById(alert.getStudentId());
        String studentLabel = student != null
                ? student.getStudentName() + "(" + student.getStudentNo() + ")" : "学生" + alert.getStudentId();

        String levelText = switch (alert.getAlertLevel()) {
            case "RED" -> "红色";
            case "ORANGE" -> "橙色";
            case "YELLOW" -> "黄色";
            default -> alert.getAlertLevel();
        };
        String typeText = switch (alert.getAlertType()) {
            case "FAILURE" -> "挂科风险";
            case "HOMEWORK" -> "作业欠交";
            case "KNOWLEDGE" -> "知识点断层";
            case "ABSENTEEISM" -> "学习倦怠";
            case "DROP" -> "成绩骤降";
            case "CUMULATIVE" -> "累积风险";
            default -> alert.getAlertType();
        };

        String title = "新预警: " + studentLabel + "触发" + levelText + typeText;
        String content = String.format("%s在课程中触发%s级%s预警，综合风险分%.1f。%s",
                studentLabel, levelText, typeText,
                alert.getRiskScore() != null ? alert.getRiskScore() : 0,
                alert.getAlertReason() != null ? alert.getAlertReason() : "");

        // 通知负责教师
        Long teacherId = findTeacherForStudent(alert.getStudentId());
        if (teacherId != null) {
            create(teacherId, "TEACHER", title, content, alert.getId(), "ALERT_NEW", "ALERT", alert.getId());
        }

        // 通知学生
        create(alert.getStudentId(), "STUDENT", "学业预警通知: " + typeText, content, alert.getId(), "ALERT_NEW", "ALERT", alert.getId());
    }

    /** 教师处理后: 通知学生 */
    public void notifyAlertHandled(Long alertId, Long studentId, Long teacherId, String note) {
        Student student = studentMapper.selectById(studentId);
        String name = student != null ? student.getStudentName() : "同学";
        String title = "教师已处理你的预警";
        String content = "老师已处理你的学业预警" + (note != null && !note.isEmpty() ? "，处理备注：" + note : "。");
        create(studentId, "STUDENT", title, content, alertId, "HANDLE_RESULT", "ALERT", alertId);
    }

    /** 教师处理后(附带练习题): 通知学生 */
    public void notifyAlertHandledWithExercises(Long alertId, Long studentId, Long teacherId,
                                                 String note, int exerciseCount) {
        Student student = studentMapper.selectById(studentId);
        String name = student != null ? student.getStudentName() : "同学";
        String title = "教师已处理你的预警并布置了练习";
        String content = String.format(
                "老师已处理你的学业预警，并为你布置了%d道练习题，请在「我的练习」中查看并完成作答。%s",
                exerciseCount,
                note != null && !note.isEmpty() ? "处理备注：" + note : "");
        create(studentId, "STUDENT", title, content, alertId, "HANDLE_RESULT_EXERCISE", "ALERT", alertId);
    }

    /** 学生回应后: 通知教师 */
    public void notifyStudentResponded(Long alertId, Long studentId, Long teacherId, String response) {
        Student student = studentMapper.selectById(studentId);
        String name = student != null ? student.getStudentName() : "学生";
        create(teacherId, "TEACHER", name + "已回应您的处理意见",
                name + "对预警的回应：" + response, alertId, "STUDENT_RESPOND", "ALERT", alertId);
    }

    /** 获取未读数量 */
    public long getUnreadCount(Long userId, String userType) {
        return notificationMapper.selectCount(
                new LambdaQueryWrapper<Notification>()
                        .eq(Notification::getRecipientId, userId)
                        .eq(Notification::getRecipientType, userType)
                        .eq(Notification::getIsRead, false));
    }

    /** 获取通知列表(最近50条) */
    public List<Notification> getList(Long userId, String userType) {
        Page<Notification> p = new Page<>(1, 50);
        Page<Notification> result = notificationMapper.selectPage(p,
                new LambdaQueryWrapper<Notification>()
                        .eq(Notification::getRecipientId, userId)
                        .eq(Notification::getRecipientType, userType)
                        .orderByDesc(Notification::getCreateTime));
        return result.getRecords();
    }

    /** 标记已读 */
    public void markRead(Long notificationId) {
        Notification n = notificationMapper.selectById(notificationId);
        if (n != null && Boolean.FALSE.equals(n.getIsRead())) {
            n.setIsRead(true);
            n.setReadTime(LocalDateTime.now());
            notificationMapper.updateById(n);
        }
    }

    /** 全部已读 */
    public void markAllRead(Long userId, String userType) {
        List<Notification> list = notificationMapper.selectList(
                new LambdaQueryWrapper<Notification>()
                        .eq(Notification::getRecipientId, userId)
                        .eq(Notification::getRecipientType, userType)
                        .eq(Notification::getIsRead, false));
        for (Notification n : list) {
            n.setIsRead(true);
            n.setReadTime(LocalDateTime.now());
            notificationMapper.updateById(n);
        }
    }

    public NotificationMapper getMapper() { return notificationMapper; }

    /** 根据学生ID查找负责教师 */
    private Long findTeacherForStudent(Long studentId) {
        Student student = studentMapper.selectById(studentId);
        if (student == null || student.getClassName() == null) return null;

        var list = teacherClassMapper.selectList(
                new LambdaQueryWrapper<com.example.academic_risk_warning.entity.TeacherClass>()
                        .eq(com.example.academic_risk_warning.entity.TeacherClass::getClassName, student.getClassName()));
        for (var tc : list) {
            if (tc.getStudentIdStart() != null && tc.getStudentIdEnd() != null
                    && studentId >= tc.getStudentIdStart() && studentId <= tc.getStudentIdEnd()) {
                return tc.getTeacherId();
            }
        }
        return list.isEmpty() ? null : list.get(0).getTeacherId();
    }
}
