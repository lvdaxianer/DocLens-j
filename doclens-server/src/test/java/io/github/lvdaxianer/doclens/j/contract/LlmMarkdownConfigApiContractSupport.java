package io.github.lvdaxianer.doclens.j.contract;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import io.github.lvdaxianer.doclens.j.processing.application.LlmMarkdownConfigTester;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
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
        return configJson("openai", url, model, apiKey);
    }

    /**
     * 创建带协议类型的配置请求 JSON。
     *
     * @param apiType LLM API 协议类型
     * @param url 接口地址
     * @param model 模型名称
     * @param apiKey API Key
     * @return 请求 JSON
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    protected String configJson(String apiType, String url, String model, String apiKey) {
        return """
                {
                  "api_type": "%s",
                  "url": "%s",
                  "model": "%s",
                  "api_key": "%s"
                }
                """.formatted(apiType, url, model, apiKey);
    }
}
