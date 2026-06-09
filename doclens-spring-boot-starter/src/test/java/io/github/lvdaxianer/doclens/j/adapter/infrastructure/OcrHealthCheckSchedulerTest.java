package io.github.lvdaxianer.doclens.j.adapter.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.lvdaxianer.doclens.j.adapter.domain.OcrNode;
import io.github.lvdaxianer.doclens.j.adapter.domain.OcrNodeCreateRequest;
import io.github.lvdaxianer.doclens.j.adapter.domain.OcrNodeRepository;
import io.github.lvdaxianer.doclens.j.adapter.domain.OcrNodeStatus;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

/**
 * OCR 健康检查调度器测试。
 *
 * @author lvdaxianerplus
 * @date 2026-06-09
 */
class OcrHealthCheckSchedulerTest {

    private static final OffsetDateTime BASE_TIME = OffsetDateTime.parse("2026-06-09T08:00:00+08:00");

    /**
     * 启动调度器后应执行健康检查并刷新运行时节点池。
     *
     * @throws Exception 等待调度任务时抛出
     * @author lvdaxianerplus
     * @date 2026-06-09
     */
    @Test
    void startRunsHealthCheckAndRefreshesNodePool() throws Exception {
        InMemoryOcrNodeRepository repository = new InMemoryOcrNodeRepository(node());
        ExecutorService healthExecutor = Executors.newSingleThreadExecutor();
        ScheduledExecutorService schedulerExecutor = Executors.newSingleThreadScheduledExecutor();
        OcrHealthChecker checker = new OcrHealthChecker(repository, ignored -> true, healthExecutor,
                new OcrHealthCheckProperties(1, 1));
        OcrRuntimeNodePool nodePool = new OcrRuntimeNodePool(repository);
        OcrHealthCheckScheduler scheduler = new OcrHealthCheckScheduler(checker, nodePool, schedulerExecutor, 60);

        nodePool.initialize();
        scheduler.start();
        Thread.sleep(200);
        schedulerExecutor.shutdownNow();
        healthExecutor.shutdownNow();

        assertThat(repository.findById("node-1")).get().extracting(OcrNode::status).isEqualTo(OcrNodeStatus.UP);
        assertThat(nodePool.snapshot()).extracting(view -> view.status()).containsExactly(OcrNodeStatus.UP);
    }

    /**
     * 重复启动调度器时不应重复注册周期任务。
     *
     * @author lvdaxianerplus
     * @date 2026-06-09
     */
    @Test
    void startIsIdempotent() {
        InMemoryOcrNodeRepository repository = new InMemoryOcrNodeRepository(node());
        ExecutorService healthExecutor = Executors.newSingleThreadExecutor();
        CountingScheduledExecutor schedulerExecutor = new CountingScheduledExecutor();
        OcrHealthChecker checker = new OcrHealthChecker(repository, ignored -> true, healthExecutor,
                new OcrHealthCheckProperties(1, 1));
        OcrRuntimeNodePool nodePool = new OcrRuntimeNodePool(repository);
        OcrHealthCheckScheduler scheduler = new OcrHealthCheckScheduler(checker, nodePool, schedulerExecutor, 60);

        scheduler.start();
        scheduler.start();
        healthExecutor.shutdownNow();

        assertThat(schedulerExecutor.scheduleCount()).isEqualTo(1);
        schedulerExecutor.shutdownNow();
    }

    /**
     * 创建恢复中的 OCR 节点。
     *
     * @return OCR 节点
     * @author lvdaxianerplus
     * @date 2026-06-09
     */
    private OcrNode node() {
        OcrNode created = OcrNode.create(new OcrNodeCreateRequest("node-1", "paddle_ocr", "node-1",
                "127.0.0.1", 8080, true, true, 100, 4, BASE_TIME));
        return new OcrNode(created.id(), created.modelKey(), created.name(), created.host(), created.port(),
                created.enabled(), created.participateGlobal(), created.weight(), created.maxConcurrency(),
                OcrNodeStatus.RECOVERING, 0L, 0L, 0L, 0L, Optional.empty(), Optional.empty(),
                Optional.empty(), Optional.empty(), created.createdAt(), created.updatedAt());
    }

    /**
     * 内存 OCR 节点仓储。
     *
     * @author lvdaxianerplus
     * @date 2026-06-09
     */
    private static class InMemoryOcrNodeRepository implements OcrNodeRepository {

        private OcrNode node;

        /**
         * 创建内存 OCR 节点仓储。
         *
         * @param node OCR 节点
         * @author lvdaxianerplus
         * @date 2026-06-09
         */
        InMemoryOcrNodeRepository(OcrNode node) {
            this.node = node;
        }

        @Override
        public void save(OcrNode node) {
            this.node = node;
        }

        @Override
        public void saveAll(List<OcrNode> nodes) {
            nodes.forEach(this::save);
        }

        @Override
        public void update(OcrNode node) {
            this.node = node;
        }

        @Override
        public Optional<OcrNode> findById(String nodeId) {
            return node.id().equals(nodeId) ? Optional.of(node) : Optional.empty();
        }

        @Override
        public List<OcrNode> listByModelKey(String modelKey) {
            return node.modelKey().equals(modelKey) ? List.of(node) : List.of();
        }

        @Override
        public List<OcrNode> listEnabled() {
            return node.enabled() ? List.of(node) : List.of();
        }

        @Override
        public List<OcrNode> listAll() {
            return List.of(node);
        }

        @Override
        public Optional<OcrNode> findByModelHostPort(String modelKey, String host, int port) {
            return node.modelKey().equals(modelKey) && node.host().equals(host) && node.port() == port
                    ? Optional.of(node)
                    : Optional.empty();
        }

        @Override
        public void deleteById(String nodeId) {
            if (node.id().equals(nodeId)) {
                node = node.changeEnabled(false, OffsetDateTime.now());
            } else {
                // 非当前测试节点无需处理。
            }
        }
    }

    /**
     * 记录调度次数的测试调度线程池。
     *
     * @author lvdaxianerplus
     * @date 2026-06-09
     */
    private static class CountingScheduledExecutor extends ScheduledThreadPoolExecutor {

        private final AtomicInteger scheduleCount = new AtomicInteger();

        /**
         * 创建记录调度次数的测试调度线程池。
         *
         * @author lvdaxianerplus
         * @date 2026-06-09
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
         * @date 2026-06-09
         */
        int scheduleCount() {
            return scheduleCount.get();
        }
    }
}
