package io.github.lvdaxianer.doclens.j.processing.application;

import java.util.List;

/**
 * Markdown 分片范围构建上下文。
 *
 * @param text 原始文本
 * @param ranges 主内容范围列表
 * @param overlapTokens 上下文重叠 Token 预算
 * @author lvdaxianerplus
 * @date 2026-06-13
 */
record RangeBuildContext(String text, List<Range> ranges, int overlapTokens) {
}
