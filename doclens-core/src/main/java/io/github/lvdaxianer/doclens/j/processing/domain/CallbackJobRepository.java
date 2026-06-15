package io.github.lvdaxianer.doclens.j.processing.domain;

import java.util.List;
import java.util.Optional;

/**
 * 回调任务仓储。
 *
 * @author lvdaxianerplus
 * @date 2026-06-15
 */
public interface CallbackJobRepository {

    /**
     * 保存回调任务。
     *
     * @param job 回调任务
     * @author lvdaxianerplus
     * @date 2026-06-15
     */
    void save(CallbackJob job);

    /**
     * 根据 ID 查找回调任务。
     *
     * @param callbackJobId 回调任务 ID
     * @return 可选回调任务
     * @author lvdaxianerplus
     * @date 2026-06-15
     */
    Optional<CallbackJob> findById(String callbackJobId);

    /**
     * 查询待投递的回调任务。
     *
     * @param limit 最大返回数量
     * @return 待投递回调任务
     * @author lvdaxianerplus
     * @date 2026-06-15
     */
    List<CallbackJob> listPending(int limit);

    /**
     * 标记回调任务投递成功。
     *
     * @param callbackJobId 回调任务 ID
     * @author lvdaxianerplus
     * @date 2026-06-15
     */
    void markSucceeded(String callbackJobId);

    /**
     * 记录一次失败回调尝试。
     *
     * @param request 失败更新请求
     * @author lvdaxianerplus
     * @date 2026-06-15
     */
    void markFailed(CallbackJobFailureRequest request);
}
