package io.github.lvdaxianer.doclens.j.processing.application;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/**
 * 近似 Token 估算器测试。
 *
 * @author lvdaxianerplus
 * @date 2026-06-13
 */
class ApproximateTokenEstimatorTest {

    /**
     * 中文文本应按保守方式估算 Token。
     *
     * @author lvdaxianerplus
     * @date 2026-06-13
     */
    @Test
    void estimatesCjkTextConservatively() {
        TokenEstimator estimator = new ApproximateTokenEstimator();

        assertThat(estimator.estimate("中文内容")).isGreaterThanOrEqualTo(4);
    }

    /**
     * 空白文本应估算为零。
     *
     * @author lvdaxianerplus
     * @date 2026-06-13
     */
    @Test
    void estimatesBlankTextAsZero() {
        TokenEstimator estimator = new ApproximateTokenEstimator();

        assertThat(estimator.estimate("  \n ")).isZero();
    }
}
