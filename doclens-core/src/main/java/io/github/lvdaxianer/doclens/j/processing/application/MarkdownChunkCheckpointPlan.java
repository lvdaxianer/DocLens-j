package io.github.lvdaxianer.doclens.j.processing.application;

/**
 * Markdown chunk 检查点计划身份。
 *
 * @param documentId 文档 ID
 * @param fileName 文件名
 * @param chunkCount chunk 总数
 * @param chunkStrategy 分块策略
 * @param maxContextTokens 最大上下文 Token 数
 * @param planFingerprint 分片计划指纹
 * @author lvdaxianerplus
 * @date 2026-06-20
 */
public record MarkdownChunkCheckpointPlan(
        String documentId,
        String fileName,
        int chunkCount,
        ChunkStrategy chunkStrategy,
        int maxContextTokens,
        String planFingerprint
) {

    /**
     * 规整 checkpoint 计划字段。
     *
     * @author lvdaxianerplus
     * @date 2026-06-20
     */
    public MarkdownChunkCheckpointPlan {
        documentId = documentId == null ? "" : documentId.trim();
        fileName = fileName == null ? "" : fileName;
        chunkStrategy = chunkStrategy == null ? ChunkStrategy.general() : chunkStrategy;
        planFingerprint = planFingerprint == null ? "" : planFingerprint.trim();
        // 文档 ID 是 checkpoint 根目录名，不能为空。
        if (documentId.isBlank()) {
            throw new IllegalArgumentException("document id must not be blank");
        }
        // chunk 总数用于编号宽度，必须是正数。
        if (chunkCount <= 0) {
            throw new IllegalArgumentException("chunk count must be greater than 0");
        }
        // 最大上下文用于区分分片计划，必须是正数。
        if (maxContextTokens <= 0) {
            throw new IllegalArgumentException("max context tokens must be greater than 0");
        }
        // 计划指纹用于拒绝旧 checkpoint，不能为空。
        if (planFingerprint.isBlank()) {
            throw new IllegalArgumentException("plan fingerprint must not be blank");
        }
    }
}
