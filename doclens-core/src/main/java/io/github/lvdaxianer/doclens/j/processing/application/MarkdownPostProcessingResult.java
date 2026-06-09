package io.github.lvdaxianer.doclens.j.processing.application;

import java.util.List;

/**
 * Markdown 后处理结果。
 *
 * @param markdown Markdown 文本
 * @param warnings 后处理警告
 * @param markdownApplied 是否实际应用了 LLM Markdown 排版
 * @author lvdaxianerplus
 * @date 2026-06-09
 */
public record MarkdownPostProcessingResult(String markdown, List<String> warnings, boolean markdownApplied) {

    /**
     * 创建成功的 Markdown 后处理结果。
     *
     * @param markdown Markdown 文本
     * @return Markdown 后处理结果
     * @author lvdaxianerplus
     * @date 2026-06-09
     */
    public static MarkdownPostProcessingResult markdown(String markdown) {
        return new MarkdownPostProcessingResult(markdown, List.of(), true);
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
        return new MarkdownPostProcessingResult(markdown, List.of(), false);
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
    }
}
