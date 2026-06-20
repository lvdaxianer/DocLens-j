package io.github.lvdaxianer.doclens.j.processing.application;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

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
    private static final String SHA_256 = "SHA-256";
    private static final String FINGERPRINT_SEPARATOR = "\n";

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

    /**
     * 基于请求与分片计划创建 checkpoint 计划。
     *
     * @param source checkpoint 计划来源
     * @return checkpoint 计划
     * @author lvdaxianerplus
     * @date 2026-06-20
     */
    public static MarkdownChunkCheckpointPlan from(MarkdownChunkCheckpointPlanSource source) {
        return new MarkdownChunkCheckpointPlan(source.request().documentId(), source.request().fileName(),
                source.plan().chunks().size(), source.request().chunkStrategy(), source.maxContextTokens(),
                fingerprint(source));
    }

    /**
     * 计算分片计划指纹。
     *
     * @param source checkpoint 计划来源
     * @return 分片计划指纹
     * @author lvdaxianerplus
     * @date 2026-06-20
     */
    private static String fingerprint(MarkdownChunkCheckpointPlanSource source) {
        String fingerprintSource = String.join(FINGERPRINT_SEPARATOR, source.request().documentId(),
                source.request().fileName(), source.request().chunkStrategy().name(),
                String.valueOf(source.maxContextTokens()), String.valueOf(source.plan().chunks().size()),
                source.request().ocrText());
        return sha256(fingerprintSource);
    }

    /**
     * 计算 SHA-256 摘要。
     *
     * @param value 原始文本
     * @return 十六进制摘要
     * @author lvdaxianerplus
     * @date 2026-06-20
     */
    private static String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance(SHA_256);
            return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("sha-256 digest unavailable", ex);
        }
    }
}
