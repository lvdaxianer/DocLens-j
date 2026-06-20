package io.github.lvdaxianer.doclens.j.processing.infrastructure;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.lvdaxianer.doclens.j.processing.application.MarkdownChunk;
import io.github.lvdaxianer.doclens.j.processing.application.MarkdownChunkCheckpoint;
import io.github.lvdaxianer.doclens.j.processing.application.MarkdownChunkCheckpointPlan;
import io.github.lvdaxianer.doclens.j.processing.application.MarkdownChunkCheckpointStore;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.OffsetDateTime;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 基于文件系统的 Markdown chunk 检查点存储。
 *
 * @author lvdaxianerplus
 * @date 2026-06-20
 */
public class FileSystemMarkdownChunkCheckpointStore implements MarkdownChunkCheckpointStore {

    private static final Logger LOGGER = LoggerFactory.getLogger(FileSystemMarkdownChunkCheckpointStore.class);
    private static final String ROOT_DIR = "llm-markdown-chunks";
    private static final String MANIFEST_FILE = "manifest.json";
    private static final String MARKDOWN_FILE = "README.md";
    private static final String META_FILE = "meta.json";
    private static final String SHA_256 = "SHA-256";
    private static final int MIN_CHUNK_NO_WIDTH = 2;
    private static final TypeReference<Map<String, Object>> JSON_MAP_TYPE = new TypeReference<>() {
    };

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
        Path chunkDir = chunkDir(plan, chunk);
        Path markdownFile = chunkDir.resolve(MARKDOWN_FILE);
        // README 存在时读取 checkpoint 内容。
        if (Files.isRegularFile(markdownFile)) {
            try {
                MarkdownChunkCheckpointCandidate candidate = new MarkdownChunkCheckpointCandidate(plan, chunk,
                        chunkDir, Files.readString(markdownFile, StandardCharsets.UTF_8));
                return validCheckpoint(candidate) ? Optional.of(candidate.markdown()) : Optional.empty();
            } catch (IOException ex) {
                throw new IllegalStateException("failed to load markdown chunk checkpoint", ex);
            }
        } else {
            // README 不存在时视为该 chunk 尚未完成。
            return Optional.empty();
        }
    }

    /**
     * 删除指定文档的全部 chunk 检查点目录。
     *
     * @param documentId 文档 ID
     * @author lvdaxianerplus
     * @date 2026-06-20
     */
    @Override
    public void deleteByDocumentId(String documentId) {
        Path documentDir = resolveCheckpointPath(documentId);
        // checkpoint 目录存在时才执行递归删除。
        if (Files.exists(documentDir)) {
            deleteExistingDirectory(documentDir);
        } else {
            // 没有 checkpoint 残留时删除动作保持幂等。
        }
    }

    /**
     * 批量删除指定文档的全部 chunk 检查点目录。
     *
     * @param documentIds 文档 ID 集合
     * @author lvdaxianerplus
     * @date 2026-06-20
     */
    @Override
    public void deleteByDocumentIds(List<String> documentIds) {
        documentIds.forEach(this::deleteByDocumentId);
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

    /**
     * 判断 checkpoint 候选是否完整且匹配当前计划。
     *
     * @param candidate checkpoint 候选
     * @return 是否可复用
     * @throws IOException checkpoint 元数据读取失败
     * @author lvdaxianerplus
     * @date 2026-06-20
     */
    private boolean validCheckpoint(MarkdownChunkCheckpointCandidate candidate) throws IOException {
        return !candidate.markdown().isBlank() && manifestMatches(candidate.plan()) && metadataMatches(candidate);
    }

    /**
     * 判断 manifest 是否匹配当前计划身份。
     *
     * @param plan 当前 checkpoint 计划
     * @return manifest 是否匹配
     * @throws IOException manifest 读取失败
     * @author lvdaxianerplus
     * @date 2026-06-20
     */
    private boolean manifestMatches(MarkdownChunkCheckpointPlan plan) throws IOException {
        Path manifestFile = documentDir(plan).resolve(MANIFEST_FILE);
        // manifest 存在时校验计划身份。
        if (Files.isRegularFile(manifestFile)) {
            Map<String, Object> manifest = readJson(manifestFile);
            return plan.documentId().equals(manifest.get("documentId"))
                    && Integer.valueOf(plan.chunkCount()).equals(manifest.get("chunkCount"))
                    && plan.planFingerprint().equals(manifest.get("planFingerprint"));
        } else {
            // manifest 不存在时不能证明 checkpoint 属于当前计划。
            return false;
        }
    }

    /**
     * 判断 chunk 元数据是否匹配当前 chunk 与 README 内容。
     *
     * @param candidate checkpoint 候选
     * @return 元数据是否匹配
     * @throws IOException 元数据读取失败
     * @author lvdaxianerplus
     * @date 2026-06-20
     */
    private boolean metadataMatches(MarkdownChunkCheckpointCandidate candidate) throws IOException {
        Path metaFile = candidate.chunkDir().resolve(META_FILE);
        // meta 存在时校验 chunk 编号与 README 摘要。
        if (Files.isRegularFile(metaFile)) {
            Map<String, Object> metadata = readJson(metaFile);
            return Integer.valueOf(candidate.chunk().chunkIndex()).equals(metadata.get("chunkIndex"))
                    && chunkNo(candidate.plan(), candidate.chunk()).equals(metadata.get("chunkNo"))
                    && sha256(candidate.markdown()).equals(metadata.get("sha256"));
        } else {
            // meta 不存在时不能证明 README 是完整 chunk 输出。
            return false;
        }
    }

    /**
     * 读取 JSON 对象。
     *
     * @param file JSON 文件
     * @return JSON 映射
     * @throws IOException JSON 读取失败
     * @author lvdaxianerplus
     * @date 2026-06-20
     */
    private Map<String, Object> readJson(Path file) throws IOException {
        return objectMapper.readValue(file.toFile(), JSON_MAP_TYPE);
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
     * 递归删除已存在的 checkpoint 目录。
     *
     * @param documentDir 文档 checkpoint 目录
     * @author lvdaxianerplus
     * @date 2026-06-20
     */
    private void deleteExistingDirectory(Path documentDir) {
        try (Stream<Path> paths = Files.walk(documentDir)) {
            List<Path> orderedPaths = paths.sorted(Comparator.reverseOrder()).toList();
            for (Path path : orderedPaths) {
                Files.deleteIfExists(path);
            }
        } catch (IOException ex) {
            LOGGER.warn("[MarkdownCheckpoint] 删除 chunk checkpoint 目录失败, documentDir={}", documentDir, ex);
            throw new IllegalStateException("failed to delete markdown chunk checkpoint", ex);
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

    /**
     * checkpoint 读取候选。
     *
     * @param plan checkpoint 计划
     * @param chunk Markdown chunk
     * @param chunkDir chunk 目录
     * @param markdown README 内容
     * @author lvdaxianerplus
     * @date 2026-06-20
     */
    private record MarkdownChunkCheckpointCandidate(
            MarkdownChunkCheckpointPlan plan,
            MarkdownChunk chunk,
            Path chunkDir,
            String markdown
    ) {
    }
}
