package io.github.lvdaxianer.doclens.j.processing.domain;

import java.util.List;
import java.util.Optional;

/**
 * 空回调任务仓储。
 *
 * @author lvdaxianerplus
 * @date 2026-06-15
 */
public final class EmptyCallbackJobRepository implements CallbackJobRepository {

    /**
     * 保存回调任务。
     *
     * @param job 回调任务
     * @author lvdaxianerplus
     * @date 2026-06-15
     */
    @Override
    public void save(CallbackJob job) {
        // 空仓储不持久化回调任务。
    }

    /**
     * 根据 ID 查找回调任务。
     *
     * @param callbackJobId 回调任务 ID
     * @return 空结果
     * @author lvdaxianerplus
     * @date 2026-06-15
     */
    @Override
    public Optional<CallbackJob> findById(String callbackJobId) {
        return Optional.empty();
    }

    /**
     * 查询待投递回调任务。
     *
     * @param limit 最大返回数量
     * @return 空集合
     * @author lvdaxianerplus
     * @date 2026-06-15
     */
    @Override
    public List<CallbackJob> listPending(int limit) {
        return List.of();
    }

    /**
     * 根据批次查询回调任务。
     *
     * @param batchId 批次 ID
     * @return 空集合
     * @author lvdaxianerplus
     * @date 2026-06-15
     */
    @Override
    public List<CallbackJob> listByBatchId(String batchId) {
        return List.of();
    }

    /**
     * 标记回调任务投递成功。
     *
     * @param callbackJobId 回调任务 ID
     * @author lvdaxianerplus
     * @date 2026-06-15
     */
    @Override
    public void markSucceeded(String callbackJobId) {
        // 空仓储不记录成功状态。
    }

    /**
     * 记录一次失败回调尝试。
     *
     * @param request 失败更新请求
     * @author lvdaxianerplus
     * @date 2026-06-15
     */
    @Override
    public void markFailed(CallbackJobFailureRequest request) {
        // 空仓储不记录失败状态。
    }
}
