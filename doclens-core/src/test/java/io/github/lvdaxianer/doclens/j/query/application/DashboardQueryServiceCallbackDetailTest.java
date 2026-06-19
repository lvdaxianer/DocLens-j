package io.github.lvdaxianer.doclens.j.query.application;

import static io.github.lvdaxianer.doclens.j.query.application.DashboardQueryServiceFixtures.batch;
import static io.github.lvdaxianer.doclens.j.query.application.DashboardQueryServiceFixtures.completedDocument;

import io.github.lvdaxianer.doclens.j.processing.domain.CallbackJob;
import io.github.lvdaxianer.doclens.j.processing.domain.CallbackJobCreateRequest;
import io.github.lvdaxianer.doclens.j.processing.domain.CallbackJobFailureRequest;
import io.github.lvdaxianer.doclens.j.processing.domain.CallbackJobRepository;
import io.github.lvdaxianer.doclens.j.processing.domain.CallbackFailureReason;
import io.github.lvdaxianer.doclens.j.processing.domain.DocumentType;
import io.github.lvdaxianer.doclens.j.processing.domain.EmptyOcrResultRepository;
import io.github.lvdaxianer.doclens.j.query.application.DashboardQueryServiceFixtures.InMemoryBatchRepository;
import io.github.lvdaxianer.doclens.j.query.application.DashboardQueryServiceFixtures.InMemoryDocumentJobRepository;
import io.github.lvdaxianer.doclens.j.query.application.DashboardQueryServiceFixtures.InMemoryOcrEventRepository;
import io.github.lvdaxianer.doclens.j.shared.domain.JsonPayload;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/**
 * Dashboard 批次详情回调结果查询测试。
 *
 * @author lvdaxianerplus
 * @date 2026-06-15
 */
class DashboardQueryServiceCallbackDetailTest {

    private static final OffsetDateTime BASE_TIME = OffsetDateTime.parse("2026-06-15T10:00:00+08:00");
    private static final String CALLBACK_JOB_ID_KEY = "callback_job_id";
    private static final String CALLBACK_STATUS_KEY = "status";
    private static final String CALLBACK_FAILURE_REASON_KEY = "failure_reason";
    private static final String CALLBACK_FAILURE_DETAIL_KEY = "failure_detail";
    private static final String CALLBACK_SUCCESS_STATUS = "success";
    private static final String CALLBACK_FAILED_STATUS = "failed";
    private static final String CALLBACK_HTTP_STATUS_REASON = "http_status";

    /**
     * 批次详情应展示 callback 投递结果。
     *
     * @author lvdaxianerplus
     * @date 2026-06-15
     */
    @Test
    void batchDetailExposesCallbackJobOutcomeDetails() {
        CallbackJobRepository callbackJobRepository = new InMemoryCallbackJobRepository(List.of(
                succeededCallbackJob(),
                failedCallbackJob()
        ));
        DashboardQueryService service = new DashboardQueryService(new DashboardQueryService.Dependencies(
                new DashboardQueryService.Dependencies.Repositories(new InMemoryBatchRepository(List.of(batch())),
                        new InMemoryDocumentJobRepository(List.of(completedDocument("doc-1", DocumentType.PDF, 0))),
                        new InMemoryOcrEventRepository(), new EmptyOcrResultRepository()),
                new DashboardQueryService.Dependencies.Services(new EmptyDashboardOcrMetricsProvider(),
                        callbackJobRepository)));

        Map<String, Object> detail = service.batchDetail("batch-test");

        List<?> callbackJobs = (List<?>) detail.get("callback_jobs");
        assertCallbackJob(callbackJobs.get(0), new ExpectedCallbackJob("callback-1",
                CALLBACK_SUCCESS_STATUS, "", ""));
        assertCallbackJob(callbackJobs.get(1), new ExpectedCallbackJob("callback-2",
                CALLBACK_FAILED_STATUS, CALLBACK_HTTP_STATUS_REASON, "HTTP 503"));
    }

    /**
     * 断言 callback 作业读模型。
     *
     * @param job callback 作业读模型
     * @param expectedCallbackJob 期望 callback 作业
     * @author lvdaxianerplus
     * @date 2026-06-15
     */
    private void assertCallbackJob(Object job, ExpectedCallbackJob expectedCallbackJob) {
        @SuppressWarnings("unchecked")
        Map<String, Object> callbackJob = (Map<String, Object>) job;
        org.assertj.core.api.Assertions.assertThat(callbackJob)
                .containsEntry(CALLBACK_JOB_ID_KEY, expectedCallbackJob.callbackJobId())
                .containsEntry(CALLBACK_STATUS_KEY, expectedCallbackJob.status())
                .containsEntry(CALLBACK_FAILURE_REASON_KEY, expectedCallbackJob.failureReason())
                .containsEntry(CALLBACK_FAILURE_DETAIL_KEY, expectedCallbackJob.failureDetail());
    }

