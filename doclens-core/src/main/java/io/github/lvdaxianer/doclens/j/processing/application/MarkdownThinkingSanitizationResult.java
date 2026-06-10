package io.github.lvdaxianer.doclens.j.processing.application;

/**
 * Markdown 思考过程清洗结果。
 *
 * @param markdown 清洗后的 Markdown
 * @param fallbackToOcrText 是否回退 OCR 原文
 * @author lvdaxianerplus
 * @date 2026-06-11
 */
record MarkdownThinkingSanitizationResult(String markdown, boolean fallbackToOcrText) {

    /**
     * 创建可继续使用的 Markdown 结果。
     *
     * @param markdown 清洗后的 Markdown
     * @return 清洗结果
     * @author lvdaxianerplus
     * @date 2026-06-11
     */
    static MarkdownThinkingSanitizationResult markdown(String markdown) {
        return new MarkdownThinkingSanitizationResult(markdown, false);
    }

    /**
     * 创建需要回退 OCR 原文的结果。
     *
     * @return 清洗结果
     * @author lvdaxianerplus
     * @date 2026-06-11
     */
    static MarkdownThinkingSanitizationResult fallback() {
        return new MarkdownThinkingSanitizationResult("", true);
    }
}
