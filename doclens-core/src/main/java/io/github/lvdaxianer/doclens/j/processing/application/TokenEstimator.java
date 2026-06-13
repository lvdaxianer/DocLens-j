package io.github.lvdaxianer.doclens.j.processing.application;

/**
 * 文本 Token 估算器。
 *
 * @author lvdaxianerplus
 * @date 2026-06-13
 */
public interface TokenEstimator {

    /**
     * 估算文本 Token 数。
     *
     * @param text 待估算文本
     * @return 估算 Token 数
     * @author lvdaxianerplus
     * @date 2026-06-13
     */
    int estimate(String text);
}
