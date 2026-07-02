package io.github.lvdaxianer.doclens.j.dashboard.interfaces;

import io.github.lvdaxianer.doclens.j.ingestion.domain.CallerIdentity;
import io.github.lvdaxianer.doclens.j.processing.domain.CallbackJob;
import io.github.lvdaxianer.doclens.j.processing.infrastructure.CallbackDeliveryWorker;
import io.github.lvdaxianer.doclens.j.shared.web.CallerIdentityRequestResolver;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * Dashboard callback 重试 HTTP 门面。
 *
 * @author lvdaxianer@yeah.net
 * @date 2026-07-02
 */
@Component
public class DashboardCallbackRetryFacade {

    private static final String CALLBACK_JOB_ID_KEY = "callback_job_id";
    private static final String DELIVERED_COUNT_KEY = "delivered_count";

    private final CallerIdentityRequestResolver callerIdentityRequestResolver;
    private final DashboardCallbackRetryAuthorizer callbackRetryAuthorizer;
    private final CallbackDeliveryWorker callbackDeliveryWorker;

    /**
     * 创建 Dashboard callback 重试 HTTP 门面。
     *
     * @param callerIdentityRequestResolver caller 请求解析器
     * @param callbackRetryAuthorizer callback 重试授权器
     * @param callbackDeliveryWorker callback 投递 worker
     * @author lvdaxianer@yeah.net
     * @date 2026-07-02
     */
    public DashboardCallbackRetryFacade(
            CallerIdentityRequestResolver callerIdentityRequestResolver,
            DashboardCallbackRetryAuthorizer callbackRetryAuthorizer,
            CallbackDeliveryWorker callbackDeliveryWorker
    ) {
        this.callerIdentityRequestResolver = callerIdentityRequestResolver;
        this.callbackRetryAuthorizer = callbackRetryAuthorizer;
        this.callbackDeliveryWorker = callbackDeliveryWorker;
    }

    /**
     * 重试当前 caller 可访问的 callback job。
     *
     * @param request HTTP 请求
     * @param callbackJobId callback job ID
     * @return callback 重试结果
     * @author lvdaxianer@yeah.net
     * @date 2026-07-02
     */
    public Map<String, Object> retryCallbackJob(HttpServletRequest request, String callbackJobId) {
        CallerIdentity caller = callerIdentityRequestResolver.resolve(request);
        CallbackJob callbackJob = callbackRetryAuthorizer.authorize(caller, callbackJobId);
        int deliveredCount = callbackDeliveryWorker.retryNow(callbackJob.callbackJobId());
        return Map.of(CALLBACK_JOB_ID_KEY, callbackJobId, DELIVERED_COUNT_KEY, deliveredCount);
    }
}
