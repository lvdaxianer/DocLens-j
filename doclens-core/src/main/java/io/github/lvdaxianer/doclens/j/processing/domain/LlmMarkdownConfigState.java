package io.github.lvdaxianer.doclens.j.processing.domain;

import java.time.OffsetDateTime;
import java.util.Optional;

/**
 * LLM Markdown 配置不可变状态。
 *
 * @author lvdaxianerplus
 * @date 2026-06-12
 */
final class LlmMarkdownConfigState {

    private final String id;
    private final String name;
    private final LlmMarkdownApiType apiType;
    private final String url;
    private final String model;
    private final Optional<String> credentialRef;
    private final boolean credentialConfigured;
    private final LlmUsageType usageType;
    private final int priority;
    private final int maxContextTokens;
    private final int maxConcurrency;
    private final int requestIntervalMillis;
    private final boolean defaultConfig;
    private final boolean enabled;
    private final boolean healthy;
    private final String healthMessage;
    private final Optional<OffsetDateTime> lastHealthAt;
    private final OffsetDateTime createdAt;
    private final OffsetDateTime updatedAt;

    /**
     * 从构建器固化状态。
     *
     * @param builder 配置构建器
     * @author lvdaxianerplus
     * @date 2026-06-12
     */
    LlmMarkdownConfigState(LlmMarkdownConfigBuilder builder) {
        this.id = builder.id;
        this.name = builder.name;
        this.apiType = builder.apiType;
        this.url = builder.url;
        this.model = builder.model;
        this.credentialRef = Optional.ofNullable(LlmMarkdownConfigBuilder.blankToNull(builder.credentialRef));
        this.credentialConfigured = builder.credentialConfigured;
        this.usageType = builder.usageType;
        this.priority = builder.priority;
        this.maxContextTokens = builder.maxContextTokens;
        this.maxConcurrency = builder.maxConcurrency;
        this.requestIntervalMillis = builder.requestIntervalMillis;
        this.defaultConfig = builder.defaultConfig;
        this.enabled = builder.enabled;
        this.healthy = builder.healthy;
        this.healthMessage = LlmMarkdownConfig.normalize(builder.healthMessage);
        this.lastHealthAt = builder.lastHealthAt;
        this.createdAt = builder.createdAt;
        this.updatedAt = builder.updatedAt;
    }

    /**
     * 获取配置 ID。
     *
     * @return 配置 ID
     * @author lvdaxianerplus
     * @date 2026-06-12
     */
    String id() {
        return id;
    }

    /**
     * 获取配置名称。
     *
     * @return 配置名称
     * @author lvdaxianerplus
     * @date 2026-06-12
     */
    String name() {
        return name;
    }

    /**
     * 获取 API 协议类型。
     *
     * @return API 协议类型
     * @author lvdaxianerplus
     * @date 2026-06-12
     */
    LlmMarkdownApiType apiType() {
        return apiType;
    }

    /**
     * 获取 LLM 接口地址。
     *
     * @return LLM 接口地址
     * @author lvdaxianerplus
     * @date 2026-06-12
     */
    String url() {
        return url;
    }

    /**
     * 获取模型名称。
     *
     * @return 模型名称
     * @author lvdaxianerplus
     * @date 2026-06-12
     */
    String model() {
        return model;
    }

    /**
     * 获取凭证引用。
     *
     * @return 凭证引用
     * @author lvdaxianerplus
     * @date 2026-06-12
     */
    Optional<String> credentialRef() {
        return credentialRef;
    }

    /**
     * 判断凭证是否已配置。
     *
     * @return 凭证是否已配置
     * @author lvdaxianerplus
     * @date 2026-06-12
     */
    boolean credentialConfigured() {
        return credentialConfigured;
    }

    /**
     * 获取配置用途。
     *
     * @return 配置用途
     * @author lvdaxianerplus
     * @date 2026-06-12
     */
    LlmUsageType usageType() {
        return usageType;
    }

    /**
     * 获取优先级。
     *
     * @return 优先级
     * @author lvdaxianerplus
     * @date 2026-06-12
     */
    int priority() {
        return priority;
    }

    /**
     * 获取最大上下文 token 数。
     *
     * @return 最大上下文 token 数
     * @author lvdaxianerplus
     * @date 2026-06-13
     */
    int maxContextTokens() {
        return maxContextTokens;
    }

    /**
     * 获取最大并发数。
     *
     * @return 最大并发数
     * @author lvdaxianerplus
     * @date 2026-06-13
     */
    int maxConcurrency() {
        return maxConcurrency;
    }

    /**
     * 获取请求启动最小间隔毫秒数。
     *
     * @return 请求启动最小间隔毫秒数
     * @author lvdaxianerplus
     * @date 2026-06-13
     */
    int requestIntervalMillis() {
        return requestIntervalMillis;
    }

    /**
     * 判断是否默认配置。
     *
     * @return 是否默认配置
     * @author lvdaxianerplus
     * @date 2026-06-12
     */
    boolean defaultConfig() {
        return defaultConfig;
    }

    /**
     * 判断是否启用。
     *
     * @return 是否启用
     * @author lvdaxianerplus
     * @date 2026-06-12
     */
    boolean enabled() {
        return enabled;
    }

    /**
     * 判断是否健康。
     *
     * @return 是否健康
     * @author lvdaxianerplus
     * @date 2026-06-12
     */
    boolean healthy() {
        return healthy;
    }

    /**
     * 获取健康检查消息。
     *
     * @return 健康检查消息
     * @author lvdaxianerplus
     * @date 2026-06-12
     */
    String healthMessage() {
        return healthMessage;
    }

    /**
     * 获取最近健康检查时间。
     *
     * @return 最近健康检查时间
     * @author lvdaxianerplus
     * @date 2026-06-12
     */
    Optional<OffsetDateTime> lastHealthAt() {
        return lastHealthAt;
    }

    /**
     * 获取创建时间。
     *
     * @return 创建时间
     * @author lvdaxianerplus
     * @date 2026-06-12
     */
    OffsetDateTime createdAt() {
        return createdAt;
    }

    /**
     * 获取更新时间。
     *
     * @return 更新时间
     * @author lvdaxianerplus
     * @date 2026-06-12
     */
    OffsetDateTime updatedAt() {
        return updatedAt;
    }
}
