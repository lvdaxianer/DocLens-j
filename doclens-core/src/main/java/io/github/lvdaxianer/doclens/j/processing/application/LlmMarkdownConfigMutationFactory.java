package io.github.lvdaxianer.doclens.j.processing.application;

import io.github.lvdaxianer.doclens.j.processing.domain.LlmMarkdownApiType;
import io.github.lvdaxianer.doclens.j.processing.domain.LlmMarkdownConfigBuilder;
import io.github.lvdaxianer.doclens.j.processing.domain.LlmMarkdownConfig;
import io.github.lvdaxianer.doclens.j.processing.domain.LlmUsageType;
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

    private static final String CONFIG_ID_PREFIX = "llm_config_";
    private final LlmMarkdownConfigSettingsNormalizer normalizer = new LlmMarkdownConfigSettingsNormalizer();

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
        return builderOf(normalized).credentialEnvVar(credentialForUpdate(current, normalized.credentialEnvVar()))
                .build();
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
        return normalizer.normalize(settings);
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
        String credential = credentialForUpdate(current, normalized.credentialEnvVar());
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
        String credential = normalize(normalized.credentialEnvVar());
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
                .credentialEnvVar(settings.credentialEnvVar())
                .usageType(settings.usageType())
                .priority(settings.priority())
                .defaultConfig(settings.defaultConfig())
                .enabled(settings.enabled())
                .maxContextTokens(settings.maxContextTokens())
                .maxConcurrency(settings.maxConcurrency())
                .requestIntervalMillis(settings.requestIntervalMillis());
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
                .runtimeLimits(settings.maxContextTokens(), settings.maxConcurrency(),
                        settings.requestIntervalMillis())
                .defaultConfig(settings.defaultConfig())
                .updatedAt(now);
    }

    /**
     * 解析编辑请求中的凭证。
     *
     * @param current 当前配置
     * @param credentialEnvVar 请求凭证环境变量名
     * @return 应保存的凭证
     * @author lvdaxianerplus
     * @date 2026-06-12
     */
    private String credentialForUpdate(LlmMarkdownConfig current, String credentialEnvVar) {
        String normalized = normalize(credentialEnvVar);
        // 用户提交新环境变量名时，优先使用本次提交值。
        if (!normalized.isBlank()) {
            return normalized;
        } else {
            // 用户未提交环境变量名时，沿用已保存引用避免误清空。
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
