package io.github.lvdaxianer.doclens.j.adapter.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.lvdaxianer.doclens.j.adapter.domain.OcrNode;
import io.github.lvdaxianer.doclens.j.adapter.domain.OcrNodeRepository;
import io.github.lvdaxianer.doclens.j.adapter.domain.OcrNodeStatus;
import io.github.lvdaxianer.doclens.j.autoconfigure.DocLensSpringProperties.PaddleOcrNodeProperties;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/**
 * OCR 节点启动初始化器测试。
 *
 * @author lvdaxianerplus
 * @date 2026-06-09
 */
class OcrNodeBootstrapperTest {

    private static final OffsetDateTime BASE_TIME = OffsetDateTime.parse("2026-06-09T07:40:00+08:00");
    private static final int TEST_NODE_CAPACITY = 4;

    /**
     * PaddleOCR 未配置节点时应插入启动节点。
     *
     * @author lvdaxianerplus
     * @date 2026-06-09
     */
    @Test
    void bootstrapInsertsPaddleNodesWhenModelHasNoNodes() {
        InMemoryOcrNodeRepository repository = new InMemoryOcrNodeRepository();
        OcrNodeBootstrapper bootstrapper = new OcrNodeBootstrapper(repository,
                List.of(bootstrapNode("paddle-215", "10.100.30.215", 8080)));

        bootstrapper.bootstrap(BASE_TIME);

        assertThat(repository.listByModelKey("paddle_ocr")).singleElement().satisfies(node -> {
            assertThat(node.name()).isEqualTo("paddle-215");
            assertThat(node.host()).isEqualTo("10.100.30.215");
            assertThat(node.port()).isEqualTo(8080);
            assertThat(node.status()).isEqualTo(OcrNodeStatus.RECOVERING);
        });
    }

    /**
     * PaddleOCR 已有页面配置节点时不应覆盖配置。
     *
     * @author lvdaxianerplus
     * @date 2026-06-09
     */
    @Test
    void bootstrapDoesNotOverwriteExistingPaddleNodes() {
        InMemoryOcrNodeRepository repository = new InMemoryOcrNodeRepository();
        repository.save(existingNode());
        OcrNodeBootstrapper bootstrapper = new OcrNodeBootstrapper(repository,
                List.of(bootstrapNode("paddle-215", "10.100.30.215", 8080)));

        bootstrapper.bootstrap(BASE_TIME);

        assertThat(repository.listByModelKey("paddle_ocr")).singleElement().satisfies(node -> {
            assertThat(node.name()).isEqualTo("page-configured");
            assertThat(node.host()).isEqualTo("127.0.0.1");
            assertThat(node.port()).isEqualTo(9000);
        });
    }

    /**
     * 创建测试启动节点配置。
     *
     * @param name 节点名称
     * @param host 节点主机
     * @param port 节点端口
     * @return 启动节点配置
     * @author lvdaxianerplus
     * @date 2026-06-09
     */
    private PaddleOcrNodeProperties bootstrapNode(String name, String host, int port) {
        return new PaddleOcrNodeProperties(name, host, port, true, true, 100, 4);
    }

    /**
     * 创建已有页面配置节点。
     *
     * @return 已有节点
     * @author lvdaxianerplus
     * @date 2026-06-09
     */
    private OcrNode existingNode() {
        return new OcrNode("existing-node", "paddle_ocr", "page-configured", "127.0.0.1", 9000, true,
                true, 100, 4, OcrNodeStatus.UP, 0L, 0L, 0L, 0L, Optional.empty(), Optional.empty(),
                Optional.empty(), Optional.empty(), BASE_TIME, BASE_TIME);
    }

    /**
     * 内存 OCR 节点仓储。
     *
     * @author lvdaxianerplus
     * @date 2026-06-09
     */
    private static class InMemoryOcrNodeRepository implements OcrNodeRepository {

        private final List<OcrNode> nodes = new ArrayList<>(TEST_NODE_CAPACITY);

        @Override
        public void save(OcrNode node) {
            nodes.add(node);
        }

        @Override
        public void saveAll(List<OcrNode> nodes) {
            this.nodes.addAll(nodes);
        }

        @Override
        public void update(OcrNode node) {
            deleteById(node.id());
            save(node);
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
            return List.copyOf(nodes);
        }

        @Override
        public Optional<OcrNode> findByModelHostPort(String modelKey, String host, int port) {
            return nodes.stream().filter(node -> sameEndpoint(node, modelKey, host, port)).findFirst();
        }

        @Override
        public void deleteById(String nodeId) {
            nodes.removeIf(node -> node.id().equals(nodeId));
        }

        /**
         * 判断节点端点是否相同。
         *
         * @param node OCR 节点
         * @param modelKey OCR 模型标识
         * @param host 主机
         * @param port 端口
         * @return true 表示端点相同
         * @author lvdaxianerplus
         * @date 2026-06-09
         */
        private boolean sameEndpoint(OcrNode node, String modelKey, String host, int port) {
            return node.modelKey().equals(modelKey) && node.host().equals(host) && node.port() == port;
        }
    }
}
