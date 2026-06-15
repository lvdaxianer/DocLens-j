package io.github.lvdaxianer.doclens.j.processing.infrastructure;

import io.github.lvdaxianer.doclens.j.processing.domain.CallbackFailureReason;
import io.github.lvdaxianer.doclens.j.processing.domain.CallbackJob;
import io.github.lvdaxianer.doclens.j.processing.domain.CallbackJobFailureRequest;
import io.github.lvdaxianer.doclens.j.processing.domain.CallbackJobRepository;
import io.github.lvdaxianer.doclens.j.processing.infrastructure.CallbackDeliveryHttpClient.CallbackDeliveryFailure;
import io.github.lvdaxianer.doclens.j.processing.infrastructure.CallbackRetryPolicy.CallbackRetryAttempt;
import io.github.lvdaxianer.doclens.j.shared.infrastructure.JsonCodec;
import java.io.IOException;
import java.net.http.HttpTimeoutException;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 回调投递处理器。
 *
 * @author lvdaxianerplus
 * @date 2026-06-15
 */
public class CallbackDeliveryProcessor {

    private static final Logger LOGGER = LoggerFactory.getLogger(CallbackDeliveryProcessor.class);

    private final CallbackJobRepository repository;
    private final CallbackDeliveryHttpClient callbackClient;
    private final ExecutorService callbackExecutor;
    private final CallbackRetryPolicy retryPolicy;

    /**
     * 创建回调投递处理器。
     *
     * @param dependencies 处理器依赖
     * @param timeout 回调超时时间
     * @author lvdaxianerplus
     * @date 2026-06-15
     */
    public CallbackDeliveryProcessor(Dependencies dependencies, Duration timeout) {
        this(dependencies, timeout, CallbackRetryPolicy.defaults());
    }

    /**
     * 创建回调投递处理器。
     *
     * @param dependencies 处理器依赖
     * @param timeout 回调超时时间
     * @param maxRetries 最大重试次数
     * @param retryBackoff 重试退避间隔
     * @author lvdaxianerplus
     * @date 2026-06-15
     */
    public CallbackDeliveryProcessor(
            Dependencies dependencies,
            Duration timeout,
            int maxRetries,
            Duration retryBackoff
    ) {
        this(dependencies, timeout, new CallbackRetryPolicy(maxRetries, retryBackoff));
    }

    /**
     * 创建回调投递处理器。
     *
     * @param dependencies 处理器依赖
     * @param timeout 回调超时时间
     * @param retryPolicy 重试策略
     * @author lvdaxianerplus
     * @date 2026-06-15
     */
    CallbackDeliveryProcessor(
            Dependencies dependencies,
            Duration timeout,
            CallbackRetryPolicy retryPolicy
    ) {
        Dependencies safeDependencies = Objects.requireNonNull(dependencies, "callback dependencies is required");
        this.repository = safeDependencies.repository();
        this.callbackClient = new CallbackDeliveryHttpClient(safeDependencies.jsonCodec(), timeout);
        this.callbackExecutor = safeDependencies.callbackExecutor();
        this.retryPolicy = Objects.requireNonNull(retryPolicy, "callback retry policy is required");
    }

    /**
     * 投递一批回调任务。
     *
     * @param jobs 待投递任务
     * @return 成功投递数量
     * @author lvdaxianerplus
     * @date 2026-06-15
     */
    public int deliver(List<CallbackJob> jobs) {
        try {
            return sumResults(callbackExecutor.invokeAll(deliveryTasks(jobs)));
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("callback delivery interrupted", ex);
        }
    }

    /**
     * 创建批量回调任务。
     *
     * @param jobs 待投递任务
     * @return 可提交任务集合
     * @author lvdaxianerplus
     * @date 2026-06-15
     */
    private List<Callable<Integer>> deliveryTasks(List<CallbackJob> jobs) {
        List<Callable<Integer>> tasks = new ArrayList<>(jobs.size());
        for (CallbackJob job : jobs) {
            tasks.add(() -> deliverJob(job));
        }
        return tasks;
    }

    /**
     * 汇总批量投递结果。
     *
     * @param futures 回调投递异步结果
     * @return 成功投递数量
     * @author lvdaxianerplus
     * @date 2026-06-15
     */
    private int sumResults(List<Future<Integer>> futures) {
        return futures.stream().mapToInt(this::deliveryResult).sum();
    }

    /**
     * 读取单个投递结果。
     *
     * @param future 回调投递异步结果
     * @return 成功投递数量
     * @author lvdaxianerplus
     * @date 2026-06-15
     */
    private int deliveryResult(Future<Integer> future) {
        try {
            return future.get();
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("callback delivery interrupted", ex);
        } catch (ExecutionException ex) {
            throw new IllegalStateException("callback delivery failed", ex);
        }
    }

