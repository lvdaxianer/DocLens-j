package io.github.lvdaxianer.doclens.j.processing.application;

/**
 * Markdown 分片计划上下文。
 *
 * @param text 原始文本
 * @param estimatedInputTokens 输入估算 Token 数
 * @param contentBudgetTokens 主内容 Token 预算
 * @param overlapTokens 上下文重叠 Token 预算
 * @author lvdaxianerplus
 * @date 2026-06-13
 */
record ChunkPlanContext(String text, int estimatedInputTokens, int contentBudgetTokens, int overlapTokens) {
}
