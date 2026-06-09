package io.github.lvdaxianer.doclens.j.processing.application;

import io.github.lvdaxianer.doclens.j.processing.domain.LlmMarkdownConfig;
import io.github.lvdaxianer.doclens.j.processing.domain.LlmMarkdownConfigRepository;
import java.net.URI;
import java.time.OffsetDateTime;
import java.util.Optional;

/**
 * LLM Markdown 后处理配置应用服务。
 *
 * @author lvdaxianerplus
 * @date 2026-06-09
 */
public class LlmMarkdownConfigService {

    private static final String SCHEME_HTTP = "http";
    private static final String SCHEME_HTTPS = "https";
    private static final String DASHSCOPE_HOST = "dashscope.aliyuncs.com";
    private static final String DASHSCOPE_COMPATIBLE_BASE_PATH = "/compatible-mode/v1";
    private static final String DASHSCOPE_CHAT_COMPLETIONS_PATH = "/compatible-mode/v1/chat/completions";

    private final LlmMarkdownConfigRepository repository;

    /**
     * 创建 LLM Markdown 配置服务。
     *
     * @param repository LLM Markdown 配置仓储
     * @author lvdaxianerplus
     * @date 2026-06-09
     */
    public LlmMarkdownConfigService(LlmMarkdownConfigRepository repository) {
        this.repository = repository;
    }

    /**
     * 查询当前配置或未配置状态。
     *
     * @return 当前配置
     * @author lvdaxianerplus
     * @date 2026-06-09
     */
    public LlmMarkdownConfig getConfig() {
        return repository.find().orElseGet(LlmMarkdownConfig::unconfigured);
    }

    /**
     * 保存 LLM Markdown 配置。
     *
     * @param settings 配置提交参数
     * @return 保存后的配置
     * @author lvdaxianerplus
     * @date 2026-06-09
     */
    public LlmMarkdownConfig saveConfig(LlmMarkdownConfigSettings settings) {
        LlmMarkdownConfig current = getConfig();
        LlmMarkdownConfig updated = buildConfig(current, normalizeSettings(settings));
        repository.save(updated);
        return updated;
    }

    /**
     * 校验并规整 LLM Markdown 配置参数。
     *
     * @param settings 原始配置参数
     * @return 规整后的配置参数
     * @author lvdaxianerplus
     * @date 2026-06-09
     */
    public LlmMarkdownConfigSettings normalizeSettings(LlmMarkdownConfigSettings settings) {
        return new LlmMarkdownConfigSettings(validUrl(settings.url()),
                required(settings.model(), "llm markdown model is required"), normalize(settings.apiKey()));
    }

    /**
     * 根据当前配置和提交参数构建新配置。
     *
     * @param current 当前配置
     * @param settings 配置提交参数
     * @return 新配置
     * @author lvdaxianerplus
     * @date 2026-06-09
     */
    private LlmMarkdownConfig buildConfig(LlmMarkdownConfig current, LlmMarkdownConfigSettings settings) {
        String url = settings.url();
        String model = settings.model();
        String credential = credentialForUpdate(current, settings.apiKey());
        OffsetDateTime now = OffsetDateTime.now();
        return new LlmMarkdownConfig(LlmMarkdownConfig.SINGLETON_ID, url, model,
                Optional.ofNullable(blankToNull(credential)), !credential.isBlank(), createdAt(current, now), now);
    }

    /**
     * 校验 LLM Markdown URL。
     *
     * @param value 原始 URL
     * @return 标准化 URL
     * @author lvdaxianerplus
     * @date 2026-06-09
     */
    private String validUrl(String value) {
        String url = required(value, "llm markdown url is required");
        URI uri = parseUrl(url);
        if (hasHttpScheme(uri) && hasHost(uri)) {
            return normalizeCompatibleEndpoint(uri).toString();
        } else {
            throw new IllegalArgumentException("llm markdown url must be http or https URL");
        }
    }

