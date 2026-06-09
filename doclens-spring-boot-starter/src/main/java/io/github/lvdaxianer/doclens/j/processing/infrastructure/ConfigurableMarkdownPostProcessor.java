package io.github.lvdaxianer.doclens.j.processing.infrastructure;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.lvdaxianer.doclens.j.processing.application.LlmMarkdownConfigService;
import io.github.lvdaxianer.doclens.j.processing.application.MarkdownPostProcessingRequest;
import io.github.lvdaxianer.doclens.j.processing.application.MarkdownPostProcessingResult;
import io.github.lvdaxianer.doclens.j.processing.application.MarkdownPostProcessor;
import io.github.lvdaxianer.doclens.j.processing.domain.LlmMarkdownConfig;
import io.github.lvdaxianer.doclens.j.processing.domain.LlmMarkdownConfigRepository;
import io.github.lvdaxianer.doclens.j.processing.infrastructure.HttpMarkdownPostProcessor.HttpMarkdownPostProcessorOptions;
import java.time.Duration;

/**
 * 支持运行时配置覆盖的 Markdown 后处理器。
 *
 * @author lvdaxianerplus
 * @date 2026-06-09
 */
public class ConfigurableMarkdownPostProcessor implements MarkdownPostProcessor {

    private static final int LLM_MARKDOWN_TIMEOUT_SECONDS = 60;

    private final ObjectMapper objectMapper;
    private final LlmMarkdownConfigRepository configRepository;
    private final MarkdownPostProcessor fallbackProcessor;

    /**
     * 创建可配置 Markdown 后处理器。
     *
     * @param objectMapper JSON 映射器
     * @param configRepository LLM Markdown 配置仓储
     * @param fallbackProcessor 兜底处理器
     * @author lvdaxianerplus
     * @date 2026-06-09
     */
    public ConfigurableMarkdownPostProcessor(
            ObjectMapper objectMapper,
            LlmMarkdownConfigRepository configRepository,
            MarkdownPostProcessor fallbackProcessor
    ) {
        this.objectMapper = objectMapper;
        this.configRepository = configRepository;
        this.fallbackProcessor = fallbackProcessor;
    }

    /**
     * 根据当前运行时配置执行 Markdown 后处理。
     *
     * @param request Markdown 后处理请求
     * @return Markdown 后处理结果
     * @author lvdaxianerplus
     * @date 2026-06-09
     */
    @Override
    public MarkdownPostProcessingResult process(MarkdownPostProcessingRequest request) {
        return configRepository.find()
                .filter(LlmMarkdownConfig::isEnabled)
                .map(config -> runtimeProcessor(config).process(request))
                .orElseGet(() -> fallbackProcessor.process(request));
    }

    /**
     * 创建当前运行时配置对应的 HTTP 处理器。
     *
     * @param config LLM Markdown 配置
     * @return HTTP Markdown 后处理器
     * @author lvdaxianerplus
     * @date 2026-06-09
     */
    private MarkdownPostProcessor runtimeProcessor(LlmMarkdownConfig config) {
        HttpMarkdownPostProcessorOptions options = new HttpMarkdownPostProcessorOptions(
                LlmMarkdownConfigService.normalizeCompatibleEndpoint(java.net.URI.create(config.url())),
                config.model(), config.credentialValue(), Duration.ofSeconds(LLM_MARKDOWN_TIMEOUT_SECONDS));
        return new HttpMarkdownPostProcessor(objectMapper, options);
    }
}
