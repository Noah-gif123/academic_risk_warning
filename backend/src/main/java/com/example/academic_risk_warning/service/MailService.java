package com.example.academic_risk_warning.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

/**
 * 邮件发送服务
 */
@Service
public class MailService {

    private static final Logger log = LoggerFactory.getLogger(MailService.class);

    private final JavaMailSender mailSender;

    @Value("${mail.enabled:false}")
    private boolean mailEnabled;

    @Value("${mail.from-name:学业风险预警系统}")
    private String fromName;

    @Value("${spring.mail.username:}")
    private String fromAddress;

    public MailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    /**
     * 发送 HTML 邮件
     *
     * @param to      收件人邮箱
     * @param subject 邮件主题
     * @param htmlBody HTML 正文
     */
    public void sendHtml(String to, String subject, String htmlBody) {
        if (!mailEnabled) {
            log.info("[MailService] 邮件功能未启用，跳过发送: {}", subject);
            return;
        }
        if (fromAddress == null || fromAddress.isBlank()) {
            log.warn("[MailService] 发件人未配置，跳过发送: {}", subject);
            return;
        }
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(fromAddress, fromName);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlBody, true);
            mailSender.send(message);
            log.info("[MailService] 邮件已发送: to={}, subject={}", to, subject);
        } catch (MessagingException | java.io.UnsupportedEncodingException e) {
            log.error("[MailService] 邮件发送失败: {}", e.getMessage(), e);
        }
    }

    /** 检查邮件功能是否可用 */
    public boolean isEnabled() {
        return mailEnabled && fromAddress != null && !fromAddress.isBlank();
    }
}
