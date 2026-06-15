package io.github.lvdaxianer.doclens.j.processing.infrastructure;

import io.github.lvdaxianer.doclens.j.processing.domain.CallbackJob;
import io.github.lvdaxianer.doclens.j.processing.domain.CallbackJobRepository;
import java.time.Duration;
import java.util.List;
import java.util.Objects;

/**
 * 回调投递 worker。
 *
 * @author lvdaxianerplus
 * @date 2026-06-15
 */
public class CallbackDeliveryWorker {

    private final CallbackJobRepository repository;
    private final CallbackDeliveryProcessor processor;
    private final int batchSize;

    /**
     * 创建回调投递 worker。
     *
     * @param dependencies worker 依赖集合
     * @param options worker 配置
     * @author lvdaxianerplus
     * @date 2026-06-15
     */
    public CallbackDeliveryWorker(Dependencies dependencies, Options options) {
        Options safeOptions = options == null ? Options.defaults() : options;
        Dependencies safeDependencies = Objects.requireNonNull(dependencies, "callback dependencies is required");
        this.repository = safeDependencies.repository();
        this.processor = new CallbackDeliveryProcessor(safeDependencies.processorDependencies(),
                safeOptions.timeout(), safeOptions.maxRetries(), safeOptions.retryBackoff());
        this.batchSize = safeOptions.batchSize();
    }

    /**
     * 扫描并投递一轮待回调任务。
     *
     * @return 成功投递数量
     * @author lvdaxianerplus
     * @date 2026-06-15
     */
    public int runOnce() {
        return deliverBatch(pendingJobs());
    }

    /**
     * 查询待投递任务。
     *
     * @return 待投递任务
     * @author lvdaxianerplus
     * @date 2026-06-15
     */
    private List<CallbackJob> pendingJobs() {
        return repository.listPending(batchSize);
    }

    /**
     * 批量提交回调投递任务。
     *
     * @param jobs 待投递任务
     * @return 成功投递数量
     * @author lvdaxianerplus
     * @date 2026-06-15
     */
    private int deliverBatch(List<CallbackJob> jobs) {
        return processor.deliver(jobs);
    }

    /**
     * 回调投递 worker 依赖集合。
     *
     * @param repository 回调任务仓储
     * @param processorDependencies 投递处理器依赖
     * @author lvdaxianerplus
     * @date 2026-06-15
     */
    public record Dependencies(
            CallbackJobRepository repository,
            CallbackDeliveryProcessor.Dependencies processorDependencies
    ) {

        /**
         * 创建回调投递 worker 依赖集合。
         *
         * @author lvdaxianerplus
         * @date 2026-06-15
         */
        public Dependencies {
            repository = Objects.requireNonNull(repository, "callback job repository is required");
            processorDependencies = Objects.requireNonNull(processorDependencies,
                    "callback processor dependencies is required");
        }
    }

    /**
     * 回调投递 worker 配置。
     *
     * @param timeout 回调超时时间
     * @param batchSize 单轮扫描数量
     * @param maxRetries 最大重试次数
     * @param retryBackoff 重试退避间隔
     * @author lvdaxianerplus
     * @date 2026-06-15
     */
    public record Options(Duration timeout, int batchSize, int maxRetries, Duration retryBackoff) {

        private static final int DEFAULT_MAX_RETRIES = 3;
        private static final int DEFAULT_RETRY_BACKOFF_SECONDS = 30;

        /**
         * 创建兼容旧调用方的配置。
         *
         * @param timeout 回调超时时间
         * @param batchSize 单轮扫描数量
         * @author lvdaxianerplus
         * @date 2026-06-15
         */
        public Options(Duration timeout, int batchSize) {
            this(timeout, batchSize, DEFAULT_MAX_RETRIES, Duration.ofSeconds(DEFAULT_RETRY_BACKOFF_SECONDS));
        }

        /**
         * 创建带安全默认值的配置。
         *
         * @author lvdaxianerplus
         * @date 2026-06-15
         */
        public Options {
            timeout = timeout == null ? Duration.ofSeconds(10) : timeout;
            batchSize = Math.max(1, batchSize);
            maxRetries = Math.max(0, maxRetries);
            retryBackoff = retryBackoff == null ? Duration.ofSeconds(DEFAULT_RETRY_BACKOFF_SECONDS) : retryBackoff;
        }

        /**
         * 创建默认配置。
         *
         * @return 默认配置
         * @author lvdaxianerplus
         * @date 2026-06-15
         */
        public static Options defaults() {
            return new Options(Duration.ofSeconds(10), 100, DEFAULT_MAX_RETRIES,
                    Duration.ofSeconds(DEFAULT_RETRY_BACKOFF_SECONDS));
        }
    }
}
