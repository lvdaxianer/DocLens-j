package io.github.lvdaxianer.doclens.j.processing.domain;

import java.time.OffsetDateTime;
import java.util.Optional;

/**
 * LLM Markdown 配置访问器。
 *
 * @author lvdaxianerplus
 * @date 2026-06-12
 */
abstract class LlmMarkdownConfigAccessors {

    /**
     * 获取不可变配置状态。
     *
     * @return 不可变配置状态
     * @author lvdaxianerplus
     * @date 2026-06-12
     */
    abstract LlmMarkdownConfigState state();

    /**
     * 获取配置 ID。
     *
     * @return 配置 ID
     * @author lvdaxianerplus
     * @date 2026-06-12
     */
    public String id() {
        return state().id();
    }

    /**
     * 获取配置名称。
     *
     * @return 配置名称
     * @author lvdaxianerplus
     * @date 2026-06-12
     */
    public String name() {
        return state().name();
    }

    /**
     * 获取 API 协议类型。
     *
     * @return API 协议类型
     * @author lvdaxianerplus
     * @date 2026-06-12
     */
    public LlmMarkdownApiType apiType() {
        return state().apiType();
    }

    /**
     * 获取 LLM 接口地址。
     *
     * @return LLM 接口地址
     * @author lvdaxianerplus
     * @date 2026-06-12
     */
    public String url() {
        return state().url();
    }

    /**
     * 获取模型名称。
     *
     * @return 模型名称
     * @author lvdaxianerplus
     * @date 2026-06-12
     */
    public String model() {
        return state().model();
    }

    /**
     * 获取凭证引用。
     *
     * @return 凭证引用
     * @author lvdaxianerplus
     * @date 2026-06-12
     */
    public Optional<String> credentialRef() {
        return state().credentialRef();
    }

    /**
     * 判断凭证是否已配置。
     *
     * @return 凭证是否已配置
     * @author lvdaxianerplus
     * @date 2026-06-12
     */
    public boolean credentialConfigured() {
        return state().credentialConfigured();
    }

    /**
     * 获取配置用途。
     *
     * @return 配置用途
     * @author lvdaxianerplus
     * @date 2026-06-12
     */
    public LlmUsageType usageType() {
        return state().usageType();
    }

    /**
     * 获取优先级。
     *
     * @return 优先级
     * @author lvdaxianerplus
     * @date 2026-06-12
     */
    public int priority() {
        return state().priority();
    }

    /**
     * 获取 LLM 最大上下文 token 数。
     *
     * @return 最大上下文 token 数
     * @author lvdaxianerplus
     * @date 2026-06-13
     */
    public int maxContextTokens() {
        return state().maxContextTokens();
    }

    /**
     * 获取 LLM 最大并发数。
     *
     * @return 最大并发数
     * @author lvdaxianerplus
     * @date 2026-06-13
     */
    public int maxConcurrency() {
        return state().maxConcurrency();
    }

    /**
     * 获取 LLM 请求启动最小间隔毫秒数。
     *
     * @return 请求启动最小间隔毫秒数
     * @author lvdaxianerplus
     * @date 2026-06-13
     */
    public int requestIntervalMillis() {
        return state().requestIntervalMillis();
    }

    /**
     * 判断是否默认配置。
     *
     * @return 是否默认配置
     * @author lvdaxianerplus
     * @date 2026-06-12
     */
    public boolean defaultConfig() {
        return state().defaultConfig();
    }

    /**
     * 判断是否启用。
     *
     * @return 是否启用
     * @author lvdaxianerplus
     * @date 2026-06-12
     */
    public boolean enabled() {
        return state().enabled();
    }

    /**
     * 判断健康状态。
     *
     * @return 是否健康
     * @author lvdaxianerplus
     * @date 2026-06-12
     */
    public boolean healthy() {
        return state().healthy();
    }

    /**
     * 获取健康检查消息。
     *
     * @return 健康检查消息
     * @author lvdaxianerplus
     * @date 2026-06-12
     */
    public String healthMessage() {
        return state().healthMessage();
    }

    /**
     * 获取最近健康检查时间。
     *
     * @return 最近健康检查时间
     * @author lvdaxianerplus
     * @date 2026-06-12
     */
    public Optional<OffsetDateTime> lastHealthAt() {
        return state().lastHealthAt();
    }

    /**
     * 获取创建时间。
     *
     * @return 创建时间
     * @author lvdaxianerplus
     * @date 2026-06-12
     */
    public OffsetDateTime createdAt() {
        return state().createdAt();
    }

    /**
     * 获取更新时间。
     *
     * @return 更新时间
     * @author lvdaxianerplus
     * @date 2026-06-12
     */
    public OffsetDateTime updatedAt() {
        return state().updatedAt();
    }

    /**
     * 判断 URL 和模型是否已形成完整配置。
     *
     * @return 是否已配置 URL 和模型
     * @author lvdaxianerplus
     * @date 2026-06-12
     */
    public boolean isConfigured() {
        return !url().isBlank() && !model().isBlank();
    }

    /**
     * 判断是否可用于 LLM 后处理。
     *
     * @return 是否已配置且未暂停
     * @author lvdaxianerplus
     * @date 2026-06-12
     */
    public boolean isAvailableForPostProcessing() {
        return enabled() && isConfigured();
    }

    /**
     * 获取凭证文本。
     *
     * @return 凭证文本
     * @author lvdaxianerplus
     * @date 2026-06-09
     */
    public String credentialValue() {
        return credentialRef().orElse("");
    }
}
