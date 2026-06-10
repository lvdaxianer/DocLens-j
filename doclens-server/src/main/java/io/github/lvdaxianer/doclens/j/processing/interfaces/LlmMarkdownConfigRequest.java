package io.github.lvdaxianer.doclens.j.processing.interfaces;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.github.lvdaxianer.doclens.j.processing.application.LlmMarkdownConfigSettings;

/**
 * LLM Markdown 配置请求。
 *
 * @param apiType API 协议类型
 * @param url OpenAI compatible 接口地址
 * @param model 模型名称
 * @param apiKey API Key，可为空
 * @author lvdaxianerplus
 * @date 2026-06-09
 */
public record LlmMarkdownConfigRequest(
        @JsonProperty("api_type") String apiType,
        String url,
        String model,
        @JsonProperty("api_key") String apiKey
) {

    /**
     * 转换为应用服务配置参数。
     *
     * @return LLM Markdown 配置参数
     * @author lvdaxianerplus
     * @date 2026-06-09
     */
    LlmMarkdownConfigSettings toSettings() {
        return new LlmMarkdownConfigSettings(apiType, url, model, apiKey);
    }
}
