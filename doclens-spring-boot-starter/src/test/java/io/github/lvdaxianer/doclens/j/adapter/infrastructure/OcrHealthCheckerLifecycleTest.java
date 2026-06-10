package io.github.lvdaxianer.doclens.j.adapter.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.lvdaxianer.doclens.j.adapter.domain.OcrNode;
import io.github.lvdaxianer.doclens.j.adapter.domain.OcrNodeCreateRequest;
import io.github.lvdaxianer.doclens.j.adapter.domain.OcrNodeRepository;
import io.github.lvdaxianer.doclens.j.adapter.domain.OcrNodeStatus;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.Queue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.junit.jupiter.api.Test;

/**
 * OCR 健康检查器生命周期测试。
 *
 * @author lvdaxianerplus
 * @date 2026-06-10
 */
class OcrHealthCheckerLifecycleTest {

    private static final OffsetDateTime BASE_TIME = OffsetDateTime.parse("2026-06-10T10:00:00+08:00");

    private final MutableClock clock = new MutableClock(BASE_TIME);
    private final InMemoryOcrNodeRepository repository = new InMemoryOcrNodeRepository();

    /**
     * 连续失败达到阈值后应打开熔断窗口。
     *
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    @Test
    void repeatedFailuresOpenCircuitUntilConfiguredDeadline() {
        OcrHealthChecker checker = checker(false, properties(3, 3, 600));
        OcrNode node = sampleNode();

        checker.checkNode(node);
        checker.checkNode(repository.findById(node.id()).orElseThrow());
        checker.checkNode(repository.findById(node.id()).orElseThrow());

        OcrNode reloaded = repository.findById(node.id()).orElseThrow();
        assertThat(reloaded.status()).isEqualTo(OcrNodeStatus.DOWN);
        assertThat(reloaded.circuitOpenUntil()).contains(BASE_TIME.plusSeconds(600));
    }

    /**
     * 熔断窗口结束后节点需要达到恢复阈值才可重新变为 UP。
     *
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    @Test
    void nodeNeedsThreeSuccessfulRecoveryProbesAfterCircuitWindowExpires() {
        OcrHealthChecker checker = checkerSequence(List.of(false, false, false, true, true, true),
                properties(3, 3, 600));
        OcrNode node = sampleNode();

        failToOpenCircuit(checker, node);
        advanceClockBy(Duration.ofSeconds(601));

        checker.checkNode(repository.findById(node.id()).orElseThrow());
        checker.checkNode(repository.findById(node.id()).orElseThrow());
        checker.checkNode(repository.findById(node.id()).orElseThrow());

        OcrNode reloaded = repository.findById(node.id()).orElseThrow();
        assertThat(reloaded.status()).isEqualTo(OcrNodeStatus.UP);
        assertThat(reloaded.circuitOpenUntil()).isEmpty();
    }

    /**
     * RECOVERING 节点即使仍有熔断窗口，也应继续执行恢复探测。
     *
     * @author lvdaxianerplus
     * @date 2026-06-11
     */
    @Test
    void recoveringNodeContinuesProbingInsideCircuitWindow() {
        OcrHealthChecker checker = checkerSequence(List.of(false, false, false, true),
                properties(3, 3, 600));
        OcrNode node = sampleNode();

        failToOpenCircuit(checker, node);
        checker.checkNodeIgnoringCircuitWindow(repository.findById(node.id()).orElseThrow());
        checker.checkNode(repository.findById(node.id()).orElseThrow());

        OcrNode reloaded = repository.findById(node.id()).orElseThrow();
        assertThat(reloaded.status()).isEqualTo(OcrNodeStatus.RECOVERING);
        assertThat(reloaded.successCount()).isEqualTo(2);
    }

    /**
     * DOWN 节点即使仍处于熔断窗口，也应继续被后台心跳探测以便服务恢复后自动复活。
     *
     * @author lvdaxianerplus
     * @date 2026-06-11
     */
    @Test
    void downNodeContinuesProbingInsideCircuitWindow() {
        OcrHealthChecker checker = checkerSequence(List.of(false, false, false, true),
                properties(3, 3, 600));
        OcrNode node = sampleNode();

        failToOpenCircuit(checker, node);
        checker.checkNode(repository.findById(node.id()).orElseThrow());

        OcrNode reloaded = repository.findById(node.id()).orElseThrow();
        assertThat(reloaded.status()).isEqualTo(OcrNodeStatus.RECOVERING);
        assertThat(reloaded.successCount()).isEqualTo(1);
        assertThat(reloaded.circuitOpenUntil()).isEmpty();
    }

