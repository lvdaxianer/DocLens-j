package io.github.lvdaxianer.doclens.j.processing.domain;

import java.time.OffsetDateTime;
import java.util.Optional;

/**
 * LLM Markdown 配置构建器。
 *
 * @author lvdaxianerplus
 * @date 2026-06-12
 */
public final class LlmMarkdownConfigBuilder {

    final String id;
    final String name;
    final LlmMarkdownApiType apiType;
    String url = "";
    String model = "";
    String credentialRef = "";
    boolean credentialConfigured;
    LlmUsageType usageType = LlmUsageType.MARKDOWN_POST_PROCESSING;
    int priority = LlmMarkdownConfig.DEFAULT_PRIORITY;
    int maxContextTokens = LlmMarkdownConfig.DEFAULT_MAX_CONTEXT_TOKENS;
    int maxConcurrency = LlmMarkdownConfig.DEFAULT_MAX_CONCURRENCY;
    int requestIntervalMillis = LlmMarkdownConfig.DEFAULT_REQUEST_INTERVAL_MILLIS;
    boolean defaultConfig = true;
    boolean enabled = true;
    boolean healthy;
    String healthMessage = "";
    Optional<OffsetDateTime> lastHealthAt = Optional.empty();
    OffsetDateTime createdAt = OffsetDateTime.now();
    OffsetDateTime updatedAt = createdAt;

    /**
     * 创建基础配置构建器。
     *
     * @param id 配置 ID
     * @param name 配置名称
     * @param apiType API 协议类型
     * @author lvdaxianerplus
     * @date 2026-06-12
     */
    LlmMarkdownConfigBuilder(String id, String name, LlmMarkdownApiType apiType) {
        this.id = id;
        this.name = name;
        this.apiType = apiType;
    }

    /**
     * 设置接口地址和模型。
     *
     * @param url LLM 接口地址
     * @param model 模型名称
     * @return 当前构建器
     * @author lvdaxianerplus
     * @date 2026-06-12
     */
    public LlmMarkdownConfigBuilder endpoint(String url, String model) {
        this.url = url;
        this.model = model;
        return this;
    }

    /**
     * 设置凭证文本。
     *
     * @param credentialRef 凭证文本
     * @return 当前构建器
     * @author lvdaxianerplus
     * @date 2026-06-12
     */
    public LlmMarkdownConfigBuilder credential(String credentialRef) {
        String normalized = LlmMarkdownConfig.normalize(credentialRef);
        this.credentialRef = normalized;
        this.credentialConfigured = !normalized.isBlank();
        return this;
    }

    /**
     * 设置用途和优先级。
     *
     * @param usageType 配置用途
     * @param priority 优先级
     * @return 当前构建器
     * @author lvdaxianerplus
     * @date 2026-06-12
     */
    public LlmMarkdownConfigBuilder usage(LlmUsageType usageType, int priority) {
        this.usageType = usageType;
        this.priority = priority;
        return this;
    }

    /**
     * 设置 LLM 调用治理参数。
     *
     * @param maxContextTokens 最大上下文 token 数
     * @param maxConcurrency 最大并发数
     * @param requestIntervalMillis 请求启动最小间隔毫秒
     * @return 当前构建器
     * @author lvdaxianerplus
     * @date 2026-06-13
     */
    public LlmMarkdownConfigBuilder runtimeLimits(
            int maxContextTokens,
            int maxConcurrency,
            int requestIntervalMillis
    ) {
        this.maxContextTokens = maxContextTokens;
        this.maxConcurrency = maxConcurrency;
        this.requestIntervalMillis = requestIntervalMillis;
        return this;
    }

    /**
     * 设置是否默认配置。
     *
     * @param defaultConfig 是否默认配置
     * @return 当前构建器
     * @author lvdaxianerplus
     * @date 2026-06-12
     */
    public LlmMarkdownConfigBuilder defaultConfig(boolean defaultConfig) {
        this.defaultConfig = defaultConfig;
        return this;
    }

    /**
     * 设置启停状态。
     *
     * @param enabled 是否启用
     * @return 当前构建器
     * @author lvdaxianerplus
     * @date 2026-06-12
     */
    public LlmMarkdownConfigBuilder enabled(boolean enabled) {
        this.enabled = enabled;
        return this;
    }

    /**
     * 设置健康状态。
     *
     * @param healthy 是否健康
     * @return 当前构建器
     * @author lvdaxianerplus
     * @date 2026-06-12
     */
    public LlmMarkdownConfigBuilder healthy(boolean healthy) {
        this.healthy = healthy;
        return this;
    }

    /**
     * 设置健康检查消息。
     *
     * @param healthMessage 健康检查消息
     * @return 当前构建器
     * @author lvdaxianerplus
     * @date 2026-06-12
     */
    public LlmMarkdownConfigBuilder healthMessage(String healthMessage) {
        this.healthMessage = healthMessage;
        return this;
    }

    /**
     * 设置最近健康检查时间。
     *
     * @param lastHealthAt 最近健康检查时间
     * @return 当前构建器
     * @author lvdaxianerplus
     * @date 2026-06-12
     */
    public LlmMarkdownConfigBuilder lastHealthAt(Optional<OffsetDateTime> lastHealthAt) {
        this.lastHealthAt = lastHealthAt;
        return this;
    }

    /**
     * 设置创建时间。
     *
     * @param createdAt 创建时间
     * @return 当前构建器
     * @author lvdaxianerplus
     * @date 2026-06-12
     */
    public LlmMarkdownConfigBuilder createdAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
        return this;
    }

    /**
     * 设置更新时间。
     *
     * @param updatedAt 更新时间
     * @return 当前构建器
     * @author lvdaxianerplus
     * @date 2026-06-12
     */
    public LlmMarkdownConfigBuilder updatedAt(OffsetDateTime updatedAt) {
        this.updatedAt = updatedAt;
        return this;
    }

    /**
     * 构建 LLM Markdown 配置。
     *
     * @return LLM Markdown 配置
     * @author lvdaxianerplus
     * @date 2026-06-12
     */
    public LlmMarkdownConfig build() {
        return new LlmMarkdownConfig(state());
    }

    /**
     * 构建不可变配置状态。
     *
     * @return 不可变配置状态
     * @author lvdaxianerplus
     * @date 2026-06-12
     */
    private LlmMarkdownConfigState state() {
        return new LlmMarkdownConfigState(this);
    }

    /**
     * 空文本转空引用。
     *
     * @param value 文本
     * @return 非空文本或空引用
     * @author lvdaxianerplus
     * @date 2026-06-12
     */
    static String blankToNull(String value) {
        return value.isBlank() ? null : value;
    }
}
