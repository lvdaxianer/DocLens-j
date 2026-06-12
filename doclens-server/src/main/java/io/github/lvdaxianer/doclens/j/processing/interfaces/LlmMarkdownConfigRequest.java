package io.github.lvdaxianer.doclens.j.processing.interfaces;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.github.lvdaxianer.doclens.j.processing.application.LlmMarkdownConfigSettings;

/**
 * LLM Markdown 配置请求。
 *
 * @author lvdaxianerplus
 * @date 2026-06-09
 */
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class LlmMarkdownConfigRequest {

    /** 配置名称。 */
    private String name;
    /** API 协议类型。 */
    @JsonProperty("api_type")
    private String apiType;
    /** OpenAI compatible 接口地址。 */
    private String url;
    /** 模型名称。 */
    private String model;
    /** API Key，可为空。 */
    @JsonProperty("api_key")
    private String apiKey;
    /** 配置用途。 */
    @JsonProperty("usage_type")
    private String usageType;
    /** 优先级。 */
    private Integer priority;
    /** 是否默认配置。 */
    @JsonProperty("is_default")
    private Boolean defaultConfig;
    /** 是否启用 LLM Markdown 后处理。 */
    private Boolean enabled;

    /**
     * 转换为应用服务配置参数。
     *
     * @return LLM Markdown 配置参数
     * @author lvdaxianerplus
     * @date 2026-06-09
     */
    LlmMarkdownConfigSettings toSettings() {
        return LlmMarkdownConfigSettings.builder(name, apiType, url)
                .model(model)
                .apiKey(apiKey)
                .usageType(usageType)
                .priority(priority == null ? 0 : priority)
                .defaultConfig(defaultConfig == null || defaultConfig)
                .enabled(enabled)
                .build();
    }
}
