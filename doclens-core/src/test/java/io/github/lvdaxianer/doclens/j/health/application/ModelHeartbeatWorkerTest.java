package io.github.lvdaxianer.doclens.j.health.application;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.lvdaxianer.doclens.j.health.domain.ModelHealthFailureType;
import io.github.lvdaxianer.doclens.j.health.domain.ModelHealthSnapshot;
import io.github.lvdaxianer.doclens.j.health.domain.ModelHealthStatus;
import io.github.lvdaxianer.doclens.j.health.domain.ModelHealthTargetId;
import io.github.lvdaxianer.doclens.j.health.domain.ModelHealthTargetType;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

/**
 * 单目标模型心跳 worker 测试。
 *
 * @author lvdaxianerplus
 * @date 2026-06-10
 */
class ModelHeartbeatWorkerTest {

    private static final OffsetDateTime BASE_TIME = OffsetDateTime.parse("2026-06-10T10:00:00+08:00");
    private static final int FAILURE_THRESHOLD = 3;
    private static final int RECOVERY_SUCCESS_THRESHOLD = 2;
    private static final int STALE_AFTER_SECONDS = 20;
    private static final int MAX_ATTEMPTS = 3;
    private static final int INTERVAL_SECONDS = 5;
    private static final int RETRY_INTERVAL_SECONDS = 1;
    private static final String MODEL_KEY = "paddle_ocr";
    private static final String TARGET_ID = "paddle-1";
    private static final String TIMEOUT_ERROR = "timeout";

