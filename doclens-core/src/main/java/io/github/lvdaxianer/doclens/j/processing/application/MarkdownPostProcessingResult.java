package io.github.lvdaxianer.doclens.j.processing.application;

import java.util.List;
import java.util.Map;

/**
 * Markdown 后处理结果。
 *
 * @param markdown Markdown 文本
 * @param warnings 后处理警告
 * @param markdownApplied 是否实际应用了 LLM Markdown 排版
 * @param metadata 后处理观测元数据
 * @author lvdaxianerplus
 * @date 2026-06-09
 */
public record MarkdownPostProcessingResult(
        String markdown,
        List<String> warnings,
        boolean markdownApplied,
        Map<String, Object> metadata
) {

    /**
     * 创建成功的 Markdown 后处理结果。
     *
     * @param markdown Markdown 文本
     * @return Markdown 后处理结果
     * @author lvdaxianerplus
     * @date 2026-06-09
     */
    public static MarkdownPostProcessingResult markdown(String markdown) {
        return new MarkdownPostProcessingResult(markdown, List.of(), true, Map.of());
    }

    /**
     * 创建带观测元数据的 Markdown 后处理结果。
     *
     * @param markdown Markdown 文本
     * @param metadata 后处理观测元数据
     * @return Markdown 后处理结果
     * @author lvdaxianerplus
     * @date 2026-06-13
     */
    public static MarkdownPostProcessingResult markdown(String markdown, Map<String, Object> metadata) {
        return new MarkdownPostProcessingResult(markdown, List.of(), true, metadata);
    }

    /**
     * 创建未启用 LLM 的直通结果。
     *
     * @param markdown 直通文本
     * @return 未应用 LLM 的结果
     * @author lvdaxianerplus
     * @date 2026-06-09
     */
    public static MarkdownPostProcessingResult passthrough(String markdown) {
        return new MarkdownPostProcessingResult(markdown, List.of(), false, Map.of());
    }

    /**
     * 创建带原因的未启用 LLM 直通结果。
     *
     * @param markdown 直通文本
     * @param reason 直通原因
     * @return 未应用 LLM 的结果
     * @author lvdaxianerplus
     * @date 2026-06-12
     */
    public static MarkdownPostProcessingResult passthrough(String markdown, String reason) {
        return new MarkdownPostProcessingResult(markdown, List.of(reason), false, Map.of());
    }

    /**
     * 规整可空 Markdown 和警告集合。
     *
     * @author lvdaxianerplus
     * @date 2026-06-09
     */
    public MarkdownPostProcessingResult {
        markdown = markdown == null ? "" : markdown;
        warnings = warnings == null ? List.of() : List.copyOf(warnings);
        metadata = metadata == null ? Map.of() : Map.copyOf(metadata);
    }
}