    /**
     * 创建固定健康结果的健康检查器。
     *
     * @param healthy 是否健康
     * @param properties 健康检查配置
     * @return 健康检查器
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    private OcrHealthChecker checker(boolean healthy, OcrHealthCheckProperties properties) {
        return new OcrHealthChecker(repository, ignored -> healthy, executor(), properties, clock::now);
    }

    /**
     * 创建按顺序返回健康结果的健康检查器。
     *
     * @param results 健康结果序列
     * @param properties 健康检查配置
     * @return 健康检查器
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    private OcrHealthChecker checkerSequence(List<Boolean> results, OcrHealthCheckProperties properties) {
        return new OcrHealthChecker(repository, new SequenceHealthClient(results), executor(), properties, clock::now);
    }

    /**
     * 创建测试用健康检查配置。
     *
     * @param failureThreshold 失败阈值
     * @param recoverySuccessThreshold 恢复阈值
     * @param circuitOpenSeconds 熔断时长秒数
     * @return 健康检查配置
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    private OcrHealthCheckProperties properties(
            int failureThreshold,
            int recoverySuccessThreshold,
            int circuitOpenSeconds
    ) {
        return new OcrHealthCheckProperties(failureThreshold, recoverySuccessThreshold, circuitOpenSeconds);
    }

    /**
     * 创建测试节点并写入仓储。
     *
     * @return OCR 节点
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    private OcrNode sampleNode() {
        OcrNode node = OcrNode.create(new OcrNodeCreateRequest("node-1", "paddle_ocr", "Node 1",
                "127.0.0.1", 8080, true, true, 50, 10, BASE_TIME));
        repository.save(node);
        return node;
    }

    /**
     * 连续执行健康检查直到节点进入熔断状态。
     *
     * @param checker 健康检查器
     * @param node OCR 节点
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    private void failToOpenCircuit(OcrHealthChecker checker, OcrNode node) {
        checker.checkNode(node);
        checker.checkNode(repository.findById(node.id()).orElseThrow());
        checker.checkNode(repository.findById(node.id()).orElseThrow());
    }

    /**
     * 推进测试时钟。
     *
     * @param duration 推进时长
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    private void advanceClockBy(Duration duration) {
        clock.advance(duration);
    }

    /**
     * 创建测试线程池。
     *
     * @return 线程池
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    private ExecutorService executor() {
        return Executors.newSingleThreadExecutor(runnable -> new Thread(runnable, "doclens-ocr-health-lifecycle-1"));
    }

    /**
     * 内存 OCR 节点仓储。
     *
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    private static final class InMemoryOcrNodeRepository implements OcrNodeRepository {

        private final HashMap<String, OcrNode> nodes = new HashMap<>(4);

        @Override
        public void save(OcrNode node) {
            nodes.put(node.id(), node);
        }

        @Override
        public void saveAll(List<OcrNode> nodes) {
            nodes.forEach(this::save);
        }

        @Override
        public void update(OcrNode node) {
            nodes.put(node.id(), node);
        }

        @Override
        public Optional<OcrNode> findById(String nodeId) {
            return Optional.ofNullable(nodes.get(nodeId));
        }

        @Override
        public List<OcrNode> listByModelKey(String modelKey) {
            return nodes.values().stream().filter(node -> modelKey.equals(node.modelKey())).toList();
        }

        @Override
        public List<OcrNode> listEnabled() {
            return nodes.values().stream().filter(OcrNode::enabled).toList();
        }

        @Override
        public List<OcrNode> listAll() {
            return nodes.values().stream().toList();
        }

        @Override
        public Optional<OcrNode> findByModelHostPort(String modelKey, String host, int port) {
            return nodes.values().stream()
                    .filter(node -> modelKey.equals(node.modelKey()) && host.equals(node.host()) && port == node.port())
                    .findFirst();
        }

        @Override
        public void deleteById(String nodeId) {
            nodes.remove(nodeId);
        }
    }

    /**
     * 顺序健康结果客户端。
     *
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    private static final class SequenceHealthClient implements OcrHealthClient {

        private final Queue<Boolean> results;

        private SequenceHealthClient(List<Boolean> results) {
            this.results = new ArrayDeque<>(results);
        }

        @Override
        public boolean isHealthy(OcrNode node) {
            return Optional.ofNullable(results.poll()).orElse(Boolean.TRUE);
        }
    }

    /**
     * 可推进的测试时钟。
     *
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    private static final class MutableClock {

        private OffsetDateTime now;

        private MutableClock(OffsetDateTime now) {
            this.now = now;
        }

        /**
         * 推进当前时间。
         *
         * @param duration 推进时长
         * @author lvdaxianerplus
         * @date 2026-06-10
         */
        void advance(Duration duration) {
            now = now.plus(duration);
        }

        /**
         * 返回当前时间。
         *
         * @return 当前时间
         * @author lvdaxianerplus
         * @date 2026-06-10
         */
        OffsetDateTime now() {
            return now;
        }
    }
}
