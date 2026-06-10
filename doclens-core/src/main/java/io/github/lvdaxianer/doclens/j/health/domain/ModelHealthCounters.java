package io.github.lvdaxianer.doclens.j.health.domain;

/**
 * 模型健康连续计数。
 *
 * @param consecutiveFailures 连续失败次数
 * @param consecutiveSuccesses 连续成功次数
 * @author lvdaxianerplus
 * @date 2026-06-10
 */
public record ModelHealthCounters(
        long consecutiveFailures,
        long consecutiveSuccesses
) {

    /**
     * 创建模型健康连续计数。
     *
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    public ModelHealthCounters {
        consecutiveFailures = Math.max(0L, consecutiveFailures);
        consecutiveSuccesses = Math.max(0L, consecutiveSuccesses);
    }

    /**
     * 创建空计数。
     *
     * @return 空计数
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    public static ModelHealthCounters empty() {
        return new ModelHealthCounters(0L, 0L);
    }

    /**
     * 创建成功计数。
     *
     * @param successes 连续成功次数
     * @return 成功计数
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    public static ModelHealthCounters successes(long successes) {
        return new ModelHealthCounters(0L, successes);
    }

    /**
     * 创建失败计数。
     *
     * @param failures 连续失败次数
     * @return 失败计数
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    public static ModelHealthCounters failures(long failures) {
        return new ModelHealthCounters(failures, 0L);
    }
}
