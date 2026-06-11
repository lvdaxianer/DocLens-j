package io.github.lvdaxianer.doclens.j.processing.application;

import java.util.List;
import java.util.Optional;

/**
 * Markdown 后处理后的文本和警告。
 *
 * @param finalText 最终文本
 * @param warnings 警告集合
 * @param llmMarkdownApplied 是否应用了 LLM Markdown
 * @param llmErrorMessage LLM 错误消息
 * @author lvdaxianerplus
 * @date 2026-06-11
 */
record DocumentPostProcessedText(
        String finalText,
        List<String> warnings,
        boolean llmMarkdownApplied,
        Optional<String> llmErrorMessage
) {
}
