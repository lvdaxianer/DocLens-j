package io.github.lvdaxianer.doclens.j.processing.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.lvdaxianer.doclens.j.processing.application.ChunkStrategy;
import io.github.lvdaxianer.doclens.j.processing.application.MarkdownChunk;
import io.github.lvdaxianer.doclens.j.processing.application.MarkdownChunkCheckpoint;
import io.github.lvdaxianer.doclens.j.processing.application.MarkdownChunkCheckpointPlan;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * 文件系统 Markdown chunk 检查点存储测试。
 *
 * @author lvdaxianerplus
 * @date 2026-06-20
 */
class FileSystemMarkdownChunkCheckpointStoreTest {

    @TempDir
    private Path storageRoot;

    /**
     * 成功分片应按文档和有序编号写入 manifest、README 与元数据。
     *
     * @throws Exception 测试文件读取失败
     * @author lvdaxianerplus
     * @date 2026-06-20
     */
    @Test
    void savesChunkCheckpointWithManifestMarkdownAndMetadata() throws Exception {
        FileSystemMarkdownChunkCheckpointStore store = new FileSystemMarkdownChunkCheckpointStore(storageRoot);
        MarkdownChunkCheckpointPlan plan = checkpointPlan("doc-1", 50);
        MarkdownChunk chunk = chunk(0, 50, "原始 chunk 01");

        store.save(checkpoint(plan, chunk, "整理后的 chunk 01"));

        Path documentDir = storageRoot.resolve("llm-markdown-chunks/doc-1");
        assertThat(Files.readString(documentDir.resolve("manifest.json")))
                .contains("\"documentId\":\"doc-1\"")
                .contains("\"chunkCount\":50")
                .contains("\"planFingerprint\":\"fingerprint-doc-1-50\"");
        assertThat(Files.readString(documentDir.resolve("01/README.md"))).isEqualTo("整理后的 chunk 01");
        assertThat(Files.readString(documentDir.resolve("01/meta.json")))
                .contains("\"chunkIndex\":0")
                .contains("\"chunkNo\":\"01\"")
                .contains("\"markdownApplied\":true");
    }

    /**
     * chunk 总数达到三位数时编号宽度应自动增长，保持目录排序稳定。
     *
     * @throws Exception 测试文件读取失败
     * @author lvdaxianerplus
     * @date 2026-06-20
     */
    @Test
    void padsChunkDirectoryWidthFromTotalChunkCount() throws Exception {
        FileSystemMarkdownChunkCheckpointStore store = new FileSystemMarkdownChunkCheckpointStore(storageRoot);
        MarkdownChunkCheckpointPlan plan = checkpointPlan("doc-2", 100);
        MarkdownChunk chunk = chunk(0, 100, "原始 chunk 001");

        store.save(checkpoint(plan, chunk, "整理后的 chunk 001"));

        Path documentDir = storageRoot.resolve("llm-markdown-chunks/doc-2");
        assertThat(Files.exists(documentDir.resolve("001/README.md"))).isTrue();
        assertThat(Files.exists(documentDir.resolve("01/README.md"))).isFalse();
    }

    /**
     * 读取 checkpoint 时应返回已保存的 Markdown。
     *
     * @author lvdaxianerplus
     * @date 2026-06-20
     */
    @Test
    void loadsSavedChunkCheckpoint() {
        FileSystemMarkdownChunkCheckpointStore store = new FileSystemMarkdownChunkCheckpointStore(storageRoot);
        MarkdownChunkCheckpointPlan plan = checkpointPlan("doc-3", 50);
        MarkdownChunk chunk = chunk(1, 50, "原始 chunk 02");
        store.save(checkpoint(plan, chunk, "整理后的 chunk 02"));

        Optional<String> checkpoint = store.load(plan, chunk);

        assertThat(checkpoint).contains("整理后的 chunk 02");
    }

    /**
     * 创建 checkpoint 计划。
     *
     * @param documentId 文档 ID
     * @param chunkCount chunk 总数
     * @return checkpoint 计划
     * @author lvdaxianerplus
     * @date 2026-06-20
     */
    private MarkdownChunkCheckpointPlan checkpointPlan(String documentId, int chunkCount) {
        return new MarkdownChunkCheckpointPlan(documentId, "demo.md", chunkCount, ChunkStrategy.general(),
                16000, "fingerprint-%s-%d".formatted(documentId, chunkCount));
    }

    /**
     * 创建测试分片。
     *
     * @param chunkIndex chunk 下标
     * @param totalChunks chunk 总数
     * @param mainContent 主内容
     * @return Markdown 分片
     * @author lvdaxianerplus
     * @date 2026-06-20
     */
    private MarkdownChunk chunk(int chunkIndex, int totalChunks, String mainContent) {
        return new MarkdownChunk(chunkIndex, totalChunks, "", mainContent, "", 100);
    }

    /**
     * 创建 checkpoint 内容。
     *
     * @param plan checkpoint 计划
     * @param chunk Markdown 分片
     * @param markdown Markdown 内容
     * @return checkpoint 内容
     * @author lvdaxianerplus
     * @date 2026-06-20
     */
    private MarkdownChunkCheckpoint checkpoint(
            MarkdownChunkCheckpointPlan plan,
            MarkdownChunk chunk,
            String markdown
    ) {
        return new MarkdownChunkCheckpoint(plan, chunk, markdown, true);
    }
}
