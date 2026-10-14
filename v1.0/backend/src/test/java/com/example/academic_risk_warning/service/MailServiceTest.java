package com.example.academic_risk_warning.service;

import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * MailService 单元测试
 * <p>
 * 使用 Mockito 模拟 JavaMailSender，验证邮件发送逻辑的正确性，
 * 包括开关控制、发件人配置校验、异常处理等。
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("MailService 单元测试")
class MailServiceTest {

    @Mock
    private JavaMailSender mailSender;

    private MailService mailService;

    @BeforeEach
    void setUp() {
        mailService = new MailService(mailSender);
    }

    // ==================== 邮件开关控制 ====================

    @Nested
    @DisplayName("mailEnabled 开关控制")
    class MailEnabled {

        @Test
        @DisplayName("mailEnabled=false → 不发送邮件，不调用 mailSender")
        void shouldNotSendWhenDisabled() {
            ReflectionTestUtils.setField(mailService, "mailEnabled", false);
            ReflectionTestUtils.setField(mailService, "fromAddress", "admin@test.com");

            mailService.sendHtml("to@test.com", "Subject", "<p>Body</p>");

            verify(mailSender, never()).createMimeMessage();
            verify(mailSender, never()).send(any(MimeMessage.class));
        }

        @Test
        @DisplayName("mailEnabled=true + 发件人已配置 → 发送邮件")
        void shouldSendWhenEnabled() {
            ReflectionTestUtils.setField(mailService, "mailEnabled", true);
            ReflectionTestUtils.setField(mailService, "fromAddress", "admin@test.com");
            ReflectionTestUtils.setField(mailService, "fromName", "学业预警");

            MimeMessage mimeMessage = mock(MimeMessage.class);
            when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

            mailService.sendHtml("student@test.com", "预警通知", "<p>您有新的预警</p>");

            verify(mailSender).createMimeMessage();
            verify(mailSender).send(any(MimeMessage.class));
        }
    }

    @Nested
    @DisplayName("发件人地址校验")
    class FromAddress {

        @Test
        @DisplayName("fromAddress 为 null → 不发送")
        void shouldNotSendWhenFromAddressIsNull() {
            ReflectionTestUtils.setField(mailService, "mailEnabled", true);
            ReflectionTestUtils.setField(mailService, "fromAddress", null);

            mailService.sendHtml("to@test.com", "Subject", "Body");

            verify(mailSender, never()).createMimeMessage();
        }

        @Test
        @DisplayName("fromAddress 为空字符串 → 不发送")
        void shouldNotSendWhenFromAddressIsBlank() {
            ReflectionTestUtils.setField(mailService, "mailEnabled", true);
            ReflectionTestUtils.setField(mailService, "fromAddress", "");

            mailService.sendHtml("to@test.com", "Subject", "Body");

            verify(mailSender, never()).createMimeMessage();
        }
    }

    @Nested
    @DisplayName("isEnabled — 检查邮件功能是否可用")
    class IsEnabled {

        @Test
        @DisplayName("mailEnabled=true + fromAddress 有效 → true")
        void shouldReturnTrueWhenFullyConfigured() {
            ReflectionTestUtils.setField(mailService, "mailEnabled", true);
            ReflectionTestUtils.setField(mailService, "fromAddress", "admin@test.com");
            assertTrue(mailService.isEnabled());
        }

        @Test
        @DisplayName("mailEnabled=false → false")
        void shouldReturnFalseWhenDisabled() {
            ReflectionTestUtils.setField(mailService, "mailEnabled", false);
            ReflectionTestUtils.setField(mailService, "fromAddress", "admin@test.com");
            assertFalse(mailService.isEnabled());
        }

        @Test
        @DisplayName("fromAddress 为 null → false")
        void shouldReturnFalseWhenFromAddressIsNull() {
            ReflectionTestUtils.setField(mailService, "mailEnabled", true);
            ReflectionTestUtils.setField(mailService, "fromAddress", null);
            assertFalse(mailService.isEnabled());
        }
    }

    @Nested
    @DisplayName("异常容错")
    class ExceptionHandling {

        @Test
        @DisplayName("JavaMailSender 抛异常 → 不对外抛出，静默处理")
        void shouldNotPropagateExceptions() {
            ReflectionTestUtils.setField(mailService, "mailEnabled", true);
            ReflectionTestUtils.setField(mailService, "fromAddress", "admin@test.com");
            ReflectionTestUtils.setField(mailService, "fromName", "测试");

            when(mailSender.createMimeMessage()).thenThrow(new RuntimeException("连接失败"));

            // 不应抛出异常
            assertDoesNotThrow(() ->
                mailService.sendHtml("to@test.com", "Subject", "Body")
            );
        }
    }
}
