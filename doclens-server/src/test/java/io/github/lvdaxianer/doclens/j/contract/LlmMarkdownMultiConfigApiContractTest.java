package io.github.lvdaxianer.doclens.j.contract;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

/**
 * LLM Markdown 多配置管理 API 契约测试。
 *
 * @author lvdaxianerplus
 * @date 2026-06-12
 */
class LlmMarkdownMultiConfigApiContractTest extends LlmMarkdownConfigApiContractSupport {

    /**
     * 新建 LLM 配置应返回配置 ID 和多配置治理字段。
     *
     * @throws Exception 请求执行失败时抛出
     * @author lvdaxianerplus
     * @date 2026-06-12
     */
    @Test
    void createConfigReturnsManagedConfig() throws Exception {
        String response = mockMvc.perform(post("/api/v1/llm-markdown-config")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(minimalOpenAiJson("主配置")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").isString())
                .andExpect(jsonPath("$.name").value("主配置"))
                .andExpect(jsonPath("$.usage_type").value("MARKDOWN_POST_PROCESSING"))
                .andExpect(jsonPath("$.priority").value(100))
                .andExpect(jsonPath("$.is_default").value(true))
                .andExpect(jsonPath("$.enabled").value(true))
                .andExpect(jsonPath("$.credential_env_var").value(TEST_CREDENTIAL_ENV_VAR))
                .andExpect(jsonPath("$.api_key").doesNotExist())
                .andReturn()
                .getResponse()
                .getContentAsString(StandardCharsets.UTF_8);

        assertThat(response).doesNotContain("api_key");
    }

    /**
     * 列表接口应按优先级返回多个配置且不回显 api_key 字段。
     *
     * @throws Exception 请求执行失败时抛出
     * @author lvdaxianerplus
     * @date 2026-06-12
     */
    @Test
    void getConfigReturnsConfigListWithoutApiKey() throws Exception {
        createDefaultOpenAiConfig("主配置");
        createBackupAnthropicConfig("备用配置");

        String response = mockMvc.perform(get("/api/v1/llm-markdown-config"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("备用配置"))
                .andExpect(jsonPath("$[0].priority").value(10))
                .andExpect(jsonPath("$[1].name").value("主配置"))
                .andExpect(jsonPath("$[1].priority").value(20))
                .andExpect(jsonPath("$[0].credential_env_var").value(OLD_TEST_CREDENTIAL_ENV_VAR))
                .andExpect(jsonPath("$[1].credential_env_var").value(TEST_CREDENTIAL_ENV_VAR))
                .andExpect(jsonPath("$[0].api_key").doesNotExist())
                .andExpect(jsonPath("$[1].api_key").doesNotExist())
                .andReturn()
                .getResponse()
                .getContentAsString(StandardCharsets.UTF_8);

        assertThat(response).doesNotContain("api_key");
    }

    /**
     * 指定 ID 更新配置应只修改目标配置。
     *
     * @throws Exception 请求执行失败时抛出
     * @author lvdaxianerplus
     * @date 2026-06-12
     */
    @Test
    void updateConfigByIdOnlyChangesTargetConfig() throws Exception {
        String targetId = createDefaultOpenAiConfig("主配置");
        createBackupAnthropicConfig("备用配置");

        mockMvc.perform(put("/api/v1/llm-markdown-config/{id}", targetId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updatedOpenAiJson("主配置 v2")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(targetId))
                .andExpect(jsonPath("$.name").value("主配置 v2"))
                .andExpect(jsonPath("$.model").value("markdown-model-v2"))
                .andExpect(jsonPath("$.priority").value(5))
                .andExpect(jsonPath("$.credential_configured").value(true));

        mockMvc.perform(get("/api/v1/llm-markdown-config"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(targetId))
                .andExpect(jsonPath("$[1].name").value("备用配置"));
    }

    /**
     * 启停接口应只切换指定配置状态。
     *
     * @throws Exception 请求执行失败时抛出
     * @author lvdaxianerplus
     * @date 2026-06-12
     */
    @Test
    void patchEnabledTogglesTargetConfigOnly() throws Exception {
        String targetId = createDefaultOpenAiConfig("主配置");
        String backupId = createBackupAnthropicConfig("备用配置");

        mockMvc.perform(patch("/api/v1/llm-markdown-config/{id}/enabled", targetId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(enabledJson(false)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(targetId))
                .andExpect(jsonPath("$.enabled").value(false));

        mockMvc.perform(get("/api/v1/llm-markdown-config"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(backupId))
                .andExpect(jsonPath("$[0].enabled").value(true))
                .andExpect(jsonPath("$[1].id").value(targetId))
                .andExpect(jsonPath("$[1].enabled").value(false));
    }

    /**
     * 设置默认配置应清理同用途其它默认标记。
     *
     * @throws Exception 请求执行失败时抛出
     * @author lvdaxianerplus
     * @date 2026-06-12
     */
    @Test
    void patchDefaultClearsOtherDefaultsForSameUsage() throws Exception {
        String firstId = createDefaultOpenAiConfig("主配置");
        String secondId = createBackupAnthropicConfig("备用配置");

        mockMvc.perform(patch("/api/v1/llm-markdown-config/{id}/default", secondId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(secondId))
                .andExpect(jsonPath("$.is_default").value(true));

        mockMvc.perform(get("/api/v1/llm-markdown-config"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(secondId))
                .andExpect(jsonPath("$[0].is_default").value(true))
                .andExpect(jsonPath("$[1].id").value(firstId))
                .andExpect(jsonPath("$[1].is_default").value(false));
    }

    /**
     * 删除指定配置后列表应移除该配置。
     *
     * @throws Exception 请求执行失败时抛出
     * @author lvdaxianerplus
     * @date 2026-06-12
     */
    @Test
    void deleteConfigRemovesTargetConfig() throws Exception {
        String targetId = createDefaultOpenAiConfig("主配置");
        String backupId = createBackupAnthropicConfig("备用配置");

        mockMvc.perform(delete("/api/v1/llm-markdown-config/{id}", targetId))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/llm-markdown-config"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(backupId))
                .andExpect(jsonPath("$[1]").doesNotExist());
    }
}
