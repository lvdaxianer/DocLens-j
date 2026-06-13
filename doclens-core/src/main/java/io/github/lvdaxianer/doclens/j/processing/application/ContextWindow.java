package io.github.lvdaxianer.doclens.j.processing.application;

/**
 * Markdown 分片上下文窗口。
 *
 * @param text 原始文本
 * @param anchor 主内容边界位置
 * @param overlapTokens 重叠 Token 预算
 * @author lvdaxianerplus
 * @date 2026-06-13
 */
record ContextWindow(String text, int anchor, int overlapTokens) {
}
