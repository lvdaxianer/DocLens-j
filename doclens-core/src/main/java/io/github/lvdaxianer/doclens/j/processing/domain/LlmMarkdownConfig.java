package io.github.lvdaxianer.doclens.j.processing.domain;

import java.time.OffsetDateTime;
import java.util.Optional;

/**
 * LLM Markdown 后处理配置。
 *
 * @author lvdaxianerplus
 * @date 2026-06-09
 */
public final class LlmMarkdownConfig extends LlmMarkdownConfigAccessors {

    public static final String SINGLETON_ID = "default";
    static final String DEFAULT_NAME = "默认 LLM 配置";
    static final int DEFAULT_PRIORITY = 100;
    public static final int DEFAULT_MAX_CONTEXT_TOKENS = 16000;
    public static final int DEFAULT_MAX_CONCURRENCY = 1;
    public static final int DEFAULT_REQUEST_INTERVAL_MILLIS = 1000;

    private final LlmMarkdownConfigState state;

    /**
     * 从状态对象创建配置。
     *
     * @param state 配置状态
     * @author lvdaxianerplus
     * @date 2026-06-12
     */
    LlmMarkdownConfig(LlmMarkdownConfigState state) {
        this.state = state;
    }

    /**
     * 获取不可变配置状态。
     *
     * @return 不可变配置状态
     * @author lvdaxianerplus
     * @date 2026-06-12
     */
    @Override
    LlmMarkdownConfigState state() {
        return state;
    }

    /**
     * 创建已配置的 LLM Markdown 配置。
     *
     * @param id 配置 ID
     * @param url OpenAI compatible 接口地址
     * @param model 模型名称
     * @param credentialRef API Key 凭证引用
     * @return LLM Markdown 配置
     * @author lvdaxianerplus
     * @date 2026-06-09
     */
    public static LlmMarkdownConfig configured(String id, String url, String model, String credentialRef) {
        return configured(id, LlmMarkdownApiType.OPENAI, url, model, credentialRef);
    }

    /**
     * 创建已配置的 LLM Markdown 配置。
     *
     * @param id 配置 ID
     * @param apiType API 协议类型
     * @param url LLM 接口地址
     * @param model 模型名称
     * @param credentialRef API Key 凭证引用
     * @return LLM Markdown 配置
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    public static LlmMarkdownConfig configured(
            String id,
            LlmMarkdownApiType apiType,
            String url,
            String model,
            String credentialRef
    ) {
        OffsetDateTime now = OffsetDateTime.now();
        return builder(id, DEFAULT_NAME, apiType == null ? LlmMarkdownApiType.OPENAI : apiType)
                .endpoint(required(url, "llm markdown url is required"),
                        required(model, "llm markdown model is required"))
                .credential(credentialRef)
                .usage(LlmUsageType.MARKDOWN_POST_PROCESSING, DEFAULT_PRIORITY)
                .runtimeLimits(DEFAULT_MAX_CONTEXT_TOKENS, DEFAULT_MAX_CONCURRENCY, DEFAULT_REQUEST_INTERVAL_MILLIS)
                .defaultConfig(true)
                .enabled(true)
                .healthy(false)
                .healthMessage("")
                .lastHealthAt(Optional.empty())
                .createdAt(now)
                .updatedAt(now)
                .build();
    }

    /**
     * 创建未配置的响应视图。
     *
     * @return 未配置 LLM Markdown 配置
     * @author lvdaxianerplus
     * @date 2026-06-09
     */
    public static LlmMarkdownConfig unconfigured() {
        OffsetDateTime now = OffsetDateTime.now();
        return builder(SINGLETON_ID, DEFAULT_NAME, LlmMarkdownApiType.OPENAI)
                .endpoint("", "")
                .credential("")
                .usage(LlmUsageType.MARKDOWN_POST_PROCESSING, DEFAULT_PRIORITY)
                .runtimeLimits(DEFAULT_MAX_CONTEXT_TOKENS, DEFAULT_MAX_CONCURRENCY, DEFAULT_REQUEST_INTERVAL_MILLIS)
                .defaultConfig(true)
                .enabled(true)
                .healthy(false)
                .healthMessage("")
                .lastHealthAt(Optional.empty())
                .createdAt(now)
                .updatedAt(now)
                .build();
    }

    /**
     * 创建 LLM 配置构建器。
     *
     * @param id 配置 ID
     * @param name 配置名称
     * @param apiType API 协议类型
     * @return 配置构建器
     * @author lvdaxianerplus
     * @date 2026-06-12
     */
    public static LlmMarkdownConfigBuilder builder(String id, String name, LlmMarkdownApiType apiType) {
        return new LlmMarkdownConfigBuilder(id, name, apiType);
    }

    /**
     * 更新 LLM 健康状态。
     *
     * @param healthy 是否健康
     * @param healthMessage 健康检查消息
     * @param lastHealthAt 最近健康检查时间
     * @return 更新后的配置
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    public LlmMarkdownConfig updateHealth(boolean healthy, String healthMessage, OffsetDateTime lastHealthAt) {
        return copyBuilder().healthy(healthy).healthMessage(healthMessage)
                .lastHealthAt(Optional.ofNullable(lastHealthAt)).updatedAt(OffsetDateTime.now()).build();
    }

    /**
     * 更新 LLM Markdown 后处理启停状态。
     *
     * @param enabled 是否启用 LLM Markdown 后处理
     * @return 更新后的配置
     * @author lvdaxianerplus
     * @date 2026-06-12
     */
    public LlmMarkdownConfig withEnabled(boolean enabled) {
        return copyBuilder().enabled(enabled).updatedAt(OffsetDateTime.now()).build();
    }

    /**
     * 更新默认配置标记。
     *
     * @param defaultConfig 是否默认配置
     * @return 更新后的配置
     * @author lvdaxianerplus
     * @date 2026-06-12
     */
    public LlmMarkdownConfig withDefaultConfig(boolean defaultConfig) {
        return copyBuilder().defaultConfig(defaultConfig).updatedAt(OffsetDateTime.now()).build();
    }

    /**
     * 复制当前配置到构建器。
     *
     * @return 预填充构建器
     * @author lvdaxianerplus
     * @date 2026-06-12
     */
    private LlmMarkdownConfigBuilder copyBuilder() {
        return builder(id(), name(), apiType()).endpoint(url(), model()).credential(credentialValue())
                .usage(usageType(), priority())
                .runtimeLimits(maxContextTokens(), maxConcurrency(), requestIntervalMillis())
                .defaultConfig(defaultConfig()).enabled(enabled()).healthy(healthy())
                .healthMessage(healthMessage()).lastHealthAt(lastHealthAt()).createdAt(createdAt())
                .updatedAt(updatedAt());
    }

    /**
     * 校验并规整必填文本。
     *
     * @param value 原始文本
     * @param message 异常消息
     * @return 规整文本
     * @author lvdaxianerplus
     * @date 2026-06-09
     */
    private static String required(String value, String message) {
        String normalized = normalize(value);
        // 必填文本非空时返回去首尾空格后的值。
        if (!normalized.isBlank()) {
            return normalized;
        } else {
            // 必填文本为空时立即失败，避免保存无效配置。
            throw new IllegalArgumentException(message);
        }
    }

    /**
     * 标准化可选文本。
     *
     * @param value 原始文本
     * @return 标准化文本
     * @author lvdaxianerplus
     * @date 2026-06-09
     */
    static String normalize(String value) {
        return value == null ? "" : value.trim();
    }
}
