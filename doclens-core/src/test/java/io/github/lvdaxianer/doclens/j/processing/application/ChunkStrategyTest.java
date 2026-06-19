package io.github.lvdaxianer.doclens.j.processing.application;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/**
 * Markdown 分块策略测试。
 *
 * @author lvdaxianerplus
 * @date 2026-06-19
 */
class ChunkStrategyTest {

    /**
     * 空策略文本应回退到通用策略。
     *
     * @author lvdaxianerplus
     * @date 2026-06-19
     */
    @Test
    void defaultsBlankTextToGeneral() {
        assertThat(ChunkStrategy.from(null)).isEqualTo(ChunkStrategy.GENERAL);
    }
}
