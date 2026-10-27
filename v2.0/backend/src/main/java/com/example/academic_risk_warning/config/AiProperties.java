package com.example.academic_risk_warning.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 第三阶段 AI 配置（Dify 自托管 + DashScope 兼容模式）。
 *
 * <p>只放"指向与开关"，不放任何密钥明文以外的逻辑：API Key 建议用环境变量注入，
 * 例如 {@code AI_REPORT_APP_KEY=app-xxx}。</p>
 */
@Data
@Component
@ConfigurationProperties(prefix = "ai")
public class AiProperties {

    /** 总开关：false 时所有 AI 入口回退模板，且不再调用 Dify */
    private boolean enabled = true;

    /** Dify 自托管地址（默认 8090，避开 80/443） */
    private String baseUrl = "http://localhost:8090";

    /** 学习评价报告工作流的 App API Key */
    private String reportAppKey = "";

    /** AI 出题工作流的 App API Key（M2 使用） */
    private String questionAppKey = "";

    /** 课程问答 App API Key（M4 使用） */
    private String chatAppKey = "";

    /** 单次调用超时（秒） */
    private int timeoutSeconds = 120;

    /** 每个用户每日调用上限（防跑飞） */
    private int dailyLimit = 30;

    /** 模型名（仅用于记录与计费估算） */
    private String model = "qwen-plus";

    /** 计费单价：元 / 千 token（默认按 qwen-plus 混合价估算） */
    private double yuanPerKToken = 0.0015;

    /** 工作流版本（写进 ai_run.prompt_version 与 ai_prompt_template） */
    private String reportPromptVersion = "report-student-v1";

    /** 直连通道：DashScope（OpenAI 兼容）。Dify 工作流未配置时的备用模型通道 */
    private String dashscopeKey = "";

    private String dashscopeBaseUrl = "https://dashscope.aliyuncs.com/compatible-mode/v1";

    private String dashscopeModel = "qwen-plus";

    /** 直连通道是否可用 */
    public boolean dashscopeReady() {
        return enabled && dashscopeKey != null && !dashscopeKey.isBlank();
    }
    public boolean reportReady() {
        return enabled && reportAppKey != null && !reportAppKey.isBlank();
    }

    /** 估算费用（元） */
    public double costOf(Integer tokens) {
        if (tokens == null || tokens <= 0) {
            return 0d;
        }
        return tokens / 1000.0 * yuanPerKToken;
    }
}
