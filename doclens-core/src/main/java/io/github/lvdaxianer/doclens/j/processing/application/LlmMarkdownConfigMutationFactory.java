package io.github.lvdaxianer.doclens.j.processing.application;

import io.github.lvdaxianer.doclens.j.processing.domain.LlmMarkdownApiType;
import io.github.lvdaxianer.doclens.j.processing.domain.LlmMarkdownConfigBuilder;
import io.github.lvdaxianer.doclens.j.processing.domain.LlmMarkdownConfig;
import io.github.lvdaxianer.doclens.j.processing.domain.LlmUsageType;
import java.net.URI;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

/**
 * LLM Markdown 配置变更工厂。
 *
 * @author lvdaxianerplus
 * @date 2026-06-12
 */
final class LlmMarkdownConfigMutationFactory {

    private static final String SCHEME_HTTP = "http";
    private static final String SCHEME_HTTPS = "https";
    private static final String DEFAULT_CONFIG_NAME = "默认 LLM 配置";
    private static final String CONFIG_ID_PREFIX = "llm_config_";
    private static final int DEFAULT_PRIORITY = 100;

    /**
     * 构建用于连通性测试的配置参数。
     *
     * @param current 当前配置
     * @param settings 原始配置参数
     * @return 规整并补齐凭证后的配置参数
     * @author lvdaxianerplus
     * @date 2026-06-12
     */
    LlmMarkdownConfigSettings settingsForTest(LlmMarkdownConfig current, LlmMarkdownConfigSettings settings) {
        LlmMarkdownConfigSettings normalized = normalizeSettings(settings);
        return builderOf(normalized).apiKey(credentialForUpdate(current, normalized.apiKey())).build();
    }

    /**
     * 校验并规整 LLM Markdown 配置参数。
     *
     * @param settings 原始配置参数
     * @return 规整后的配置参数
     * @author lvdaxianerplus
     * @date 2026-06-12
     */
    LlmMarkdownConfigSettings normalizeSettings(LlmMarkdownConfigSettings settings) {
        LlmMarkdownApiType apiType = LlmMarkdownApiType.from(settings.apiType());
        return LlmMarkdownConfigSettings.builder(configName(settings.name()), apiType.value(), validUrl(settings.url()))
                .model(required(settings.model(), "llm markdown model is required"))
                .apiKey(normalize(settings.apiKey()))
                .usageType(LlmUsageType.from(settings.usageType()).name())
                .priority(normalizedPriority(settings.priority()))
                .defaultConfig(settings.defaultConfig())
                .enabled(settings.enabled())
                .build();
    }

    /**
     * 根据当前配置和提交参数构建编辑后的配置。
     *
     * @param current 当前配置
     * @param settings 原始配置参数
     * @return 编辑后的配置
     * @author lvdaxianerplus
     * @date 2026-06-12
     */
    LlmMarkdownConfig buildUpdated(LlmMarkdownConfig current, LlmMarkdownConfigSettings settings) {
        LlmMarkdownConfigSettings normalized = normalizeSettings(settings);
        String credential = credentialForUpdate(current, normalized.apiKey());
        OffsetDateTime now = OffsetDateTime.now();
        return domainBuilder(current.id(), normalized, credential, now)
                .enabled(enabledForUpdate(current, normalized.enabled()))
                .healthy(current.healthy())
                .healthMessage(current.healthMessage())
                .lastHealthAt(current.lastHealthAt())
                .createdAt(createdAt(current, now))
                .build();
    }

    /**
     * 根据提交参数构建新配置。
     *
     * @param settings 原始配置参数
     * @return 新配置
     * @author lvdaxianerplus
     * @date 2026-06-12
     */
    LlmMarkdownConfig buildNew(LlmMarkdownConfigSettings settings) {
        LlmMarkdownConfigSettings normalized = normalizeSettings(settings);
        String credential = normalize(normalized.apiKey());
        OffsetDateTime now = OffsetDateTime.now();
        return domainBuilder(newConfigId(), normalized, credential, now)
                .enabled(enabledForCreate(normalized.enabled()))
                .healthy(false)
                .healthMessage("")
                .lastHealthAt(Optional.empty())
                .createdAt(now)
                .build();
    }

    /**
     * 从已有参数创建构建器。
     *
     * @param settings 已有配置参数
     * @return 配置参数构建器
     * @author lvdaxianerplus
     * @date 2026-06-12
     */
    private LlmMarkdownConfigSettings.Builder builderOf(LlmMarkdownConfigSettings settings) {
        return LlmMarkdownConfigSettings.builder(settings.name(), settings.apiType(), settings.url())
                .model(settings.model())
                .apiKey(settings.apiKey())
                .usageType(settings.usageType())
                .priority(settings.priority())
                .defaultConfig(settings.defaultConfig())
                .enabled(settings.enabled());
    }

    /**
     * 创建领域配置构建器。
     *
     * @param id 配置 ID
     * @param settings 配置参数
     * @param credential 凭证文本
     * @param now 当前时间
     * @return 领域配置构建器
     * @author lvdaxianerplus
     * @date 2026-06-12
     */
    private LlmMarkdownConfigBuilder domainBuilder(
            String id,
            LlmMarkdownConfigSettings settings,
            String credential,
            OffsetDateTime now
    ) {
        return LlmMarkdownConfig.builder(id, settings.name(), LlmMarkdownApiType.from(settings.apiType()))
                .endpoint(settings.url(), settings.model())
                .credential(credential)
                .usage(LlmUsageType.from(settings.usageType()), settings.priority())
                .defaultConfig(settings.defaultConfig())
                .updatedAt(now);
    }

