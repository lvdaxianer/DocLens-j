package io.github.lvdaxianer.doclens.j.adapter.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.lvdaxianer.doclens.j.adapter.domain.OcrNode;
import io.github.lvdaxianer.doclens.j.adapter.domain.OcrNodeCreateRequest;
import io.github.lvdaxianer.doclens.j.adapter.domain.OcrNodeDeploymentType;
import io.github.lvdaxianer.doclens.j.adapter.domain.OcrNodeRepository;
import io.github.lvdaxianer.doclens.j.adapter.domain.OcrNodeStatus;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.junit.jupiter.api.Test;

/**
 * OCR 健康检查器测试。
 *
 * @author lvdaxianerplus
 * @date 2026-06-08
 */
class OcrHealthCheckerTest {

    private static final OffsetDateTime BASE_TIME = OffsetDateTime.parse("2026-06-08T10:00:00+08:00");
    private static final Path HEALTH_CHECKER_SOURCE = Path.of("src/main/java/io/github/lvdaxianer/doclens/j/"
            + "adapter/infrastructure/OcrHealthChecker.java");

    /**
     * UP 节点连续健康检查失败后应变为 DOWN。
     *
     * @author lvdaxianerplus
     * @date 2026-06-08
     */
    @Test
    void upNodeBecomesDownAfterConsecutiveFailures() {
        TestContext context = context(node("node-1", OcrNodeStatus.UP, 1, 0));
        context.healthClient.fail("node-1");

        context.checker.checkOnce();

        assertThat(context.repository.findById("node-1")).get().extracting(OcrNode::status)
                .isEqualTo(OcrNodeStatus.DOWN);
    }

    /**
     * DOWN 节点首次健康检查成功后应进入恢复中。
     *
     * @author lvdaxianerplus
     * @date 2026-06-08
     */
    @Test
    void downNodeBecomesRecoveringAfterFirstSuccess() {
        TestContext context = context(node("node-1", OcrNodeStatus.DOWN, 0, 0));

        context.checker.checkOnce();

        assertThat(context.repository.findById("node-1")).get().extracting(OcrNode::status)
                .isEqualTo(OcrNodeStatus.RECOVERING);
    }

    /**
     * RECOVERING 节点达到恢复成功阈值后应变为 UP。
     *
     * @author lvdaxianerplus
     * @date 2026-06-08
     */
    @Test
    void recoveringNodeBecomesUpAfterSuccessThreshold() {
        TestContext context = context(node("node-1", OcrNodeStatus.RECOVERING, 0, 1));

        context.checker.checkOnce();

        assertThat(context.repository.findById("node-1")).get().extracting(OcrNode::status)
                .isEqualTo(OcrNodeStatus.UP);
    }

    /**
     * DISABLED 节点不应执行健康检查。
     *
     * @author lvdaxianerplus
     * @date 2026-06-08
     */
    @Test
    void disabledNodeIsNotChecked() {
        TestContext context = context(node("node-1", OcrNodeStatus.DISABLED, 0, 0));

        context.checker.checkOnce();

        assertThat(context.healthClient.checkedNodeIds).isEmpty();
    }

    /**
     * 健康检查任务应提交到健康检查线程池执行。
     *
     * @author lvdaxianerplus
     * @date 2026-06-08
     */
    @Test
    void healthChecksRunThroughHealthExecutor() {
        TestContext context = context(node("node-1", OcrNodeStatus.UP, 0, 0));

        context.checker.checkOnce();

        assertThat(context.healthClient.threadNames).allMatch(name -> name.startsWith("doclens-ocr-health-test-"));
    }

    /**
     * 在线节点健康检查需要校验执行权限，不能只依赖配置完整性。
     *
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    @Test
    void onlineNodeHealthCheckRequiresExecutionPermission() {
        TestContext context = context(onlineNode("node-online"));
        context.healthClient.fail("node-online");

        context.checker.checkOnce();

        assertThat(context.healthClient.checkedNodeIds).containsExactly("node-online");
        assertThat(context.repository.findById("node-online")).get().extracting(OcrNode::status)
                .isEqualTo(OcrNodeStatus.DOWN);
    }

    /**
     * 健康检查器生产代码不应通过 return null 适配异步任务。
     *
     * @author lvdaxianerplus
     * @date 2026-06-09
     */
    @Test
    void healthCheckerSourceDoesNotReturnNullFromAsyncTask() throws IOException {
        String source = Files.readString(HEALTH_CHECKER_SOURCE);

        assertThat(source).doesNotContain("return null;");
    }