    /**
     * 创建成功 callback 作业。
     *
     * @return 成功 callback 作业
     * @author lvdaxianerplus
     * @date 2026-06-15
     */
    private CallbackJob succeededCallbackJob() {
        return callbackJob("callback-1", "doc-1").markSucceeded(BASE_TIME.plusSeconds(1));
    }

    /**
     * 创建失败 callback 作业。
     *
     * @return 失败 callback 作业
     * @author lvdaxianerplus
     * @date 2026-06-15
     */
    private CallbackJob failedCallbackJob() {
        return callbackJob("callback-2", "doc-1").markFailed(new CallbackJobFailureRequest("callback-2",
                CallbackFailureReason.HTTP_STATUS, "HTTP 503", 1, null, BASE_TIME.plusSeconds(2)));
    }

    /**
     * 创建 callback 作业。
     *
     * @param callbackJobId callback 作业 ID
     * @param documentId 文档 ID
     * @return callback 作业
     * @author lvdaxianerplus
     * @date 2026-06-15
     */
    private CallbackJob callbackJob(String callbackJobId, String documentId) {
        return CallbackJob.create(new CallbackJobCreateRequest(callbackJobId, "event-" + callbackJobId,
                "batch-test", documentId, "https://callback.example.test/done", JsonPayload.empty().values(),
                BASE_TIME));
    }

    /**
     * 期望 callback 作业读模型。
     *
     * @param callbackJobId callback 作业 ID
     * @param status callback 作业状态
     * @param failureReason 失败原因
     * @param failureDetail 失败详情
     * @author lvdaxianerplus
     * @date 2026-06-15
     */
    private record ExpectedCallbackJob(
            String callbackJobId,
            String status,
            String failureReason,
            String failureDetail
    ) {
    }

    /**
     * 简单内存 callback 仓储。
     *
     * @param jobs 测试回调任务
     * @author lvdaxianerplus
     * @date 2026-06-15
     */
    private static final class InMemoryCallbackJobRepository implements CallbackJobRepository {

        private final List<CallbackJob> jobs;

        /**
         * 创建内存 callback 仓储。
         *
         * @param jobs 测试回调任务
         * @author lvdaxianerplus
         * @date 2026-06-15
         */
        private InMemoryCallbackJobRepository(List<CallbackJob> jobs) {
            this.jobs = jobs;
        }

        /**
         * 保存 callback 作业。
         *
         * @param job callback 作业
         * @author lvdaxianerplus
         * @date 2026-06-15
         */
        @Override
        public void save(CallbackJob job) {
            throw new UnsupportedOperationException();
        }

        /**
         * 根据 ID 查找 callback 作业。
         *
         * @param callbackJobId callback 作业 ID
         * @return 可选 callback 作业
         * @author lvdaxianerplus
         * @date 2026-06-15
         */
        @Override
        public Optional<CallbackJob> findById(String callbackJobId) {
            return jobs.stream().filter(job -> job.callbackJobId().equals(callbackJobId)).findFirst();
        }

        /**
         * 查询待投递 callback 作业。
         *
         * @param limit 最大返回数量
         * @return callback 作业列表
         * @author lvdaxianerplus
         * @date 2026-06-15
         */
        @Override
        public List<CallbackJob> listPending(int limit) {
            return jobs;
        }

        /**
         * 按批次查询 callback 作业。
         *
         * @param batchId 批次 ID
         * @return callback 作业列表
         * @author lvdaxianerplus
         * @date 2026-06-15
         */
        @Override
        public List<CallbackJob> listByBatchId(String batchId) {
            return jobs.stream().filter(job -> job.batchId().equals(batchId)).toList();
        }

        /**
         * 标记 callback 作业投递成功。
         *
         * @param callbackJobId callback 作业 ID
         * @author lvdaxianerplus
         * @date 2026-06-15
         */
        @Override
        public void markSucceeded(String callbackJobId) {
            throw new UnsupportedOperationException();
        }

        /**
         * 重置 callback 作业投递轮次。
         *
         * @param callbackJobId callback 作业 ID
         * @author lvdaxianerplus
         * @date 2026-06-16
         */
        @Override
        public void restartAttempts(String callbackJobId) {
            throw new UnsupportedOperationException();
        }

        /**
         * 标记 callback 作业投递失败。
         *
         * @param request 失败更新请求
         * @author lvdaxianerplus
         * @date 2026-06-15
         */
        @Override
        public void markFailed(CallbackJobFailureRequest request) {
            throw new UnsupportedOperationException();
        }
    }

}
