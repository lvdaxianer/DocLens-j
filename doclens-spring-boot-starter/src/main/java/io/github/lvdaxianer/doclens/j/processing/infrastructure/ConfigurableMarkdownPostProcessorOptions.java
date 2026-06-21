package io.github.lvdaxianer.doclens.j.processing.infrastructure;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.lvdaxianer.doclens.j.processing.application.MarkdownPostProcessor;
import io.github.lvdaxianer.doclens.j.processing.domain.LlmMarkdownConfigRepository;
import java.util.Map;
import java.util.Objects;

/**
 * 可配置 Markdown 后处理器选项。
 *
 * @param objectMapper JSON 映射器
 * @param configRepository LLM Markdown 配置仓储
 * @param fallbackProcessor 兜底处理器
 * @param environmentValues 环境变量映射
 * @param runtimeOptions 运行时选项
 * @author lvdaxianerplus
 * @date 2026-06-20
 */
public record ConfigurableMarkdownPostProcessorOptions(
        ObjectMapper objectMapper,
        LlmMarkdownConfigRepository configRepository,
        MarkdownPostProcessor fallbackProcessor,
        Map<String, String> environmentValues,
        ConfigurableMarkdownRuntimeOptions runtimeOptions
) {

    /**
     * 规整可选字段。
     *
     * @author lvdaxianerplus
     * @date 2026-06-20
     */
    public ConfigurableMarkdownPostProcessorOptions {
        environmentValues = environmentValues == null ? System.getenv() : Map.copyOf(environmentValues);
        runtimeOptions = Objects.requireNonNull(runtimeOptions, "markdown runtime options is required");
    }
}
