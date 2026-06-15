package io.github.lvdaxianer.doclens.j.processing.infrastructure;

import io.github.lvdaxianer.doclens.j.processing.domain.CallbackJob;
import io.github.lvdaxianer.doclens.j.processing.domain.CallbackJobFailureRequest;
import io.github.lvdaxianer.doclens.j.processing.domain.CallbackJobRepository;
import io.github.lvdaxianer.doclens.j.processing.domain.CallbackJobStatus;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * 回调任务内存仓储。
 *
 * @author lvdaxianerplus
 * @date 2026-06-15
 */
final class InMemoryCallbackJobRepository implements CallbackJobRepository {

    private static final OffsetDateTime BASE_TIME = OffsetDateTime.parse("2026-06-15T10:00:00+08:00");

    private final List<CallbackJob> jobs = new ArrayList<>(1);

    /**
     * 保存回调任务。
     *
     * @param job 回调任务
     * @author lvdaxianerplus
     * @date 2026-06-15
     */
    @Override
    public void save(CallbackJob job) {
        jobs.add(job);
    }

    /**
     * 根据 ID 查询回调任务。
     *
     * @param callbackJobId 回调任务 ID
     * @return 回调任务
     * @author lvdaxianerplus
     * @date 2026-06-15
     */
    @Override
    public Optional<CallbackJob> findById(String callbackJobId) {
        return jobs.stream().filter(job -> job.callbackJobId().equals(callbackJobId)).findFirst();
    }

    /**
     * 查询待投递任务。
     *
     * @param limit 最大数量
     * @return 待投递任务
     * @author lvdaxianerplus
     * @date 2026-06-15
     */
    @Override
    public List<CallbackJob> listPending(int limit) {
        return jobs.stream().filter(job -> job.status() == CallbackJobStatus.PENDING).limit(limit).toList();
    }

    /**
     * 标记任务投递成功。
     *
     * @param callbackJobId 回调任务 ID
     * @author lvdaxianerplus
     * @date 2026-06-15
     */
    @Override
    public void markSucceeded(String callbackJobId) {
        CallbackJob job = findById(callbackJobId).orElseThrow();
        jobs.set(jobs.indexOf(job), job.markSucceeded(BASE_TIME.plusSeconds(1)));
    }

    /**
     * 标记任务投递失败。
     *
     * @param request 失败更新请求
     * @author lvdaxianerplus
     * @date 2026-06-15
     */
    @Override
    public void markFailed(CallbackJobFailureRequest request) {
        CallbackJob job = findById(request.callbackJobId()).orElseThrow();
        jobs.set(jobs.indexOf(job), job.markFailed(request));
    }
}
