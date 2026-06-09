package io.github.lvdaxianer.doclens.j.processing.domain;

import java.time.OffsetDateTime;
import java.util.Optional;

/**
 * LLM Markdown 后处理配置。
 *
 * @param id 配置 ID
 * @param url OpenAI compatible 接口地址
 * @param model 模型名称
 * @param credentialRef API Key 凭证引用
 * @param credentialConfigured 是否已配置凭证
 * @param createdAt 创建时间
 * @param updatedAt 更新时间
 * @author lvdaxianerplus
 * @date 2026-06-09
 */
public record LlmMarkdownConfig(
        String id,
        String url,
        String model,
        Optional<String> credentialRef,
        boolean credentialConfigured,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {

    public static final String SINGLETON_ID = "default";

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
        OffsetDateTime now = OffsetDateTime.now();
        String normalizedCredentialRef = normalize(credentialRef);
        return new LlmMarkdownConfig(id, required(url, "llm markdown url is required"),
                required(model, "llm markdown model is required"), Optional.ofNullable(blankToNull(normalizedCredentialRef)),
                !normalizedCredentialRef.isBlank(), now, now);
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
        return new LlmMarkdownConfig(SINGLETON_ID, "", "", Optional.empty(), false, now, now);
    }

    /**
     * 判断是否可用于 LLM 后处理。
     *
     * @return 是否已配置 URL 和模型
     * @author lvdaxianerplus
     * @date 2026-06-09
     */
    public boolean isEnabled() {
        return !url.isBlank() && !model.isBlank();
    }

    /**
     * 获取凭证文本。
     *
     * @return 凭证文本
     * @author lvdaxianerplus
     * @date 2026-06-09
     */
    public String credentialValue() {
        return credentialRef.orElse("");
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
    private static String normalize(String value) {
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
    private static String blankToNull(String value) {
        return value.isBlank() ? null : value;
    }
}
