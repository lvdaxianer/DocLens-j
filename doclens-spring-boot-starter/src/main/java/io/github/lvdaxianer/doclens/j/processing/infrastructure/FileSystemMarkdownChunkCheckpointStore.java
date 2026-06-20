package io.github.lvdaxianer.doclens.j.processing.infrastructure;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.lvdaxianer.doclens.j.processing.application.MarkdownChunk;
import io.github.lvdaxianer.doclens.j.processing.application.MarkdownChunkCheckpoint;
import io.github.lvdaxianer.doclens.j.processing.application.MarkdownChunkCheckpointPlan;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.OffsetDateTime;
import java.util.HexFormat;
import java.util.Map;
import java.util.Optional;

/**
 * 基于文件系统的 Markdown chunk 检查点存储。
 *
 * @author lvdaxianerplus
 * @date 2026-06-20
 */
public class FileSystemMarkdownChunkCheckpointStore {

    private static final String ROOT_DIR = "llm-markdown-chunks";
    private static final String MANIFEST_FILE = "manifest.json";
    private static final String MARKDOWN_FILE = "README.md";
    private static final String META_FILE = "meta.json";
    private static final String SHA_256 = "SHA-256";
    private static final int MIN_CHUNK_NO_WIDTH = 2;

    private final Path storageRoot;
    private final ObjectMapper objectMapper;

    /**
     * 创建文件系统 checkpoint 存储。
     *
     * @param storageRoot 存储根目录
     * @author lvdaxianerplus
     * @date 2026-06-20
     */
    public FileSystemMarkdownChunkCheckpointStore(Path storageRoot) {
        this(storageRoot, new ObjectMapper());
    }

    /**
     * 创建文件系统 checkpoint 存储。
     *
     * @param storageRoot 存储根目录
     * @param objectMapper JSON 映射器
     * @author lvdaxianerplus
     * @date 2026-06-20
     */
    FileSystemMarkdownChunkCheckpointStore(Path storageRoot, ObjectMapper objectMapper) {
        this.storageRoot = storageRoot.toAbsolutePath().normalize();
        this.objectMapper = objectMapper;
    }

    /**
     * 保存 chunk 检查点。
     *
     * @param checkpoint chunk checkpoint 内容
     * @author lvdaxianerplus
     * @date 2026-06-20
     */
    public void save(MarkdownChunkCheckpoint checkpoint) {
        try {
            Path documentDir = documentDir(checkpoint.plan());
            Path chunkDir = chunkDir(checkpoint.plan(), checkpoint.chunk());
            Files.createDirectories(chunkDir);
            writeJsonAtomically(documentDir.resolve(MANIFEST_FILE), manifest(checkpoint.plan()));
            writeTextAtomically(chunkDir.resolve(MARKDOWN_FILE), checkpoint.markdown());
            writeJsonAtomically(chunkDir.resolve(META_FILE), metadata(checkpoint));
        } catch (IOException ex) {
            throw new IllegalStateException("failed to save markdown chunk checkpoint", ex);
        }
    }

    /**
     * 读取 chunk 检查点 Markdown。
     *
     * @param plan checkpoint 计划
     * @param chunk Markdown chunk
     * @return 已保存 Markdown
     * @author lvdaxianerplus
     * @date 2026-06-20
     */
    public Optional<String> load(MarkdownChunkCheckpointPlan plan, MarkdownChunk chunk) {
        Path markdownFile = chunkDir(plan, chunk).resolve(MARKDOWN_FILE);
        // README 存在时读取 checkpoint 内容。
        if (Files.isRegularFile(markdownFile)) {
            try {
                String markdown = Files.readString(markdownFile, StandardCharsets.UTF_8);
                return markdown.isBlank() ? Optional.empty() : Optional.of(markdown);
            } catch (IOException ex) {
                throw new IllegalStateException("failed to load markdown chunk checkpoint", ex);
            }
        } else {
            // README 不存在时视为该 chunk 尚未完成。
            return Optional.empty();
        }
    }

    private Map<String, Object> manifest(MarkdownChunkCheckpointPlan plan) {
        return Map.of(
                "documentId", plan.documentId(),
                "fileName", plan.fileName(),
                "chunkCount", plan.chunkCount(),
                "chunkStrategy", plan.chunkStrategy().name(),
                "maxContextTokens", plan.maxContextTokens(),
                "planFingerprint", plan.planFingerprint());
    }

    private Map<String, Object> metadata(MarkdownChunkCheckpoint checkpoint) {
        return Map.of(
                "documentId", checkpoint.plan().documentId(),
                "chunkIndex", checkpoint.chunk().chunkIndex(),
                "chunkNo", chunkNo(checkpoint.plan(), checkpoint.chunk()),
                "totalChunks", checkpoint.chunk().totalChunks(),
                "markdownApplied", checkpoint.markdownApplied(),
                "sha256", sha256(checkpoint.markdown()),
                "createdAt", OffsetDateTime.now().toString());
    }

    private Path documentDir(MarkdownChunkCheckpointPlan plan) {
        return resolveCheckpointPath(plan.documentId());
    }

    private Path chunkDir(MarkdownChunkCheckpointPlan plan, MarkdownChunk chunk) {
        return documentDir(plan).resolve(chunkNo(plan, chunk));
    }

    private String chunkNo(MarkdownChunkCheckpointPlan plan, MarkdownChunk chunk) {
        int width = Math.max(MIN_CHUNK_NO_WIDTH, String.valueOf(plan.chunkCount()).length());
        return String.format("%0" + width + "d", chunk.chunkIndex() + 1);
    }

    private void writeJsonAtomically(Path target, Map<String, Object> value) throws IOException {
        writeTextAtomically(target, objectMapper.writeValueAsString(value));
    }

    private void writeTextAtomically(Path target, String value) throws IOException {
        Files.createDirectories(target.getParent());
        Path temp = target.resolveSibling(target.getFileName() + ".tmp");
        Files.writeString(temp, value, StandardCharsets.UTF_8);
        try {
            Files.move(temp, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } catch (AtomicMoveNotSupportedException ex) {
            Files.move(temp, target, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    /**
     * 解析 checkpoint 路径并防止逃逸 storageRoot。
     *
     * @param documentId 文档 ID
     * @return 文档 checkpoint 目录
     * @author lvdaxianerplus
     * @date 2026-06-20
     */
    private Path resolveCheckpointPath(String documentId) {
        Path checkpointRoot = storageRoot.resolve(ROOT_DIR).normalize();
        Path documentDir = checkpointRoot.resolve(documentId).normalize();
        // 解析后的目录仍在 checkpoint 根目录内时才允许访问。
        if (documentDir.startsWith(checkpointRoot)) {
            return documentDir;
        } else {
            throw new IllegalArgumentException("invalid checkpoint document id");
        }
    }

    private String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance(SHA_256);
            return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("sha-256 digest unavailable", ex);
        }
    }
}
