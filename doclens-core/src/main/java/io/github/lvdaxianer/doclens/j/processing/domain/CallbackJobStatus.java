package io.github.lvdaxianer.doclens.j.processing.domain;

/**
 * 回调任务状态。
 *
 * @author lvdaxianerplus
 * @date 2026-06-15
 */
public enum CallbackJobStatus {
    PENDING,
    SENDING,
    RETRYING,
    SUCCESS,
    FAILED;

    /**
     * 判断是否为终态。
     *
     * @return 是否为终态
     * @author lvdaxianerplus
     * @date 2026-06-15
     */
    public boolean isTerminal() {
        return this == SUCCESS || this == FAILED;
    }
}
