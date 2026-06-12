package io.github.lvdaxianer.doclens.j.processing.interfaces;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.github.lvdaxianer.doclens.j.processing.domain.LlmMarkdownConfig;

/**
 * LLM Markdown 配置响应。
 *
 * @author lvdaxianerplus
 * @date 2026-06-09
 */
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class LlmMarkdownConfigResponse {

    /** 配置 ID。 */
    private String id;
    /** 配置名称。 */
    private String name;
    /** API 协议类型。 */
    @JsonProperty("api_type")
    private String apiType;
    /** OpenAI compatible 接口地址。 */
    private String url;
    /** 模型名称。 */
    private String model;
    /** 是否已配置 API Key。 */
    @JsonProperty("credential_configured")
    private boolean credentialConfigured;
    /** 配置用途。 */
    @JsonProperty("usage_type")
    private String usageType;
    /** 优先级。 */
    private int priority;
    /** 是否默认配置。 */
    @JsonProperty("is_default")
    private boolean defaultConfig;
    /** 是否启用 LLM Markdown 后处理。 */
    private boolean enabled;
    /** 健康状态。 */
    private boolean healthy;
    /** 健康检查消息。 */
    @JsonProperty("health_message")
    private String healthMessage;
    /** 最近健康检查时间。 */
    @JsonProperty("last_health_at")
    private String lastHealthAt;

    /**
     * 从领域配置创建响应。
     *
     * @param config LLM Markdown 配置
     * @return LLM Markdown 配置响应
     * @author lvdaxianerplus
     * @date 2026-06-09
     */
    public static LlmMarkdownConfigResponse from(LlmMarkdownConfig config) {
        LlmMarkdownConfigResponse response = new LlmMarkdownConfigResponse();
        response.fillIdentity(config);
        response.fillGovernance(config);
        response.fillHealth(config);
        return response;
    }

    /**
     * 填充配置身份字段。
     *
     * @param config LLM Markdown 配置
     * @author lvdaxianerplus
     * @date 2026-06-12
     */
    private void fillIdentity(LlmMarkdownConfig config) {
        id = config.id();
        name = config.name();
        apiType = config.apiType().value();
        url = config.url();
        model = config.model();
        credentialConfigured = config.credentialConfigured();
    }

    /**
     * 填充配置治理字段。
     *
     * @param config LLM Markdown 配置
     * @author lvdaxianerplus
     * @date 2026-06-12
     */
    private void fillGovernance(LlmMarkdownConfig config) {
        usageType = config.usageType().name();
        priority = config.priority();
        defaultConfig = config.defaultConfig();
        enabled = config.enabled();
    }

    /**
     * 填充配置健康字段。
     *
     * @param config LLM Markdown 配置
     * @author lvdaxianerplus
     * @date 2026-06-12
     */
    private void fillHealth(LlmMarkdownConfig config) {
        healthy = config.healthy();
        healthMessage = config.healthMessage();
        lastHealthAt = config.lastHealthAt().map(java.time.OffsetDateTime::toString).orElse("");
    }
}
