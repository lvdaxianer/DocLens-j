package io.github.lvdaxianer.doclens.j.processing.application;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;

/**
 * Markdown 文本分片器测试。
 *
 * @author lvdaxianerplus
 * @date 2026-06-13
 */
class MarkdownChunkerTest {

    /**
     * 小文档应保持单个分片。
     *
     * @author lvdaxianerplus
     * @date 2026-06-13
     */
    @Test
    void keepsSmallDocumentAsSingleChunk() {
        MarkdownChunker chunker = new MarkdownChunker(new ApproximateTokenEstimator());

        MarkdownChunkPlan plan = chunker.plan("短文档", 16000);

        assertThat(plan.chunked()).isFalse();
        assertThat(plan.chunks()).hasSize(1);
        assertThat(plan.chunks().getFirst().mainContent()).isEqualTo("短文档");
    }

    /**
     * 大文档应按顺序拆分并提供上下文重叠。
     *
     * @author lvdaxianerplus
     * @date 2026-06-13
     */
    @Test
    void splitsLargeDocumentWithContextOnlyOverlap() {
        MarkdownChunker chunker = new MarkdownChunker(new ApproximateTokenEstimator());
        String text = "段落内容\n\n".repeat(5000);

        MarkdownChunkPlan plan = chunker.plan(text, 2000);

        assertThat(plan.chunked()).isTrue();
        assertThat(plan.chunks()).hasSizeGreaterThan(1);
        assertThat(plan.chunks().get(1).previousContext()).isNotBlank();
        assertThat(plan.chunks().getFirst().nextContext()).isNotBlank();
        assertThat(plan.chunks()).extracting(MarkdownChunk::chunkIndex)
                .containsExactlyElementsOf(IntStream.range(0, plan.chunks().size()).boxed().toList());
    }
}
