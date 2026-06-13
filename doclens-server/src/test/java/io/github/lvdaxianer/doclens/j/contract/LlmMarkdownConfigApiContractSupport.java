package io.github.lvdaxianer.doclens.j.contract;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import io.github.lvdaxianer.doclens.j.processing.application.LlmMarkdownConfigTester;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

/**
 * LLM Markdown 配置 API 契约测试共享支持。
 *
 * @author lvdaxianerplus
 * @date 2026-06-11
 */
@SpringBootTest
@AutoConfigureMockMvc
abstract class LlmMarkdownConfigApiContractSupport {

    /** 默认 LLM 配置地址。 */
    protected static final String DEFAULT_LLM_URL = "https://llm.example.com/v1/chat/completions";
    /** DashScope 兼容模式完整地址。 */
    protected static final String DASHSCOPE_COMPATIBLE_URL = "https://dashscope.aliyuncs.com/compatible-mode/v1";
    /** Anthropic 完整地址。 */
    protected static final String ANTHROPIC_URL = "https://api.minimaxi.com/anthropic";
    /** 测试专用 API Key 占位值。 */
    protected static final String TEST_API_KEY = "test-api-key";
    /** 测试专用旧 API Key 占位值。 */
    protected static final String OLD_TEST_API_KEY = "old-test-api-key";
    /** 测试专用新 API Key 占位值。 */
    protected static final String NEW_TEST_API_KEY = "new-test-api-key";

    /** 临时目录用于隔离测试数据库和存储。 */
    @TempDir
    static java.nio.file.Path tempDir;

    /** MockMvc 测试客户端。 */
    @Autowired
    protected MockMvc mockMvc;

    /** 测试数据库访问工具。 */
    @Autowired
    private JdbcTemplate jdbcTemplate;

    /** LLM Markdown 配置探测器 mock。 */
    @MockBean
    protected LlmMarkdownConfigTester configTester;

