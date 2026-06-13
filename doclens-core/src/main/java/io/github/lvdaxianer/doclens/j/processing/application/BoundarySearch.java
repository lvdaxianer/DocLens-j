package io.github.lvdaxianer.doclens.j.processing.application;

/**
 * Markdown 分片边界搜索参数。
 *
 * @param text 原始文本
 * @param boundary 边界标识
 * @param minimumEnd 最小结束位置
 * @param hardEnd 最大结束位置
 * @author lvdaxianerplus
 * @date 2026-06-13
 */
record BoundarySearch(String text, String boundary, int minimumEnd, int hardEnd) {
}