    /**
     * 校验 LLM Markdown URL。
     *
     * @param value 原始 URL
     * @return 规整后的完整 URL
     * @author lvdaxianerplus
     * @date 2026-06-12
     */
    private String validUrl(String value) {
        String url = required(value, "llm markdown url is required");
        URI uri = parseUrl(url);
        // URL 使用 http/https 且包含主机时，按用户提交的完整地址保存。
        if (hasHttpScheme(uri) && hasHost(uri)) {
            return uri.toString();
        } else {
            // URL 协议或主机缺失时拒绝保存，避免后续请求阶段才失败。
            throw new IllegalArgumentException("llm markdown url must be http or https URL");
        }
    }

    /**
     * 判断 URL 是否使用 HTTP/HTTPS 协议。
     *
     * @param uri URI 对象
     * @return 是否为 HTTP/HTTPS
     * @author lvdaxianerplus
     * @date 2026-06-12
     */
    private boolean hasHttpScheme(URI uri) {
        return SCHEME_HTTP.equalsIgnoreCase(uri.getScheme()) || SCHEME_HTTPS.equalsIgnoreCase(uri.getScheme());
    }

    /**
     * 判断 URL 是否包含 host。
     *
     * @param uri URI 对象
     * @return 是否包含 host
     * @author lvdaxianerplus
     * @date 2026-06-12
     */
    private boolean hasHost(URI uri) {
        return uri.getHost() != null && !uri.getHost().isBlank();
    }

    /**
     * 解析 LLM Markdown URL。
     *
     * @param url 标准化 URL
     * @return URI 对象
     * @author lvdaxianerplus
     * @date 2026-06-12
     */
    private URI parseUrl(String url) {
        try {
            return URI.create(url);
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("llm markdown url must be http or https URL", ex);
        }
    }

    /**
     * 解析编辑请求中的凭证。
     *
     * @param current 当前配置
     * @param apiKey 请求 API Key
     * @return 应保存的凭证
     * @author lvdaxianerplus
     * @date 2026-06-12
     */
    private String credentialForUpdate(LlmMarkdownConfig current, String apiKey) {
        String normalized = normalize(apiKey);
        // 用户提交新凭证时，优先使用本次提交值。
        if (!normalized.isBlank()) {
            return normalized;
        } else {
            // 用户未提交凭证时，沿用已保存凭证避免误清空。
            return current.credentialValue();
        }
    }

    /**
     * 继承已有创建时间。
     *
     * @param current 当前配置
     * @param fallbackCreatedAt 兜底创建时间
     * @return 创建时间
     * @author lvdaxianerplus
     * @date 2026-06-12
     */
    private OffsetDateTime createdAt(LlmMarkdownConfig current, OffsetDateTime fallbackCreatedAt) {
        // 已有完整配置继续保留原创建时间。
        if (current.isConfigured()) {
            return current.createdAt();
        } else {
            // 未配置状态首次保存时使用当前时间。
            return fallbackCreatedAt;
        }
    }

    /**
     * 解析编辑请求中的启停状态。
     *
     * @param current 当前配置
     * @param requestedEnabled 请求启停状态
     * @return 应保存的启停状态
     * @author lvdaxianerplus
     * @date 2026-06-12
     */
    private boolean enabledForUpdate(LlmMarkdownConfig current, Boolean requestedEnabled) {
        // 请求显式提交启停状态时，以用户操作为准。
        if (requestedEnabled != null) {
            return requestedEnabled;
        } else if (current.isConfigured()) {
            // 老客户端未传 enabled 时，沿用当前配置状态。
            return current.enabled();
        } else {
            // 首次配置默认启用，保持旧行为。
            return true;
        }
    }

    /**
     * 解析新建配置启停状态。
     *
     * @param requestedEnabled 请求启停状态
     * @return 启停状态
     * @author lvdaxianerplus
     * @date 2026-06-12
     */
    private boolean enabledForCreate(Boolean requestedEnabled) {
        // 新建请求显式提交启停状态时，以用户操作为准。
        if (requestedEnabled != null) {
            return requestedEnabled;
        } else {
            // 新建配置默认启用，避免创建后不可用。
            return true;
        }
    }

    /**
     * 生成新配置 ID。
     *
     * @return 新配置 ID
     * @author lvdaxianerplus
     * @date 2026-06-12
     */
    private String newConfigId() {
        return CONFIG_ID_PREFIX + UUID.randomUUID().toString().replace("-", "");
    }

    /**
     * 标准化配置名称。
     *
     * @param value 配置名称
     * @return 配置名称
     * @author lvdaxianerplus
     * @date 2026-06-12
     */
    private String configName(String value) {
        String normalized = normalize(value);
        // 名称为空时给出兼容旧单配置的默认名称。
        if (normalized.isBlank()) {
            return DEFAULT_CONFIG_NAME;
        } else {
            // 名称非空时保留用户提交名称。
            return normalized;
        }
    }

    /**
     * 标准化优先级。
     *
     * @param priority 优先级
     * @return 优先级
     * @author lvdaxianerplus
     * @date 2026-06-12
     */
    private int normalizedPriority(int priority) {
        // 正数优先级代表用户已显式配置。
        if (priority > 0) {
            return priority;
        } else {
            // 非法或缺省优先级使用系统默认值。
            return DEFAULT_PRIORITY;
        }
    }

    /**
     * 校验并规整必填文本。
     *
     * @param value 原始文本
     * @param message 异常消息
     * @return 规整文本
     * @author lvdaxianerplus
     * @date 2026-06-12
     */
    private String required(String value, String message) {
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
     * @date 2026-06-12
     */
    private String normalize(String value) {
        return value == null ? "" : value.trim();
    }
}
