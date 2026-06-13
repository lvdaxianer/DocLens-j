package io.github.lvdaxianer.doclens.j.contract;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import java.nio.charset.StandardCharsets;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

/**
 * LLM Markdown 多配置 API 契约测试夹具。
 *
 * @author lvdaxianerplus
 * @date 2026-06-12
 */
final class LlmMarkdownMultiConfigApiFixtures {

    /** 默认 OpenAI 兼容接口地址。 */
    private static final String DEFAULT_LLM_URL = "https://llm.example.com/v1/chat/completions";
    /** 备用 Anthropic 接口地址。 */
    private static final String ANTHROPIC_URL = "https://api.minimaxi.com/anthropic";
    /** 测试专用凭证环境变量名。 */
    private static final String TEST_CREDENTIAL_ENV_VAR = "MINIMAX_API_KEY";
    /** 测试专用旧凭证环境变量名。 */
    private static final String OLD_TEST_CREDENTIAL_ENV_VAR = "OLD_MINIMAX_API_KEY";
    /** 默认更新模型名称。 */
    private static final String MARKDOWN_MODEL_V2 = "markdown-model-v2";
    /** 默认配置优先级。 */
    private static final int DEFAULT_PRIORITY = 20;
    /** 备用配置优先级。 */
    private static final int BACKUP_PRIORITY = 10;
    /** 更新配置优先级。 */
    private static final int UPDATED_PRIORITY = 5;

    /** MockMvc 测试客户端。 */
    private final MockMvc mockMvc;

    /**
     * 创建多配置 API 测试夹具。
     *
     * @param mockMvc MockMvc 测试客户端
     * @author lvdaxianerplus
     * @date 2026-06-12
     */
    LlmMarkdownMultiConfigApiFixtures(MockMvc mockMvc) {
        this.mockMvc = mockMvc;
    }

    /**
     * 创建默认 OpenAI 配置。
     *
     * @param name 配置名称
     * @return 配置 ID
     * @throws Exception 请求执行失败时抛出
     * @author lvdaxianerplus
     * @date 2026-06-12
     */
    String createDefaultOpenAiConfig(String name) throws Exception {
        return createManagedConfig(defaultOpenAiJson(name));
    }

    /**
     * 创建备用 Anthropic 配置。
     *
     * @param name 配置名称
     * @return 配置 ID
     * @throws Exception 请求执行失败时抛出
     * @author lvdaxianerplus
     * @date 2026-06-12
     */
    String createBackupAnthropicConfig(String name) throws Exception {
        return createManagedConfig(backupAnthropicJson(name));
    }

    /**
     * 创建最小 OpenAI 配置请求 JSON。
     *
     * @param name 配置名称
     * @return 请求 JSON
     * @author lvdaxianerplus
     * @date 2026-06-12
     */
    String minimalOpenAiJson(String name) {
        return llmConfigJson(openAiConfig(name));
    }

    /**
     * 创建 OpenAI 更新请求 JSON。
     *
     * @param name 配置名称
     * @return 请求 JSON
     * @author lvdaxianerplus
     * @date 2026-06-12
     */
    String updatedOpenAiJson(String name) {
        return llmConfigJson(openAiConfig(name).withoutCredentialEnvVar().priority(UPDATED_PRIORITY)
                .model(MARKDOWN_MODEL_V2));
    }

    /**
     * 创建启停请求 JSON。
     *
     * @param enabled 是否启用
     * @return 请求 JSON
     * @author lvdaxianerplus
     * @date 2026-06-12
     */
    String enabledJson(boolean enabled) {
        return """
                {
                  "enabled": %s
                }
                """.formatted(enabled);
    }

    /**
     * 新建受管理配置并读取配置 ID。
     *
     * @param json 请求 JSON
     * @return 配置 ID
     * @throws Exception 请求执行失败时抛出
     * @author lvdaxianerplus
     * @date 2026-06-12
     */
    private String createManagedConfig(String json) throws Exception {
        String response = mockMvc.perform(post("/api/v1/llm-markdown-config")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString(StandardCharsets.UTF_8);
        return JsonPath.read(response, "$.id");
    }

    /**
     * 创建默认 OpenAI 配置 JSON。
     *
     * @param name 配置名称
     * @return 请求 JSON
     * @author lvdaxianerplus
     * @date 2026-06-12
     */
    private String defaultOpenAiJson(String name) {
        return llmConfigJson(openAiConfig(name).priority(DEFAULT_PRIORITY));
    }

    /**
     * 创建备用 Anthropic 配置 JSON。
     *
     * @param name 配置名称
     * @return 请求 JSON
     * @author lvdaxianerplus
     * @date 2026-06-12
     */
    private String backupAnthropicJson(String name) {
        return llmConfigJson(anthropicConfig(name).priority(BACKUP_PRIORITY));
    }

    /**
     * 创建 OpenAI 配置对象。
     *
     * @param name 配置名称
     * @return 配置对象
     * @author lvdaxianerplus
     * @date 2026-06-12
     */
    private LlmMarkdownTestConfig openAiConfig(String name) {
        return LlmMarkdownTestConfig.openAi(name);
    }

    /**
     * 创建 Anthropic 配置对象。
     *
     * @param name 配置名称
     * @return 配置对象
     * @author lvdaxianerplus
     * @date 2026-06-12
     */
    private LlmMarkdownTestConfig anthropicConfig(String name) {
        return LlmMarkdownTestConfig.anthropic(name);
    }

    /**
     * 序列化 LLM 测试配置。
     *
     * @param config 测试配置
     * @return 请求 JSON
     * @author lvdaxianerplus
     * @date 2026-06-12
     */
    private String llmConfigJson(LlmMarkdownTestConfig config) {
        return """
                {
                  "name": "%s",
                  "api_type": "%s",
                  "url": "%s",
                  "model": "%s",
                  "credential_env_var": "%s",
                  "usage_type": "%s",
                  "priority": %d,
                  "is_default": %s,
                  "max_context_tokens": 16000,
                  "max_concurrency": 2,
                  "request_interval_millis": 1500,
                  "enabled": %s
                }
                """.formatted(config.name(), config.apiType(), config.url(), config.model(), config.credentialEnvVar(),
                config.usageType(), config.priority(), config.isDefault(), config.isEnabled());
    }
}
