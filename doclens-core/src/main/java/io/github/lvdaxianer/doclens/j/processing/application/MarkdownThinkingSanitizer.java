package io.github.lvdaxianer.doclens.j.processing.application;

/**
 * Markdown 后处理思考过程清洗器。
 *
 * @author lvdaxianerplus
 * @date 2026-06-11
 */
final class MarkdownThinkingSanitizer {

    private static final String THINK_OPEN_TAG = "<think>";
    private static final String THINK_CLOSE_TAG = "</think>";

    /**
     * 禁止创建工具类实例。
     *
     * @author lvdaxianerplus
     * @date 2026-06-11
     */
    private MarkdownThinkingSanitizer() {
    }

    /**
     * 清洗 Markdown 中的模型思考过程。
     *
     * @param markdown LLM 返回 Markdown
     * @return 清洗结果
     * @author lvdaxianerplus
     * @date 2026-06-11
     */
    static MarkdownThinkingSanitizationResult sanitize(String markdown) {
        String normalizedMarkdown = markdown == null ? "" : markdown.strip();
        String cleanedMarkdown = removePairedThinkBlocks(normalizedMarkdown).strip();
        if (cleanedMarkdown.startsWith(THINK_OPEN_TAG)) {
            // 未闭合 think 通常表示模型只输出了推理内容，不能作为最终 Markdown 保存。
            return MarkdownThinkingSanitizationResult.fallback();
        } else {
            // 成对 think 已清除或原文不包含 think，保留可展示 Markdown。
            return MarkdownThinkingSanitizationResult.markdown(cleanedMarkdown);
        }
    }

    /**
     * 删除成对 think 标签及其内部内容。
     *
     * @param markdown 原始 Markdown
     * @return 删除成对 think 后的 Markdown
     * @author lvdaxianerplus
     * @date 2026-06-11
     */
    private static String removePairedThinkBlocks(String markdown) {
        String cleanedMarkdown = markdown;
        int openIndex = cleanedMarkdown.indexOf(THINK_OPEN_TAG);
        while (openIndex >= 0) {
            int closeIndex = cleanedMarkdown.indexOf(THINK_CLOSE_TAG, openIndex + THINK_OPEN_TAG.length());
            if (closeIndex < 0) {
                return cleanedMarkdown;
            } else {
                cleanedMarkdown = cleanedMarkdown.substring(0, openIndex)
                        + cleanedMarkdown.substring(closeIndex + THINK_CLOSE_TAG.length());
                openIndex = cleanedMarkdown.indexOf(THINK_OPEN_TAG);
            }
        }
        return cleanedMarkdown;
    }
}
