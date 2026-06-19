package io.github.lvdaxianer.doclens.j.processing.application;

/**
 * Markdown 滑动窗口分块策略。
 *
 * @author lvdaxianerplus
 * @date 2026-06-19
 */
public enum ChunkStrategy {
    GENERAL(400, 80),
    NEWS(300, 60),
    TECHNICAL(500, 100),
    ACADEMIC(800, 150);

    private static final ChunkStrategy DEFAULT = GENERAL;

    private final int chunkSize;
    private final int overlapSize;

    /**
     * 创建分块策略。
     *
     * @param chunkSize 主窗口大小
     * @param overlapSize 重叠大小
     * @author lvdaxianerplus
     * @date 2026-06-19
     */
    ChunkStrategy(int chunkSize, int overlapSize) {
        this.chunkSize = chunkSize;
        this.overlapSize = overlapSize;
    }

    /**
     * 获取主窗口大小。
     *
     * @return 主窗口大小
     * @author lvdaxianerplus
     * @date 2026-06-19
     */
    public int chunkSize() {
        return chunkSize;
    }

    /**
     * 获取重叠大小。
     *
     * @return 重叠大小
     * @author lvdaxianerplus
     * @date 2026-06-19
     */
    public int overlapSize() {
        return overlapSize;
    }

    /**
     * 获取默认分块策略。
     *
     * @return 默认分块策略
     * @author lvdaxianerplus
     * @date 2026-06-19
     */
    public static ChunkStrategy general() {
        return DEFAULT;
    }

    /**
     * 解析策略文本。
     *
     * @param value 策略文本
     * @return 分块策略
     * @author lvdaxianerplus
     * @date 2026-06-19
     */
    public static ChunkStrategy from(String value) {
        if (value == null || value.isBlank()) {
            // 未传入预设时回退到通用分块，保持现有默认行为。
            return DEFAULT;
        } else {
            // 显式传入预设时按枚举名称解析对应窗口配置。
            return valueOf(value.trim().toUpperCase());
        }
    }
}
