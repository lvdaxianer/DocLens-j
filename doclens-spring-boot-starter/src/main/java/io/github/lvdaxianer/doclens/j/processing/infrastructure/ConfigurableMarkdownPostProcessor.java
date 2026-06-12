package io.github.lvdaxianer.doclens.j.processing.infrastructure;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.lvdaxianer.doclens.j.processing.application.LlmConfigSelector;
import io.github.lvdaxianer.doclens.j.processing.application.MarkdownPostProcessingRequest;
import io.github.lvdaxianer.doclens.j.processing.application.MarkdownPostProcessingResult;
import io.github.lvdaxianer.doclens.j.processing.application.MarkdownPostProcessor;
import io.github.lvdaxianer.doclens.j.processing.domain.LlmMarkdownConfig;
import io.github.lvdaxianer.doclens.j.processing.domain.LlmMarkdownConfigRepository;
import io.github.lvdaxianer.doclens.j.processing.domain.LlmUsageType;
import java.time.Duration;

/**
 * 支持运行时配置覆盖的 Markdown 后处理器。
 *
 * @author lvdaxianerplus
 * @date 2026-06-09
 */
public class ConfigurableMarkdownPostProcessor implements MarkdownPostProcessor {

    private static final int LLM_MARKDOWN_TIMEOUT_SECONDS = 60;
    private static final String NO_AVAILABLE_LLM_CONFIG_REASON = "no_available_llm_config";

    private final LlmConfigSelector configSelector;
    private final MarkdownPostProcessor fallbackProcessor;
    private final MarkdownPostProcessorFactory processorFactory;

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
        this.configSelector = new LlmConfigSelector(configRepository);
        this.fallbackProcessor = fallbackProcessor;
        this.processorFactory = new MarkdownPostProcessorFactory(objectMapper,
                Duration.ofSeconds(LLM_MARKDOWN_TIMEOUT_SECONDS));
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
        return configSelector.select(LlmUsageType.MARKDOWN_POST_PROCESSING)
                .map(config -> processRuntimeConfig(config, request))
                .orElseGet(() -> noAvailableConfigResult(request));
    }

    /**
     * 根据当前运行时配置处理 Markdown。
     *
     * @param config LLM Markdown 配置
     * @param request Markdown 后处理请求
     * @return Markdown 后处理结果
     * @author lvdaxianerplus
     * @date 2026-06-12
     */
    private MarkdownPostProcessingResult processRuntimeConfig(
            LlmMarkdownConfig config,
            MarkdownPostProcessingRequest request
    ) {
        // 已配置且未暂停时调用运行时 LLM。
        if (config.isAvailableForPostProcessing()) {
            return runtimeProcessor(config).process(request);
        } else {
            // 选择器正常不会返回不可用配置；这里保留兜底分支兼容直接调用。
            return fallbackProcessor.process(request);
        }
    }

    /**
     * 创建没有可用 LLM 配置时的直通结果。
     *
     * @param request Markdown 后处理请求
     * @return 直通 OCR 原文的后处理结果
     * @author lvdaxianerplus
     * @date 2026-06-12
     */
    private MarkdownPostProcessingResult noAvailableConfigResult(MarkdownPostProcessingRequest request) {
        return MarkdownPostProcessingResult.passthrough(request.ocrText(), NO_AVAILABLE_LLM_CONFIG_REASON);
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
        return processorFactory.create(config);
    }
}
