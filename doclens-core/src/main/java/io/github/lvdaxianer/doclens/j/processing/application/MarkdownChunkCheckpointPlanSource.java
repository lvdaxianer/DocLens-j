package io.github.lvdaxianer.doclens.j.processing.application;

/**
 * Markdown chunk checkpoint 计划来源。
 *
 * @param request Markdown 后处理请求
 * @param plan Markdown 分片计划
 * @param maxContextTokens 最大上下文 Token 数
 * @author lvdaxianerplus
 * @date 2026-06-20
 */
public record MarkdownChunkCheckpointPlanSource(
        MarkdownPostProcessingRequest request,
        MarkdownChunkPlan plan,
        int maxContextTokens
) {

    /**
     * 规整计划来源字段。
     *
     * @author lvdaxianerplus
     * @date 2026-06-20
     */
    public MarkdownChunkCheckpointPlanSource {
        // 请求缺失时无法获得文档身份，直接拒绝。
        if (request == null) {
            throw new IllegalArgumentException("markdown post processing request must not be null");
        // 分片计划缺失时无法获得 chunk 数量，直接拒绝。
        } else if (plan == null) {
            throw new IllegalArgumentException("markdown chunk plan must not be null");
        }
    }
}
