package io.github.lvdaxianer.doclens.j.processing.interfaces;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.github.lvdaxianer.doclens.j.processing.domain.LlmMarkdownConfig;

/**
 * LLM Markdown 配置响应。
 *
 * @param url OpenAI compatible 接口地址
 * @param model 模型名称
 * @param credentialConfigured 是否已配置 API Key
 * @author lvdaxianerplus
 * @date 2026-06-09
 */
public record LlmMarkdownConfigResponse(
        String url,
        String model,
        @JsonProperty("credential_configured") boolean credentialConfigured
) {

    /**
     * 从领域配置创建响应。
     *
     * @param config LLM Markdown 配置
     * @return LLM Markdown 配置响应
     * @author lvdaxianerplus
     * @date 2026-06-09
     */
    public static LlmMarkdownConfigResponse from(LlmMarkdownConfig config) {
        return new LlmMarkdownConfigResponse(config.url(), config.model(), config.credentialConfigured());
    }
}