    /**
     * 创建测试上下文。
     *
     * @param node OCR 节点
     * @return 测试上下文
     * @author lvdaxianerplus
     * @date 2026-06-08
     */
    private TestContext context(OcrNode node) {
        InMemoryOcrNodeRepository repository = new InMemoryOcrNodeRepository(node);
        RecordingOcrHealthClient healthClient = new RecordingOcrHealthClient();
        ExecutorService executor = Executors.newSingleThreadExecutor(runnable -> new Thread(runnable,
                "doclens-ocr-health-test-1"));
        OcrHealthChecker checker = new OcrHealthChecker(repository, healthClient, executor,
                new OcrHealthCheckProperties(1, 2));
        return new TestContext(repository, healthClient, checker, executor);
    }

    /**
     * 创建 OCR 节点。
     *
     * @param nodeId 节点 ID
     * @param status 节点状态
     * @param failureCount 失败次数
     * @param successCount 成功次数
     * @return OCR 节点
     * @author lvdaxianerplus
     * @date 2026-06-08
     */
    private OcrNode node(String nodeId, OcrNodeStatus status, long failureCount, long successCount) {
        OcrNode created = OcrNode.create(new OcrNodeCreateRequest(nodeId, "paddle_ocr", nodeId,
                "127.0.0.1", 8080, status != OcrNodeStatus.DISABLED, true, 100, 4, BASE_TIME));
        return new OcrNode(created.id(), created.modelKey(), created.name(), created.host(), created.port(),
                created.enabled(), created.participateGlobal(), created.weight(), created.maxConcurrency(), status,
                failureCount, successCount, 0L, 0L, Optional.empty(), Optional.empty(), Optional.empty(),
                Optional.empty(), created.createdAt(), created.updatedAt());
    }

    /**
     * 创建在线 OCR 节点。
     *
     * @param nodeId 节点 ID
     * @return OCR 节点
     * @author lvdaxianerplus
     * @date 2026-06-09
     */
    private OcrNode onlineNode(String nodeId) {
        OcrNode created = OcrNode.create(new OcrNodeCreateRequest(nodeId, "paddle_ocr", OcrNodeDeploymentType.ONLINE,
                nodeId, "", 0, "aliyun_bailian_dashscope", "qwen-vl-ocr-2025-11-20",
                "sk-secret", true, true, true, 100, 4, BASE_TIME));
        return new OcrNode(created.id(), created.modelKey(), created.deploymentType(), created.name(),
                created.host(), created.port(), created.channelKey(), created.providerModel(), created.credentialRef(),
                created.credentialConfigured(), created.enabled(), created.participateGlobal(), created.weight(),
                created.maxConcurrency(), OcrNodeStatus.RECOVERING, 0L, 1L, 0L, 0L, Optional.empty(),
                Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(),
                created.createdAt(), created.updatedAt());
    }

    /**
     * 健康检查测试上下文。
     *
     * @param repository OCR 节点仓储
     * @param healthClient 健康检查客户端
     * @param checker 健康检查器
     * @param executor 健康检查线程池
     * @author lvdaxianerplus
     * @date 2026-06-08
     */
    private record TestContext(
            InMemoryOcrNodeRepository repository,
            RecordingOcrHealthClient healthClient,
            OcrHealthChecker checker,
            ExecutorService executor
    ) {
    }

    /**
     * 内存 OCR 节点仓储。
     *
     * @author lvdaxianerplus
     * @date 2026-06-08
     */
    private static class InMemoryOcrNodeRepository implements OcrNodeRepository {

        private final HashMap<String, OcrNode> nodes = new HashMap<>(4);

        /**
         * 创建内存 OCR 节点仓储。
         *
         * @param node OCR 节点
         * @author lvdaxianerplus
         * @date 2026-06-08
         */
        InMemoryOcrNodeRepository(OcrNode node) {
            nodes.put(node.id(), node);
        }

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
     * 记录型 OCR 健康检查客户端。
     *
     * @author lvdaxianerplus
     * @date 2026-06-08
     */
    private static class RecordingOcrHealthClient implements OcrHealthClient {

        private final Set<String> failedNodeIds = new HashSet<>(4);
        private final List<String> checkedNodeIds = new ArrayList<>(4);
        private final List<String> threadNames = new ArrayList<>(4);

        @Override
        public boolean isHealthy(OcrNode node) {
            checkedNodeIds.add(node.id());
            threadNames.add(Thread.currentThread().getName());
            return !failedNodeIds.contains(node.id());
        }

        /**
         * 标记节点健康检查失败。
         *
         * @param nodeId OCR 节点 ID
         * @author lvdaxianerplus
         * @date 2026-06-08
         */
        void fail(String nodeId) {
            failedNodeIds.add(nodeId);
        }
    }
}