    /**
     * 投递单个回调任务。
     *
     * @param job 回调任务
     * @return 是否成功投递
     * @author lvdaxianerplus
     * @date 2026-06-15
     */
    private int deliverJob(CallbackJob job) {
        try {
            return completeDelivery(job, callbackClient.sendCallback(job));
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            LOGGER.warn("[第三方接口调用] 回调投递失败|DocLensCallback|{}|callbackJobId={}, callbackUrl={}, error={}",
                    job.callbackUrl(), job.callbackJobId(), job.callbackUrl(), ex.getMessage(), ex);
            return markFailed(job, CallbackFailureReason.UNEXPECTED_EXCEPTION, failureDetail(ex));
        } catch (HttpTimeoutException ex) {
            LOGGER.warn("[第三方接口调用] 回调投递超时|DocLensCallback|{}|callbackJobId={}, callbackUrl={}, error={}",
                    job.callbackUrl(), job.callbackJobId(), job.callbackUrl(), ex.getMessage(), ex);
            return markFailed(job, CallbackFailureReason.TIMEOUT, failureDetail(ex));
        } catch (IOException ex) {
            LOGGER.warn("[第三方接口调用] 回调投递失败|DocLensCallback|{}|callbackJobId={}, callbackUrl={}, error={}",
                    job.callbackUrl(), job.callbackJobId(), job.callbackUrl(), ex.getMessage(), ex);
            return markFailed(job, CallbackFailureReason.NETWORK_ERROR, failureDetail(ex));
        } catch (RuntimeException ex) {
            LOGGER.warn("[第三方接口调用] 回调投递失败|DocLensCallback|{}|callbackJobId={}, callbackUrl={}, error={}",
                    job.callbackUrl(), job.callbackJobId(), job.callbackUrl(), ex.getMessage(), ex);
            return markFailed(job, CallbackFailureReason.UNEXPECTED_EXCEPTION, failureDetail(ex));
        }
    }

    /**
     * 根据回调响应完成投递状态转换。
     *
     * @param job 回调任务
     * @param failure 可选失败结果
     * @return 成功投递数量
     * @author lvdaxianerplus
     * @date 2026-06-15
     */
    private int completeDelivery(CallbackJob job, Optional<CallbackDeliveryFailure> failure) {
        // 外部返回非 2xx 时进入失败处理，否则直接标记成功。
        if (failure.isPresent()) {
            return markFailed(job, failure.get().reason(), failure.get().detail());
        } else {
            repository.markSucceeded(job.callbackJobId());
            return 1;
        }
    }

    /**
     * 标记回调投递失败。
     *
     * @param job 回调任务
     * @param reason 失败原因
     * @param detail 失败详情
     * @return 成功投递数量
     * @author lvdaxianerplus
     * @date 2026-06-15
     */
    private int markFailed(CallbackJob job, CallbackFailureReason reason, String detail) {
        OffsetDateTime now = OffsetDateTime.now();
        CallbackRetryAttempt attempt = retryPolicy.failureAttempt(job, now);
        repository.markFailed(new CallbackJobFailureRequest(job.callbackJobId(), reason, detail,
                attempt.retryCount(), attempt.nextRetryAt(), now));
        return 0;
    }

    /**
     * 构建异常失败详情。
     *
     * @param ex 异常
     * @return 失败详情
     * @author lvdaxianerplus
     * @date 2026-06-15
     */
    private String failureDetail(Exception ex) {
        String message = ex.getMessage();
        // 异常无消息时退回到异常类型名，便于定位。
        if (message == null || message.isBlank()) {
            return ex.getClass().getSimpleName();
        } else {
            return message;
        }
    }

    /**
     * 回调投递处理器依赖集合。
     *
     * @param repository 回调任务仓储
     * @param jsonCodec JSON 编解码器
     * @param callbackExecutor 回调投递线程池
     * @author lvdaxianerplus
     * @date 2026-06-15
     */
    public record Dependencies(
            CallbackJobRepository repository,
            JsonCodec jsonCodec,
            ExecutorService callbackExecutor
    ) {

        /**
         * 创建回调投递处理器依赖集合。
         *
         * @author lvdaxianerplus
         * @date 2026-06-15
         */
        public Dependencies {
            repository = Objects.requireNonNull(repository, "callback job repository is required");
            jsonCodec = Objects.requireNonNull(jsonCodec, "json codec is required");
            callbackExecutor = Objects.requireNonNull(callbackExecutor, "callback executor is required");
        }
    }

}