    /**
     * 规整兼容模式 endpoint，避免仅填写 DashScope 基础路径时运行时请求失败。
     *
     * @param uri 原始 URI
     * @return 规整后的 URI
     * @author lvdaxianerplus
     * @date 2026-06-09
     */
    public static URI normalizeCompatibleEndpoint(URI uri) {
        if (isDashScopeCompatibleBaseUri(uri)) {
            return URI.create(buildDashScopeChatCompletionsUrl(uri));
        } else {
            return uri;
        }
    }

    /**
     * 判断是否为 DashScope 兼容模式基础地址。
     *
     * @param uri 原始 URI
     * @return 是否需要补全 chat completions 路径
     * @author lvdaxianerplus
     * @date 2026-06-09
     */
    private static boolean isDashScopeCompatibleBaseUri(URI uri) {
        String host = uri.getHost();
        String path = uri.getPath();
        return host != null
                && DASHSCOPE_HOST.equalsIgnoreCase(host)
                && DASHSCOPE_COMPATIBLE_BASE_PATH.equals(trimTrailingSlash(path));
    }

    /**
     * 组装 DashScope chat completions endpoint。
     *
     * @param uri 原始 URI
     * @return 完整 endpoint 字符串
     * @author lvdaxianerplus
     * @date 2026-06-09
     */
    private static String buildDashScopeChatCompletionsUrl(URI uri) {
        StringBuilder builder = new StringBuilder();
        builder.append(uri.getScheme()).append("://").append(uri.getAuthority())
                .append(DASHSCOPE_CHAT_COMPLETIONS_PATH);
        if (uri.getQuery() != null && !uri.getQuery().isBlank()) {
            // 保留调用方显式传入的查询参数。
            builder.append("?").append(uri.getQuery());
        } else {
            // 无查询参数时保持最小 endpoint。
        }
        if (uri.getFragment() != null && !uri.getFragment().isBlank()) {
            // 保留片段信息，避免意外丢失。
            builder.append("#").append(uri.getFragment());
        } else {
            // 无片段时无需追加。
        }
        return builder.toString();
    }

    /**
     * 去除路径末尾斜杠，避免基础路径匹配误差。
     *
     * @param path 原始路径
     * @return 去除末尾斜杠后的路径
     * @author lvdaxianerplus
     * @date 2026-06-09
     */
    private static String trimTrailingSlash(String path) {
        if (path == null || path.isBlank()) {
            return "";
        } else if (path.endsWith("/") && path.length() > 1) {
            return path.substring(0, path.length() - 1);
        } else {
            return path;
        }
    }

    /**
     * 判断 URL 是否使用 HTTP/HTTPS 协议。
     *
     * @param uri URI 对象
     * @return 是否为 HTTP/HTTPS
     * @author lvdaxianerplus
     * @date 2026-06-09
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
     * @date 2026-06-09
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
     * @date 2026-06-09
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
     * @date 2026-06-09
     */
    private String credentialForUpdate(LlmMarkdownConfig current, String apiKey) {
        String normalized = normalize(apiKey);
        if (!normalized.isBlank()) {
            return normalized;
        } else {
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
     * @date 2026-06-09
     */
    private OffsetDateTime createdAt(LlmMarkdownConfig current, OffsetDateTime fallbackCreatedAt) {
        if (current.isEnabled()) {
            return current.createdAt();
        } else {
            return fallbackCreatedAt;
        }
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
    private String required(String value, String message) {
        String normalized = normalize(value);
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
     * @date 2026-06-09
     */
    private String normalize(String value) {
        return value == null ? "" : value.trim();
    }

    /**
     * 空文本转空引用。
     *
     * @param value 文本
     * @return 非空文本或空引用
     * @author lvdaxianerplus
     * @date 2026-06-09
     */
    private String blankToNull(String value) {
        return value.isBlank() ? null : value;
    }
}
