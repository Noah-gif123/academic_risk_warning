package com.example.academic_risk_warning.llm;

import com.example.academic_risk_warning.config.AgentProperties;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 百炼 Chat 客户端配置生效测试：agent.enabled 关闭、Key 未配置都应快速失败，
 * 且不会再发出 HTTP 请求（此前 agent.enabled 是"死配置"）。
 */
@DisplayName("百炼 Chat 客户端配置开关测试")
class BailianChatClientTest {

    @Test
    @DisplayName("agent.enabled=false → 直接抛异常，不发起调用")
    void shouldRejectWhenAgentDisabled() {
        AgentProperties properties = new AgentProperties();
        properties.setEnabled(false);
        BailianChatClient client = new BailianChatClient(properties);
        ReflectionTestUtils.setField(client, "apiKey", "sk-test");

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> client.chat("system", "user"));
        assertTrue(ex.getMessage().contains("agent.enabled=false"), ex.getMessage());
    }

    @Test
    @DisplayName("未配置 API Key → 抛出明确提示")
    void shouldRejectWhenApiKeyMissing() {
        AgentProperties properties = new AgentProperties();
        properties.setEnabled(true);
        BailianChatClient client = new BailianChatClient(properties);
        ReflectionTestUtils.setField(client, "apiKey", "");

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> client.chat("system", "user"));
        assertTrue(ex.getMessage().contains("API Key"), ex.getMessage());
    }
}
