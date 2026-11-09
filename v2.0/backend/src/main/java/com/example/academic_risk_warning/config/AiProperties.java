package com.example.academic_risk_warning.config;

import jakarta.annotation.PostConstruct;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 第三阶段 AI 配置（Dify 自托管 + DashScope 兼容模式）。
 *
 * <p>只放"指向与开关"，不放任何密钥明文以外的逻辑：API Key 建议用环境变量注入，
 * 例如 {@code AI_REPORT_APP_KEY=app-xxx}。</p>
 */
@Slf4j
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

    /** 预警解释卡工作流 App Key，未配置则走直连或模板 */
    private String alertExplainAppKey = "";

    /** 群体报告（教学班 / 行政班）工作流 App Key，未配置则走直连或模板 */
    private String groupReportAppKey = "";

    /** P3 Dify Diagnostician Agent App Key；未配置时 P3 parity 自动跳过 */
    private String diagnosticianAppKey = "";

    /** P3 Dify 自定义工具回调自研后端的服务令牌（header: X-Dify-Token） */
    private String difyToolToken = "";

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

    /**
     * 通用直连通道（任意 OpenAI 兼容端点：DashScope / DeepSeek / 本地 vLLM …）。
     * 留空则回退到下面的 dashscope-* 三个配置。
     */
    private String llmBaseUrl = "";

    private String llmApiKey = "";

    private String llmModel = "";

    /** 实际使用的直连地址 */
    public String directBaseUrl() {
        return notBlank(llmBaseUrl) ? llmBaseUrl : dashscopeBaseUrl;
    }

    /** 实际使用的直连 Key */
    public String directApiKey() {
        return notBlank(llmApiKey) ? llmApiKey : dashscopeKey;
    }

    /** 实际使用的直连模型 */
    public String directModel() {
        return notBlank(llmModel) ? llmModel : dashscopeModel;
    }

    /** 直连通道是否可用 */
    public boolean directReady() {
        return enabled && notBlank(directApiKey());
    }

    /**
     * 直连通道不可用时，给用户看的**可执行**提示：明确要设哪几个环境变量。
     *
     * <p>原来的文案写的是 {@code app.ai.llm-*}，但真实属性前缀是 {@code ai}、环境变量名是
     * {@code AI_LLM_*}，排障时会把人带偏，所以这里直接给出变量名与启动方式。</p>
     */
    public String directNotReadyHint() {
        if (!enabled) {
            return "AI 总开关是关的（ai.enabled=false / AI_ENABLED=false），打开后重试";
        }
        return "当前进程没有读到直连模型的密钥。两种办法："
                + "① 用 v2.0/backend/run-ai.ps1 启动后端（它会把 ai.env 里的密钥注入进程环境）；"
                + "② 在当前 shell 里先设置 AI_LLM_BASE_URL / AI_LLM_KEY / AI_LLM_MODEL（或 AI_DASHSCOPE_KEY）再启动。"
                + "设置后打开 /api/ai/health 确认 directReady=true。";
    }

    /** 直连通道缺的环境变量名（写日志/诊断用） */
    public String directMissingEnv() {
        if (notBlank(llmApiKey)) {
            return "";
        }
        if (notBlank(dashscopeKey)) {
            return "";
        }
        return "AI_LLM_KEY 或 AI_DASHSCOPE_KEY";
    }

    private static boolean notBlank(String s) {
        return s != null && !s.isBlank();
    }
    /** 直连通道是否可用（旧名，保留兼容） */
    public boolean dashscopeReady() {
        return enabled && dashscopeKey != null && !dashscopeKey.isBlank();
    }
    public boolean reportReady() {
        return enabled && reportAppKey != null && !reportAppKey.isBlank();
    }

    /** 启动时把 AI 通道状态打进日志：缺密钥不再要等用户点了才知道 */
    @PostConstruct
    void logChannelStatus() {
        if (!enabled) {
            log.warn("[AI] 总开关关闭（ai.enabled=false），全部 AI 入口将回退模板");
            return;
        }
        if (directReady()) {
            log.info("[AI] 直连通道就绪：{} / {}", directBaseUrl(), directModel());
        } else {
            log.warn("[AI] 直连通道未配置（缺 {}）：AI 出题 / 批改辅助 / 课程答疑不可用；"
                    + "报告等 Dify 场景不受影响。启动方式见 v2.0/backend/run-ai.ps1", directMissingEnv());
        }
        if (!reportReady()) {
            log.warn("[AI] 未配置 AI_REPORT_APP_KEY：学习评价报告将走直连或模板");
        }
    }

    /** 估算费用（元） */
    public double costOf(Integer tokens) {
        if (tokens == null || tokens <= 0) {
            return 0d;
        }
        return tokens / 1000.0 * yuanPerKToken;
    }
}
