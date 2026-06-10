package io.github.lvdaxianer.doclens.j.processing.application;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Markdown 后处理思考过程清洗器。
 *
 * @author lvdaxianerplus
 * @date 2026-06-11
 */
final class MarkdownThinkingSanitizer {

    private static final Pattern PAIRED_THINK_BLOCK_PATTERN =
            Pattern.compile("(?is)<\\s*think\\b[^>]*>.*?<\\s*/\\s*think\\s*>");
    private static final Pattern LEADING_THINK_TAG_PATTERN = Pattern.compile("(?is)^\\s*<\\s*think\\b[^>]*>");

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
        if (startsWithThinkTag(cleanedMarkdown)) {
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
        Matcher matcher = PAIRED_THINK_BLOCK_PATTERN.matcher(markdown);
        return matcher.replaceAll("");
    }

    /**
     * 判断 Markdown 是否以未闭合思考标签开头。
     *
     * @param markdown 已移除成对思考块的 Markdown
     * @return 是否需要回退 OCR 原文
     * @author lvdaxianerplus
     * @date 2026-06-11
     */
    private static boolean startsWithThinkTag(String markdown) {
        Matcher matcher = LEADING_THINK_TAG_PATTERN.matcher(markdown);
        return matcher.find();
    }
}
