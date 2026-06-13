package io.github.lvdaxianer.doclens.j.processing.application;

/**
 * Markdown 单分片构建上下文。
 *
 * @param text 原始文本
 * @param range 主内容范围
 * @param index 分片序号
 * @param total 分片总数
 * @param overlapTokens 上下文重叠 Token 预算
 * @author lvdaxianerplus
 * @date 2026-06-13
 */
record ChunkBuildContext(
        String text,
        Range range,
        int index,
        int total,
        int overlapTokens
) {
}
