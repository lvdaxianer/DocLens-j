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

    /**
     * 不同分块策略应产出不同的窗口预算。
     *
     * @author lvdaxianerplus
     * @date 2026-06-19
     */
    @Test
    void appliesDifferentWindowBudgetsForDifferentStrategies() {
        MarkdownChunker chunker = new MarkdownChunker(new ApproximateTokenEstimator());
        String text = "技术内容".repeat(2000);

        MarkdownChunkPlan general = chunker.plan(text, 16000, ChunkStrategy.GENERAL);
        MarkdownChunkPlan academic = chunker.plan(text, 16000, ChunkStrategy.ACADEMIC);

        assertThat(general.contentBudgetTokens()).isEqualTo(400);
        assertThat(general.overlapTokens()).isEqualTo(80);
        assertThat(academic.contentBudgetTokens()).isEqualTo(800);
        assertThat(academic.overlapTokens()).isEqualTo(150);
    }
}