    /**
     * 配置隔离的测试数据库和存储目录。
     *
     * @param registry 动态属性注册表
     * @author lvdaxianerplus
     * @date 2026-06-09
     */
    @DynamicPropertySource
    static void testProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url",
                () -> "jdbc:h2:file:" + tempDir.resolve("llm-config-api") + ";MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE");
        registry.add("doclens.storage-root", () -> tempDir.resolve("storage").toString());
        registry.add("doclens.paddle-ocr.enabled", () -> "false");
    }

    /**
     * 清理单例配置，避免契约用例之间互相污染。
     *
     * @author lvdaxianerplus
     * @date 2026-06-12
     */
    @BeforeEach
    void cleanLlmMarkdownConfig() {
        jdbcTemplate.update("DELETE FROM doclens_llm_markdown_config");
    }

    /**
     * 保存 LLM Markdown 配置。
     *
     * @param url 接口地址
     * @param model 模型名称
     * @param apiKey API Key
     * @throws Exception 请求执行失败时抛出
     * @author lvdaxianerplus
     * @date 2026-06-09
     */
    protected void saveConfig(String url, String model, String apiKey) throws Exception {
        mockMvc.perform(put("/api/v1/llm-markdown-config")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(configJson(url, model, apiKey)))
                .andExpect(status().isOk());
    }

    /**
     * 创建配置请求 JSON。
     *
     * @param url 接口地址
     * @param model 模型名称
     * @param apiKey API Key
     * @return 请求 JSON
     * @author lvdaxianerplus
     * @date 2026-06-09
     */
    protected String configJson(String url, String model, String apiKey) {
        return configJson(new CompatibilityConfig(url, model, apiKey));
    }

    /**
     * 创建 Anthropic 配置请求 JSON。
     *
     * @param url 接口地址
     * @param model 模型名称
     * @param apiKey API Key
     * @return 请求 JSON
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    protected String anthropicConfigJson(String url, String model, String apiKey) {
        return configJson(new CompatibilityConfig(url, model, apiKey).anthropic());
    }

    /**
     * 创建禁用状态的 OpenAI 配置请求 JSON。
     *
     * @return 请求 JSON
     * @author lvdaxianerplus
     * @date 2026-06-12
     */
    protected String disabledOpenAiConfigJson() {
        return configJson(new CompatibilityConfig(DEFAULT_LLM_URL, "markdown-model", TEST_API_KEY).disabled());
    }

    /**
     * 创建兼容配置请求 JSON。
     *
     * @param config 兼容配置
     * @return 请求 JSON
     * @author lvdaxianerplus
     * @date 2026-06-12
     */
    private String configJson(CompatibilityConfig config) {
        return """
                {
                  "api_type": "%s",
                  "url": "%s",
                  "model": "%s",
                  "api_key": "%s",
                  "max_context_tokens": 16000,
                  "max_concurrency": 2,
                  "request_interval_millis": 1500,
                  "enabled": %s
                }
                """.formatted(config.apiType(), config.url(), config.model(), config.apiKey(), config.enabled());
    }

    /**
     * 创建默认 OpenAI 测试配置。
     *
     * @param name 配置名称
     * @return 配置 ID
     * @throws Exception 请求执行失败时抛出
     * @author lvdaxianerplus
     * @date 2026-06-12
     */
    protected String createDefaultOpenAiConfig(String name) throws Exception {
        return fixtures().createDefaultOpenAiConfig(name);
    }

    /**
     * 创建备用 Anthropic 测试配置。
     *
     * @param name 配置名称
     * @return 配置 ID
     * @throws Exception 请求执行失败时抛出
     * @author lvdaxianerplus
     * @date 2026-06-12
     */
    protected String createBackupAnthropicConfig(String name) throws Exception {
        return fixtures().createBackupAnthropicConfig(name);
    }

    /**
     * 创建最小 OpenAI 请求 JSON。
     *
     * @param name 配置名称
     * @return 请求 JSON
     * @author lvdaxianerplus
     * @date 2026-06-12
     */
    protected String minimalOpenAiJson(String name) {
        return fixtures().minimalOpenAiJson(name);
    }

    /**
     * 创建 OpenAI 更新请求 JSON。
     *
     * @param name 配置名称
     * @return 请求 JSON
     * @author lvdaxianerplus
     * @date 2026-06-12
     */
    protected String updatedOpenAiJson(String name) {
        return fixtures().updatedOpenAiJson(name);
    }

    /**
     * 创建启停请求 JSON。
     *
     * @param enabled 是否启用
     * @return 请求 JSON
     * @author lvdaxianerplus
     * @date 2026-06-12
     */
    protected String enabledJson(boolean enabled) {
        return fixtures().enabledJson(enabled);
    }

    /**
     * 创建多配置测试夹具。
     *
     * @return 多配置测试夹具
     * @author lvdaxianerplus
     * @date 2026-06-12
     */
    private LlmMarkdownMultiConfigApiFixtures fixtures() {
        return new LlmMarkdownMultiConfigApiFixtures(mockMvc);
    }

    /**
     * 兼容配置测试数据。
     *
     * @param url 接口地址
     * @param model 模型名称
     * @param apiKey API Key
     * @param apiType API 协议类型
     * @param enabled 是否启用
     * @author lvdaxianerplus
     * @date 2026-06-12
     */
    private record CompatibilityConfig(
            String url,
            String model,
            String apiKey,
            String apiType,
            boolean enabled
    ) {

        /**
         * 创建默认 OpenAI 兼容配置。
         *
         * @param url 接口地址
         * @param model 模型名称
         * @param apiKey API Key
         * @author lvdaxianerplus
         * @date 2026-06-12
         */
        private CompatibilityConfig(String url, String model, String apiKey) {
            this(url, model, apiKey, "openai", true);
        }

        /**
         * 复制为 Anthropic 协议配置。
         *
         * @return Anthropic 协议配置
         * @author lvdaxianerplus
         * @date 2026-06-12
         */
        private CompatibilityConfig anthropic() {
            return new CompatibilityConfig(url, model, apiKey, "anthropic", enabled);
        }

        /**
         * 复制为禁用状态配置。
         *
         * @return 禁用状态配置
         * @author lvdaxianerplus
         * @date 2026-06-12
         */
        private CompatibilityConfig disabled() {
            return new CompatibilityConfig(url, model, apiKey, apiType, false);
        }
    }
}