    /**
     * 探测成功应立即将心跳池标记为可用。
     *
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    @Test
    void successfulProbeMarksPoolUpImmediately() {
        InMemoryModelHeartbeatPool pool = pool();
        RecordingSleeper sleeper = new RecordingSleeper();
        ModelHeartbeatWorker worker = worker(successfulProbe(), pool, sleeper);

        worker.runOnce();

        assertThat(pool.find(target())).get()
                .extracting(ModelHealthSnapshot::status)
                .isEqualTo(ModelHealthStatus.UP);
        assertThat(sleeper.sleeps()).isEmpty();
    }

    /**
     * 连续三次探测失败应将心跳池标记为不可用。
     *
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    @Test
    void threeProbeFailuresMarkPoolDown() {
        InMemoryModelHeartbeatPool pool = pool();
        RecordingSleeper sleeper = new RecordingSleeper();
        ModelHeartbeatWorker worker = worker(failingProbe(), pool, sleeper);

        worker.runOnce();

        assertThat(pool.find(target())).get()
                .extracting(ModelHealthSnapshot::status)
                .isEqualTo(ModelHealthStatus.DOWN);
        assertThat(sleeper.sleeps()).containsExactly(Duration.ofSeconds(RETRY_INTERVAL_SECONDS),
                Duration.ofSeconds(RETRY_INTERVAL_SECONDS));
    }

    /**
     * 第三次探测恢复时不应最终标记为不可用。
     *
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    @Test
    void thirdProbeAttemptCanRecoverWithoutMarkingDown() {
        InMemoryModelHeartbeatPool pool = pool();
        RecordingSleeper sleeper = new RecordingSleeper();
        AtomicInteger attempts = new AtomicInteger(0);
        ModelHeartbeatWorker worker = worker(() -> flakyResult(attempts), pool, sleeper);

        worker.runOnce();

        assertThat(attempts).hasValue(MAX_ATTEMPTS);
        assertThat(pool.find(target())).get()
                .extracting(ModelHealthSnapshot::status)
                .isEqualTo(ModelHealthStatus.UP);
        assertThat(sleeper.sleeps()).hasSize(RECOVERY_SUCCESS_THRESHOLD);
    }

    /**
     * 重试休眠被中断时应停止本轮探测并保留中断标记。
     *
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    @Test
    void interruptedRetryStopsCurrentProbeRound() {
        InMemoryModelHeartbeatPool pool = pool();
        AtomicInteger attempts = new AtomicInteger(0);
        ModelHeartbeatWorker worker = worker(() -> countedFailure(attempts), pool, new InterruptingSleeper());

        worker.runOnce();

        assertThat(attempts).hasValue(1);
        assertThat(Thread.interrupted()).isTrue();
        assertThat(pool.find(target())).get()
                .extracting(ModelHealthSnapshot::lastFailureType)
                .isEqualTo(Optional.of(ModelHealthFailureType.UNKNOWN));
    }

    /**
     * 创建测试用心跳 worker。
     *
     * @param probe 心跳探针
     * @param pool 心跳池
     * @param sleeper 休眠器
     * @return 心跳 worker
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    private ModelHeartbeatWorker worker(
            ModelHeartbeatProbe probe,
            InMemoryModelHeartbeatPool pool,
            ModelHeartbeatWorker.Sleeper sleeper
    ) {
        return new ModelHeartbeatWorker(new ModelHeartbeatWorker.Dependencies(target(), probe, pool, sleeper),
                settings());
    }

    /**
     * 创建测试用心跳池。
     *
     * @return 心跳池
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    private InMemoryModelHeartbeatPool pool() {
        InMemoryModelHeartbeatPool pool = new InMemoryModelHeartbeatPool(poolSettings());
        pool.register(List.of(target()));
        return pool;
    }

    /**
     * 创建成功探针。
     *
     * @return 成功探针
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    private ModelHeartbeatProbe successfulProbe() {
        return ModelHeartbeatProbeResult::success;
    }

    /**
     * 创建失败探针。
     *
     * @return 失败探针
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    private ModelHeartbeatProbe failingProbe() {
        return () -> ModelHeartbeatProbeResult.failure(ModelHealthFailureType.TIMEOUT, TIMEOUT_ERROR);
    }

    /**
     * 创建先失败后成功的探测结果。
     *
     * @param attempts 尝试次数
     * @return 探测结果
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    private ModelHeartbeatProbeResult flakyResult(AtomicInteger attempts) {
        if (attempts.incrementAndGet() < MAX_ATTEMPTS) {
            return ModelHeartbeatProbeResult.failure(ModelHealthFailureType.TIMEOUT, TIMEOUT_ERROR);
        } else {
            return ModelHeartbeatProbeResult.success();
        }
    }

    /**
     * 创建计数失败探测结果。
     *
     * @param attempts 尝试次数
     * @return 探测结果
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    private ModelHeartbeatProbeResult countedFailure(AtomicInteger attempts) {
        attempts.incrementAndGet();
        return ModelHeartbeatProbeResult.failure(ModelHealthFailureType.TIMEOUT, TIMEOUT_ERROR);
    }

    /**
     * 创建测试用 worker 设置。
     *
     * @return worker 设置
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    private ModelHeartbeatWorkerSettings settings() {
        return new ModelHeartbeatWorkerSettings(INTERVAL_SECONDS, RETRY_INTERVAL_SECONDS, MAX_ATTEMPTS);
    }

    /**
     * 创建测试用心跳池设置。
     *
     * @return 心跳池设置
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    private ModelHeartbeatPoolSettings poolSettings() {
        return new ModelHeartbeatPoolSettings(this::now, FAILURE_THRESHOLD, RECOVERY_SUCCESS_THRESHOLD,
                STALE_AFTER_SECONDS);
    }

    /**
     * 创建测试用模型目标。
     *
     * @return 模型目标
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    private ModelHealthTargetId target() {
        return new ModelHealthTargetId(ModelHealthTargetType.OCR_NODE, MODEL_KEY, TARGET_ID);
    }

    /**
     * 返回固定当前时间。
     *
     * @return 当前时间
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    private OffsetDateTime now() {
        return BASE_TIME;
    }

    /**
     * 记录型休眠器。
     *
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    private static class RecordingSleeper implements ModelHeartbeatWorker.Sleeper {

        private final List<Duration> sleeps = new ArrayList<>(RECOVERY_SUCCESS_THRESHOLD);

        /**
         * 记录休眠时长。
         *
         * @param duration 休眠时长
         * @author lvdaxianerplus
         * @date 2026-06-10
         */
        @Override
        public void sleep(Duration duration) {
            sleeps.add(duration);
        }

        /**
         * 返回休眠记录。
         *
         * @return 休眠记录
         * @author lvdaxianerplus
         * @date 2026-06-10
         */
        private List<Duration> sleeps() {
            return sleeps;
        }
    }

    /**
     * 中断型休眠器。
     *
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    private static class InterruptingSleeper implements ModelHeartbeatWorker.Sleeper {

        /**
         * 抛出中断异常。
         *
         * @param duration 休眠时长
         * @throws InterruptedException 固定抛出中断异常
         * @author lvdaxianerplus
         * @date 2026-06-10
         */
        @Override
        public void sleep(Duration duration) throws InterruptedException {
            throw new InterruptedException("test interruption");
        }
    }
}
