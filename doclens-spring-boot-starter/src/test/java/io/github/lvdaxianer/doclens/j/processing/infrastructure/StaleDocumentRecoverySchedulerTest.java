package io.github.lvdaxianer.doclens.j.processing.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.lvdaxianer.doclens.j.ingestion.domain.Batch;
import io.github.lvdaxianer.doclens.j.ingestion.domain.BatchRepository;
import io.github.lvdaxianer.doclens.j.processing.application.StaleDocumentRecoveryService;
import io.github.lvdaxianer.doclens.j.processing.application.StaleDocumentRecoveryDependencies;
import io.github.lvdaxianer.doclens.j.processing.domain.DocumentJob;
import io.github.lvdaxianer.doclens.j.processing.domain.DocumentJobRepository;
import io.github.lvdaxianer.doclens.j.processing.domain.OcrEvent;
import io.github.lvdaxianer.doclens.j.processing.domain.OcrEventFactory;
import io.github.lvdaxianer.doclens.j.processing.domain.OcrEventRepository;
import io.github.lvdaxianer.doclens.j.shared.application.TransactionRunner;
import io.github.lvdaxianer.doclens.j.shared.infrastructure.IdGenerator;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

/**
 * 卡死文档恢复调度器测试。
 *
 * @author lvdaxianerplus
 * @date 2026-06-10
 */
class StaleDocumentRecoverySchedulerTest {

    /**
     * 启动调度器后应立即执行一次卡死扫描。
     *
     * @throws Exception 等待调度任务时抛出
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    @Test
    void startRunsRecoveryOnceImmediately() throws Exception {
        RecordingStaleDocumentRecoveryService service = new RecordingStaleDocumentRecoveryService();
        java.util.concurrent.ScheduledExecutorService schedulerExecutor =
                java.util.concurrent.Executors.newSingleThreadScheduledExecutor();
        StaleDocumentRecoveryScheduler scheduler =
                new StaleDocumentRecoveryScheduler(service, schedulerExecutor, 60);

        scheduler.start();
        Thread.sleep(200);
        schedulerExecutor.shutdownNow();

        assertThat(service.recoveryCount()).isGreaterThanOrEqualTo(1);
    }

    /**
     * 重复启动调度器时不应重复注册周期任务。
     *
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    @Test
    void startIsIdempotent() {
        RecordingStaleDocumentRecoveryService service = new RecordingStaleDocumentRecoveryService();
        CountingScheduledExecutor schedulerExecutor = new CountingScheduledExecutor();
        StaleDocumentRecoveryScheduler scheduler =
                new StaleDocumentRecoveryScheduler(service, schedulerExecutor, 60);

        scheduler.start();
        scheduler.start();
        schedulerExecutor.shutdownNow();

        assertThat(schedulerExecutor.scheduleCount()).isEqualTo(1);
    }

    /**
     * 记录恢复调用次数的服务桩。
     *
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    private static final class RecordingStaleDocumentRecoveryService extends StaleDocumentRecoveryService {

        private final AtomicInteger recoveryCount = new AtomicInteger();

        /**
         * 创建记录调用次数的恢复服务桩。
         *
         * @author lvdaxianerplus
         * @date 2026-06-10
         */
        RecordingStaleDocumentRecoveryService() {
            super(new StaleDocumentRecoveryDependencies(new EmptyDocumentJobRepository(), new EmptyBatchRepository(),
                            new EmptyOcrEventRepository(), new OcrEventFactory(new IdGenerator())),
                    new InlineTransactionRunner(), Duration.ofMinutes(5));
        }

        @Override
        public int markStaleDocuments(OffsetDateTime now) {
            return recoveryCount.incrementAndGet();
        }

        /**
         * 返回恢复调用次数。
         *
         * @return 恢复调用次数
         * @author lvdaxianerplus
         * @date 2026-06-10
         */
        int recoveryCount() {
            return recoveryCount.get();
        }
    }

    /**
     * 记录调度次数的测试调度线程池。
     *
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    private static final class CountingScheduledExecutor extends ScheduledThreadPoolExecutor {

        private final AtomicInteger scheduleCount = new AtomicInteger();

        /**
         * 创建记录调度次数的测试调度线程池。
         *
         * @author lvdaxianerplus
         * @date 2026-06-10
         */
        CountingScheduledExecutor() {
            super(1);
        }

        @Override
        public ScheduledFuture<?> schedule(
                Runnable command,
                long delay,
                TimeUnit unit
        ) {
            scheduleCount.incrementAndGet();
            return super.schedule(() -> {
            }, delay, unit);
        }

        /**
         * 返回固定延迟任务注册次数。
         *
         * @return 调度次数
         * @author lvdaxianerplus
         * @date 2026-06-10
         */
        int scheduleCount() {
            return scheduleCount.get();
        }
    }

    /**
     * 空文档仓储桩。
     *
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    private static final class EmptyDocumentJobRepository implements DocumentJobRepository {

        @Override
        public void save(DocumentJob document) {
        }

        @Override
        public void saveAll(List<DocumentJob> documents) {
        }

        @Override
        public void update(DocumentJob document) {
        }

        @Override
        public void updateAll(List<DocumentJob> documents) {
        }

        @Override
        public Optional<DocumentJob> findById(String documentId) {
            return Optional.empty();
        }

        @Override
        public List<DocumentJob> listByBatchId(String batchId) {
            return List.of();
        }

        @Override
        public List<DocumentJob> listByBatchIds(List<String> batchIds) {
            return List.of();
        }

        @Override
        public List<DocumentJob> listRecent(int limit) {
            return List.of();
        }
    }

    /**
     * 空批次仓储桩。
     *
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    private static final class EmptyBatchRepository implements BatchRepository {

        @Override
        public void save(Batch batch) {
        }

        @Override
        public Optional<Batch> findById(String batchId) {
            return Optional.empty();
        }

        @Override
        public Optional<Batch> findByIdempotencyKey(String idempotencyKey) {
            return Optional.empty();
        }

        @Override
        public List<Batch> listRecent(int limit) {
            return List.of();
        }

        @Override
        public void updateSummary(String batchId, int completedFiles, int failedFiles, io.github.lvdaxianer.doclens.j.ingestion.domain.BatchStatus status) {
        }
    }

    /**
     * 空事件仓储桩。
     *
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    private static final class EmptyOcrEventRepository implements OcrEventRepository {

        @Override
        public void save(OcrEvent event) {
        }

        @Override
        public void saveAll(List<OcrEvent> events) {
        }

        @Override
        public List<OcrEvent> listByBatchId(String batchId) {
            return List.of();
        }

        @Override
        public List<OcrEvent> listRecent(int limit) {
            return List.of();
        }
    }

    /**
     * 直接执行事务的测试事务器。
     *
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    private static final class InlineTransactionRunner implements TransactionRunner {

        @Override
        public <T> T requiredResult(java.util.function.Supplier<T> action) {
            return action.get();
        }

        @Override
        public void requiredVoid(Runnable action) {
            action.run();
        }
    }
}
