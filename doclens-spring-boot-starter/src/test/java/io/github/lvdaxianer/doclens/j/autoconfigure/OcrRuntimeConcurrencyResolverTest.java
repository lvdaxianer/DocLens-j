package io.github.lvdaxianer.doclens.j.autoconfigure;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.lvdaxianer.doclens.j.adapter.domain.OcrNode;
import io.github.lvdaxianer.doclens.j.adapter.domain.OcrNodeRepository;
import io.github.lvdaxianer.doclens.j.adapter.domain.OcrNodeStatus;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/**
 * OCR 运行时并发解析器测试。
 *
 * @author lvdaxianerplus
 * @date 2026-06-20
 */
class OcrRuntimeConcurrencyResolverTest {

    private static final OffsetDateTime NOW = OffsetDateTime.parse("2026-06-20T22:50:00+08:00");
    private static final int BOOTSTRAP_CONCURRENCY = 10;
    private static final int DASHBOARD_CONCURRENCY = 24;
    private static final int TEST_WEIGHT = 50;
    private static final int TEST_BASE_PORT = 8080;
    private static final int TEST_TIMEOUT_SECONDS = 600;
    private static final long INITIAL_COUNTER = 0L;
    private static final String MODEL_KEY = "paddle_ocr";
    private static final String TEST_HOST = "127.0.0.1";
    private static final String TEST_ENDPOINT = "http://127.0.0.1:8080/ocr";

    /**
     * 页面持久化节点存在时，应优先按页面健康节点并发求和。
     *
     * @author lvdaxianerplus
     * @date 2026-06-20
     */
    @Test
    void resolvesDashboardNodeConcurrencyBeforeBootstrapConcurrency() {
        OcrRuntimeConcurrencyResolver resolver = new OcrRuntimeConcurrencyResolver(propertiesWithBootstrapNodes(10),
                repository(upNode("dashboard-a", 10),
                        upNode("dashboard-b", 4),
                        upNode("dashboard-c", 10)));

        assertThat(resolver.defaultConcurrency()).isEqualTo(DASHBOARD_CONCURRENCY);
    }

    /**
     * 页面中的非健康节点不能增加本地默认并发。
     *
     * @author lvdaxianerplus
     * @date 2026-06-20
     */
    @Test
    void ignoresDashboardNodesThatAreNotUp() {
        OcrRuntimeConcurrencyResolver resolver = new OcrRuntimeConcurrencyResolver(propertiesWithBootstrapNodes(30),
                repository(upNode("dashboard-up", 10), downNode("dashboard-down", 10)));

        assertThat(resolver.defaultConcurrency()).isEqualTo(BOOTSTRAP_CONCURRENCY);
    }

    /**
     * 页面没有可用健康节点时，应回退到配置文件 bootstrap 节点并发。
     *
     * @author lvdaxianerplus
     * @date 2026-06-20
     */
    @Test
    void fallsBackToBootstrapConcurrencyWhenNoDashboardNodeQualifies() {
        OcrRuntimeConcurrencyResolver resolver = new OcrRuntimeConcurrencyResolver(propertiesWithBootstrapNodes(30),
                repository(disabledNode("dashboard-disabled", 10),
                        localNode("dashboard-local", 10),
                        downNode("dashboard-down", 10)));

        assertThat(resolver.defaultConcurrency()).isEqualTo(30);
    }

    /**
     * 创建指定 bootstrap 并发总量的配置。
     *
     * @param totalConcurrency bootstrap 并发总量
     * @return DocLens Spring 配置
     * @author lvdaxianerplus
     * @date 2026-06-20
     */
    private DocLensSpringProperties propertiesWithBootstrapNodes(int totalConcurrency) {
        int nodeCount = totalConcurrency / BOOTSTRAP_CONCURRENCY;
        List<DocLensSpringProperties.PaddleOcrNodeProperties> nodes =
                java.util.stream.IntStream.range(0, nodeCount)
                        .mapToObj(index -> new DocLensSpringProperties.PaddleOcrNodeProperties("bootstrap-" + index,
                                TEST_HOST, TEST_BASE_PORT + index, true, true, TEST_WEIGHT, BOOTSTRAP_CONCURRENCY))
                        .toList();
        return new DocLensSpringProperties(null, true, "local-worker", null, null, null, null, null,
                new DocLensSpringProperties.PaddleOcrProperties(true, TEST_ENDPOINT, TEST_TIMEOUT_SECONDS, false, nodes),
                null, null, null, null, null, null, null, null);
    }

    /**
     * 创建可参与全局调度的健康测试 OCR 节点。
     *
     * @param id 节点 ID
     * @param maxConcurrency 最大并发
     * @return OCR 节点
     * @author lvdaxianerplus
     * @date 2026-06-20
     */
    static OcrNode upNode(String id, int maxConcurrency) {
        return node(new TestNodeProperties(id, true, true, OcrNodeStatus.UP, maxConcurrency));
    }

