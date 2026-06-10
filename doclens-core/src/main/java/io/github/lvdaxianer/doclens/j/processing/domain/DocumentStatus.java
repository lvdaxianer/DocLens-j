package io.github.lvdaxianer.doclens.j.processing.domain;

/**
 * 文档任务生命周期状态。
 *
 * @author lvdaxianerplus
 * @date 2026-06-07
 */
public enum DocumentStatus {
    QUEUED,
    PROCESSING,
    STALLED,
    COMPLETED,
    FAILED;

    /**
     * 判断当前状态是否属于失败类终态。
     *
     * @return 是否失败类终态
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    public boolean isFailureLike() {
        if (this == FAILED) {
            return true;
        } else {
            return this == STALLED;
        }
    }
}
