package io.github.lvdaxianer.doclens.j.dashboard.interfaces;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.github.lvdaxianer.doclens.j.ingestion.domain.Batch;
import io.github.lvdaxianer.doclens.j.ingestion.domain.BatchRepository;
import io.github.lvdaxianer.doclens.j.ingestion.domain.CallerIdentity;
import io.github.lvdaxianer.doclens.j.processing.domain.CallbackJob;
import io.github.lvdaxianer.doclens.j.processing.domain.CallbackJobCreateRequest;
import io.github.lvdaxianer.doclens.j.processing.domain.CallbackJobRepository;
import io.github.lvdaxianer.doclens.j.processing.infrastructure.CallbackDeliveryWorker;
import io.github.lvdaxianer.doclens.j.shared.domain.JsonPayload;
import io.github.lvdaxianer.doclens.j.shared.domain.ResourceNotFoundException;
import io.github.lvdaxianer.doclens.j.shared.web.CallerIdentityRequestResolver;
import io.github.lvdaxianer.doclens.j.shared.web.CallerPartitionInterceptor;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

/**
 * Dashboard callback retry 门面测试。
 *
 * @author lvdaxianer@yeah.net
 * @date 2026-07-02
 */
class DashboardCallbackRetryFacadeTest {

    private static final String CALLBACK_JOB_ID = "callback-job-1";
    private static final String CALLBACK_JOB_ID_KEY = "callback_job_id";
    private static final String DELIVERED_COUNT_KEY = "delivered_count";
    private static final String CALLBACK_JOB_NOT_FOUND_MESSAGE = "callback job callback-job-1 not found";
    private static final String CALLBACK_EVENT_ID = "callback-job-1-event";
    private static final String CALLBACK_URL = "http://callback.example.test";
    private static final String EMPTY_DOCUMENT_ID = "";
    private static final String CLIENT_ID = "client-a";
    private static final String SOURCE_APP = "dashboard";
    private static final String TENANT_KEY = "tenant-a";
    private static final String OWNED_BATCH_ID = "owned-batch";
    private static final String FOREIGN_BATCH_ID = "foreign-batch";
    private static final int DELIVERED_COUNT = 1;
    private static final int TOTAL_FILES = 1;
    private static final OffsetDateTime NOW = OffsetDateTime.parse("2026-07-02T10:00:00+08:00");
    private static final CallerIdentity CALLER = new CallerIdentity(CLIENT_ID, SOURCE_APP, Optional.of(TENANT_KEY));

    /**
     * foreign caller 的 callback job 不允许重试。
     *
     * @author lvdaxianer@yeah.net
     * @date 2026-07-02
     */
    @Test
    void rejectsForeignCallerCallbackJobWithoutRetrying() {
        CallbackJobRepository callbackJobRepository = org.mockito.Mockito.mock(CallbackJobRepository.class);
        BatchRepository batchRepository = org.mockito.Mockito.mock(BatchRepository.class);
        CallbackDeliveryWorker callbackDeliveryWorker = org.mockito.Mockito.mock(CallbackDeliveryWorker.class);
        DashboardCallbackRetryFacade facade = facade(callbackJobRepository, batchRepository, callbackDeliveryWorker);
        when(callbackJobRepository.findById(CALLBACK_JOB_ID)).thenReturn(Optional.of(callbackJob(FOREIGN_BATCH_ID)));
        when(batchRepository.findByIdForCaller(CALLER, FOREIGN_BATCH_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> facade.retryCallbackJob(request(), CALLBACK_JOB_ID))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage(CALLBACK_JOB_NOT_FOUND_MESSAGE);
        verify(callbackDeliveryWorker, never()).retryNow(anyString());
    }

    /**
     * 当前 caller 的 callback job 可以重试。
     *
     * @author lvdaxianer@yeah.net
     * @date 2026-07-02
     */
    @Test
    void retriesOwnedCallerCallbackJob() {
        CallbackJobRepository callbackJobRepository = org.mockito.Mockito.mock(CallbackJobRepository.class);
        BatchRepository batchRepository = org.mockito.Mockito.mock(BatchRepository.class);
        CallbackDeliveryWorker callbackDeliveryWorker = org.mockito.Mockito.mock(CallbackDeliveryWorker.class);
        DashboardCallbackRetryFacade facade = facade(callbackJobRepository, batchRepository, callbackDeliveryWorker);
        when(callbackJobRepository.findById(CALLBACK_JOB_ID)).thenReturn(Optional.of(callbackJob(OWNED_BATCH_ID)));
        when(batchRepository.findByIdForCaller(CALLER, OWNED_BATCH_ID)).thenReturn(Optional.of(batch(OWNED_BATCH_ID)));
        when(callbackDeliveryWorker.retryNow(CALLBACK_JOB_ID)).thenReturn(DELIVERED_COUNT);

        Map<String, Object> response = facade.retryCallbackJob(request(), CALLBACK_JOB_ID);

        assertThat(response).containsEntry(CALLBACK_JOB_ID_KEY, CALLBACK_JOB_ID)
                .containsEntry(DELIVERED_COUNT_KEY, DELIVERED_COUNT);
    }

    /**
     * 创建 callback retry 门面。
     *
     * @param callbackJobRepository callback job 仓储
     * @param batchRepository batch 仓储
     * @param callbackDeliveryWorker callback 投递 worker
     * @return callback retry 门面
     * @author lvdaxianer@yeah.net
     * @date 2026-07-02
     */
    private DashboardCallbackRetryFacade facade(
            CallbackJobRepository callbackJobRepository,
            BatchRepository batchRepository,
            CallbackDeliveryWorker callbackDeliveryWorker
    ) {
        DashboardCallbackRetryAuthorizer authorizer = new DashboardCallbackRetryAuthorizer(callbackJobRepository,
                batchRepository);
        return new DashboardCallbackRetryFacade(new CallerIdentityRequestResolver(), authorizer, callbackDeliveryWorker);
    }

    /**
     * 创建 callback job。
     *
     * @param batchId 批次 ID
     * @return callback job
     * @author lvdaxianer@yeah.net
     * @date 2026-07-02
     */
    private CallbackJob callbackJob(String batchId) {
        return CallbackJob.create(new CallbackJobCreateRequest(CALLBACK_JOB_ID, CALLBACK_EVENT_ID,
                batchId, EMPTY_DOCUMENT_ID, CALLBACK_URL, Map.of(), NOW));
    }

    /**
     * 创建带 caller 的批次。
     *
     * @param batchId 批次 ID
     * @return 批次
     * @author lvdaxianer@yeah.net
     * @date 2026-07-02
     */
    private Batch batch(String batchId) {
        return Batch.create(batchId, TOTAL_FILES, JsonPayload.empty(), Optional.empty(), Optional.empty(), NOW, CALLER);
    }

    /**
     * 创建带 caller 上下文的请求。
     *
     * @return HTTP 请求
     * @author lvdaxianer@yeah.net
     * @date 2026-07-02
     */
    private MockHttpServletRequest request() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setAttribute(CallerPartitionInterceptor.CALLER_IDENTITY_ATTRIBUTE, CALLER);
        return request;
    }
}
