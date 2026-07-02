package io.github.lvdaxianer.doclens.j.dashboard.interfaces;

import io.github.lvdaxianer.doclens.j.ingestion.domain.BatchRepository;
import io.github.lvdaxianer.doclens.j.ingestion.domain.CallerIdentity;
import io.github.lvdaxianer.doclens.j.processing.domain.CallbackJob;
import io.github.lvdaxianer.doclens.j.processing.domain.CallbackJobRepository;
import io.github.lvdaxianer.doclens.j.shared.domain.ResourceNotFoundException;
import org.springframework.stereotype.Component;

/**
 * Dashboard callback 重试分区授权器。
 *
 * @author lvdaxianer@yeah.net
 * @date 2026-07-02
 */
@Component
public class DashboardCallbackRetryAuthorizer {

    private static final String CALLBACK_JOB_NOT_FOUND_PREFIX = "callback job ";
    private static final String CALLBACK_JOB_NOT_FOUND_SUFFIX = " not found";

    private final CallbackJobRepository callbackJobRepository;
    private final BatchRepository batchRepository;

    /**
     * 创建 Dashboard callback 重试分区授权器。
     *
     * @param callbackJobRepository callback job 仓储
     * @param batchRepository batch 仓储
     * @author lvdaxianer@yeah.net
     * @date 2026-07-02
     */
    public DashboardCallbackRetryAuthorizer(
            CallbackJobRepository callbackJobRepository,
            BatchRepository batchRepository
    ) {
        this.callbackJobRepository = callbackJobRepository;
        this.batchRepository = batchRepository;
    }

    /**
     * 查找并校验当前 caller 可访问的 callback job。
     *
     * @param caller caller 身份
     * @param callbackJobId callback job ID
     * @return callback job
     * @author lvdaxianer@yeah.net
     * @date 2026-07-02
     */
    public CallbackJob authorize(CallerIdentity caller, String callbackJobId) {
        CallbackJob callbackJob = callbackJob(callbackJobId);
        assertOwnedByCaller(caller, callbackJob);
        return callbackJob;
    }

    /**
     * 查找 callback job。
     *
     * @param callbackJobId callback job ID
     * @return callback job
     * @author lvdaxianer@yeah.net
     * @date 2026-07-02
     */
    private CallbackJob callbackJob(String callbackJobId) {
        return callbackJobRepository.findById(callbackJobId)
                .orElseThrow(() -> notFound(callbackJobId));
    }

    /**
     * 校验 callback job 所属批次归属于当前 caller。
     *
     * @param caller caller 身份
     * @param callbackJob callback job
     * @author lvdaxianer@yeah.net
     * @date 2026-07-02
     */
    private void assertOwnedByCaller(CallerIdentity caller, CallbackJob callbackJob) {
        // 当前 caller 不拥有 callback job 所属 batch 时，按不存在处理避免泄露分区资源。
        if (batchRepository.findByIdForCaller(caller, callbackJob.batchId()).isEmpty()) {
            throw notFound(callbackJob.callbackJobId());
        }
    }

    /**
     * 创建 callback job 不存在异常。
     *
     * @param callbackJobId callback job ID
     * @return 资源不存在异常
     * @author lvdaxianer@yeah.net
     * @date 2026-07-02
     */
    private ResourceNotFoundException notFound(String callbackJobId) {
        return new ResourceNotFoundException(CALLBACK_JOB_NOT_FOUND_PREFIX + callbackJobId
                + CALLBACK_JOB_NOT_FOUND_SUFFIX);
    }
}