    /**
     * 创建状态为 DOWN 的测试 OCR 节点。
     *
     * @param id 节点 ID
     * @param maxConcurrency 最大并发
     * @return OCR 节点
     * @author lvdaxianerplus
     * @date 2026-06-20
     */
    static OcrNode downNode(String id, int maxConcurrency) {
        return node(new TestNodeProperties(id, true, true, OcrNodeStatus.DOWN, maxConcurrency));
    }

    /**
     * 创建禁用的测试 OCR 节点。
     *
     * @param id 节点 ID
     * @param maxConcurrency 最大并发
     * @return OCR 节点
     * @author lvdaxianerplus
     * @date 2026-06-20
     */
    static OcrNode disabledNode(String id, int maxConcurrency) {
        return node(new TestNodeProperties(id, false, true, OcrNodeStatus.DISABLED, maxConcurrency));
    }

    /**
     * 创建不参与全局调度的测试 OCR 节点。
     *
     * @param id 节点 ID
     * @param maxConcurrency 最大并发
     * @return OCR 节点
     * @author lvdaxianerplus
     * @date 2026-06-20
     */
    static OcrNode localNode(String id, int maxConcurrency) {
        return node(new TestNodeProperties(id, true, false, OcrNodeStatus.UP, maxConcurrency));
    }

    /**
     * 按测试节点属性创建 OCR 节点。
     *
     * @param nodeProperties 测试节点属性
     * @return OCR 节点
     * @author lvdaxianerplus
     * @date 2026-06-20
     */
    private static OcrNode node(TestNodeProperties nodeProperties) {
        return new OcrNode(nodeProperties.id(), MODEL_KEY, nodeProperties.id(), TEST_HOST, TEST_BASE_PORT,
                nodeProperties.enabled(), nodeProperties.participateGlobal(), TEST_WEIGHT,
                nodeProperties.maxConcurrency(), nodeProperties.status(), INITIAL_COUNTER, INITIAL_COUNTER,
                INITIAL_COUNTER, INITIAL_COUNTER, Optional.empty(), Optional.empty(), Optional.empty(),
                Optional.empty(), NOW, NOW);
    }

    /**
     * 创建内存 OCR 节点仓储。
     *
     * @param nodes 节点集合
     * @return OCR 节点仓储
     * @author lvdaxianerplus
     * @date 2026-06-20
     */
    static OcrNodeRepository repository(OcrNode... nodes) {
        return new InMemoryOcrNodeRepository(List.of(nodes));
    }

    /**
     * 测试 OCR 节点构造属性。
     *
     * @param id 节点 ID
     * @param enabled 是否启用
     * @param participateGlobal 是否参与全局
     * @param status 节点状态
     * @param maxConcurrency 最大并发
     * @author lvdaxianerplus
     * @date 2026-06-20
     */
    private record TestNodeProperties(
            String id,
            boolean enabled,
            boolean participateGlobal,
            OcrNodeStatus status,
            int maxConcurrency
    ) {
    }

    /**
     * 测试用内存 OCR 节点仓储。
     *
     * @param nodes 节点集合
     * @author lvdaxianerplus
     * @date 2026-06-20
     */
    private record InMemoryOcrNodeRepository(List<OcrNode> nodes) implements OcrNodeRepository {

        @Override
        public void save(OcrNode node) {
            // 本测试只读取节点列表，不覆盖保存路径。
            throw new UnsupportedOperationException("save is not used by this test");
        }

        @Override
        public void saveAll(List<OcrNode> nodes) {
            // 本测试只读取节点列表，不覆盖批量保存路径。
            throw new UnsupportedOperationException("saveAll is not used by this test");
        }

        @Override
        public void update(OcrNode node) {
            throw new UnsupportedOperationException("update is not used by this test");
        }

        @Override
        public Optional<OcrNode> findById(String nodeId) {
            return nodes.stream().filter(node -> node.id().equals(nodeId)).findFirst();
        }

        @Override
        public List<OcrNode> listByModelKey(String modelKey) {
            return nodes.stream().filter(node -> node.modelKey().equals(modelKey)).toList();
        }

        @Override
        public List<OcrNode> listEnabled() {
            return nodes.stream().filter(OcrNode::enabled).toList();
        }

        @Override
        public List<OcrNode> listAll() {
            return nodes;
        }

        @Override
        public Optional<OcrNode> findByModelHostPort(String modelKey, String host, int port) {
            return nodes.stream()
                    .filter(node -> node.modelKey().equals(modelKey))
                    .filter(node -> node.host().equals(host))
                    .filter(node -> node.port() == port)
                    .findFirst();
        }

        @Override
        public void deleteById(String nodeId) {
            throw new UnsupportedOperationException("deleteById is not used by this test");
        }
    }
}
