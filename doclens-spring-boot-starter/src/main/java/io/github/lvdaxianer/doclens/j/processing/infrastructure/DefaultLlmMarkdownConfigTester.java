package io.github.lvdaxianer.doclens.j.processing.infrastructure;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.lvdaxianer.doclens.j.processing.application.LlmMarkdownConfigService;
import io.github.lvdaxianer.doclens.j.processing.application.LlmMarkdownConfigSettings;
import io.github.lvdaxianer.doclens.j.processing.application.LlmMarkdownConfigTestResponse;
import io.github.lvdaxianer.doclens.j.processing.application.LlmMarkdownConfigTester;
import io.github.lvdaxianer.doclens.j.processing.application.MarkdownPostProcessingRequest;
import java.net.URI;
import java.time.Duration;
import java.util.Map;
import io.github.lvdaxianer.doclens.j.processing.domain.LlmMarkdownApiType;

/**
 * 基于 OpenAI compatible HTTP 后处理器的 LLM 配置测试实现。
 *
 * @author lvdaxianerplus
 * @date 2026-06-09
 */
public class DefaultLlmMarkdownConfigTester implements LlmMarkdownConfigTester {

    private static final int TEST_TIMEOUT_SECONDS = 10;
    private static final String TEST_DOCUMENT_ID = "llm-config-test";
    private static final String TEST_FILE_NAME = "llm-config-test.txt";
    private static final String TEST_OCR_TEXT = "测试 OCR 文本";

    private final ObjectMapper objectMapper;
    private final LlmMarkdownConfigService configService;

    /**
     * 创建 LLM 配置测试实现。
     *
     * @param objectMapper JSON 映射器
     * @param configService LLM 配置服务
     * @author lvdaxianerplus
     * @date 2026-06-09
     */
    public DefaultLlmMarkdownConfigTester(ObjectMapper objectMapper, LlmMarkdownConfigService configService) {
        this.objectMapper = objectMapper;
        this.configService = configService;
    }

    /**
     * 使用最小 Markdown 请求测试远端可达性。
     *
     * @param settings 配置参数
     * @return 测试结果
     * @author lvdaxianerplus
     * @date 2026-06-09
     */
    @Override
    public LlmMarkdownConfigTestResponse test(LlmMarkdownConfigSettings settings) {
        LlmMarkdownConfigSettings normalized = configService.normalizeSettings(settings);
        LlmMarkdownApiType apiType = LlmMarkdownApiType.from(normalized.apiType());
        URI endpoint = LlmMarkdownConfigService.normalizeEndpoint(URI.create(normalized.url()), apiType);
        MarkdownPostProcessorFactory factory = new MarkdownPostProcessorFactory(objectMapper,
                Duration.ofSeconds(TEST_TIMEOUT_SECONDS));
        try {
            factory.create(apiType, endpoint, normalized.model(), normalized.apiKey())
                    .process(new MarkdownPostProcessingRequest(TEST_DOCUMENT_ID, TEST_FILE_NAME, Map.of(), TEST_OCR_TEXT));
            return LlmMarkdownConfigTestResponse.reachable();
        } catch (IllegalStateException ex) {
            return LlmMarkdownConfigTestResponse.unreachable(ex.getMessage());
        }
    }
}
