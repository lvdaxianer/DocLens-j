package io.github.lvdaxianer.doclens.j.processing.application;

import io.github.lvdaxianer.doclens.j.processing.domain.LlmMarkdownApiType;
import io.github.lvdaxianer.doclens.j.processing.domain.LlmUsageType;
import java.net.URI;
import java.util.regex.Pattern;

/**
 * LLM Markdown 配置提交参数规整器。
 *
 * @author lvdaxianerplus
 * @date 2026-06-13
 */
final class LlmMarkdownConfigSettingsNormalizer {

    private static final String SCHEME_HTTP = "http";
    private static final String SCHEME_HTTPS = "https";
    private static final String DEFAULT_CONFIG_NAME = "默认 LLM 配置";
    private static final int DEFAULT_PRIORITY = 100;
    private static final int MIN_MAX_CONTEXT_TOKENS = 1000;
    private static final int MIN_MAX_CONCURRENCY = 1;
    private static final int MIN_REQUEST_INTERVAL_MILLIS = 0;
    private static final Pattern ENV_VAR_NAME = Pattern.compile("[A-Z_][A-Z0-9_]*");

    /**
     * 校验并规整 LLM Markdown 配置参数。
     *
     * @param settings 原始配置参数
     * @return 规整后的配置参数
     * @author lvdaxianerplus
     * @date 2026-06-13
     */
    LlmMarkdownConfigSettings normalize(LlmMarkdownConfigSettings settings) {
        LlmMarkdownApiType apiType = LlmMarkdownApiType.from(settings.apiType());
        return LlmMarkdownConfigSettings.builder(configName(settings.name()), apiType.value(), validUrl(settings.url()))
                .model(required(settings.model(), "llm markdown model is required"))
                .credentialEnvVar(normalizedCredentialEnvVar(settings.credentialEnvVar()))
                .usageType(LlmUsageType.from(settings.usageType()).name())
                .priority(normalizedPriority(settings.priority()))
                .defaultConfig(settings.defaultConfig())
                .enabled(settings.enabled())
                .maxContextTokens(normalizedMaxContextTokens(settings.maxContextTokens()))
                .maxConcurrency(normalizedMaxConcurrency(settings.maxConcurrency()))
                .requestIntervalMillis(normalizedRequestIntervalMillis(settings.requestIntervalMillis()))
                .build();
    }

    /**
     * 校验 LLM Markdown URL。
     *
     * @param value 原始 URL
     * @return 规整后的完整 URL
     * @author lvdaxianerplus
     * @date 2026-06-13
     */
    private String validUrl(String value) {
        String url = required(value, "llm markdown url is required");
        URI uri = parseUrl(url);
        if (hasHttpScheme(uri) && hasHost(uri)) {
            return uri.toString();
        } else {
            throw new IllegalArgumentException("llm markdown url must be http or https URL");
        }
    }

    /**
     * 判断 URL 是否使用 HTTP/HTTPS 协议。
     *
     * @param uri URI 对象
     * @return 是否为 HTTP/HTTPS
     * @author lvdaxianerplus
     * @date 2026-06-13
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
     * @date 2026-06-13
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
     * @date 2026-06-13
     */
    private URI parseUrl(String url) {
        try {
            return URI.create(url);
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("llm markdown url must be http or https URL", ex);
        }
    }

    /**
     * 标准化配置名称。
     *
     * @param value 配置名称
     * @return 配置名称
     * @author lvdaxianerplus
     * @date 2026-06-13
     */
    private String configName(String value) {
        String normalized = normalizeText(value);
        if (normalized.isBlank()) {
            return DEFAULT_CONFIG_NAME;
        } else {
            return normalized;
        }
    }

    /**
     * 标准化优先级。
     *
     * @param priority 优先级
     * @return 优先级
     * @author lvdaxianerplus
     * @date 2026-06-13
     */
    private int normalizedPriority(int priority) {
        if (priority > 0) {
            return priority;
        } else {
            return DEFAULT_PRIORITY;
        }
    }

    /**
     * 校验最大上下文 token 数。
     *
     * @param value 原始最大上下文 token 数
     * @return 校验后的最大上下文 token 数
     * @author lvdaxianerplus
     * @date 2026-06-13
     */
    private int normalizedMaxContextTokens(int value) {
        if (value >= MIN_MAX_CONTEXT_TOKENS) {
            return value;
        } else {
            throw new IllegalArgumentException("llm markdown max context tokens must be at least 1000");
        }
    }

    /**
     * 校验最大并发数。
     *
     * @param value 原始最大并发数
     * @return 校验后的最大并发数
     * @author lvdaxianerplus
     * @date 2026-06-13
     */
    private int normalizedMaxConcurrency(int value) {
        if (value >= MIN_MAX_CONCURRENCY) {
            return value;
        } else {
            throw new IllegalArgumentException("llm markdown max concurrency must be at least 1");
        }
    }

    /**
     * 校验请求启动最小间隔。
     *
     * @param value 原始请求启动最小间隔毫秒数
     * @return 校验后的请求启动最小间隔毫秒数
     * @author lvdaxianerplus
     * @date 2026-06-13
     */
    private int normalizedRequestIntervalMillis(int value) {
        if (value >= MIN_REQUEST_INTERVAL_MILLIS) {
            return value;
        } else {
            throw new IllegalArgumentException("llm markdown request interval millis must be at least 0");
        }
    }

    /**
     * 校验凭证环境变量名。
     *
     * @param value 原始凭证环境变量名
     * @return 标准化凭证环境变量名
     * @author lvdaxianerplus
     * @date 2026-06-13
     */
    private String normalizedCredentialEnvVar(String value) {
        String normalized = normalizeText(value);
        // 空值表示复用已保存环境变量名，由变更工厂补齐。
        if (normalized.isBlank()) {
            return normalized;
        } else if (ENV_VAR_NAME.matcher(normalized).matches()) {
            return normalized;
        } else {
            throw new IllegalArgumentException("credential env var must match [A-Z_][A-Z0-9_]*");
        }
    }

    /**
     * 校验并规整必填文本。
     *
     * @param value 原始文本
     * @param message 异常消息
     * @return 规整文本
     * @author lvdaxianerplus
     * @date 2026-06-13
     */
    private String required(String value, String message) {
        String normalized = normalizeText(value);
        if (!normalized.isBlank()) {
            return normalized;
        } else {
            throw new IllegalArgumentException(message);
        }
    }

    /**
     * 标准化可选文本。
     *
     * @param value 原始文本
     * @return 标准化文本
     * @author lvdaxianerplus
     * @date 2026-06-13
     */
    private String normalizeText(String value) {
        return value == null ? "" : value.trim();
    }
}
