package io.github.lvdaxianer.doclens.j.processing.application;

import java.util.List;

/**
 * Markdown 后处理结果。
 *
 * @param markdown Markdown 文本
 * @param warnings 后处理警告
 * @author lvdaxianerplus
 * @date 2026-06-09
 */
public record MarkdownPostProcessingResult(String markdown, List<String> warnings) {

    /**
     * 创建成功的 Markdown 后处理结果。
     *
     * @param markdown Markdown 文本
     * @return Markdown 后处理结果
     * @author lvdaxianerplus
     * @date 2026-06-09
     */
    public static MarkdownPostProcessingResult markdown(String markdown) {
        return new MarkdownPostProcessingResult(markdown, List.of());
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
