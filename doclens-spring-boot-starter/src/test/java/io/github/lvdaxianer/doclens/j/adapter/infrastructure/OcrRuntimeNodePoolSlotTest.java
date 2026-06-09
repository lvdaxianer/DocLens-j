package io.github.lvdaxianer.doclens.j.adapter.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.lvdaxianer.doclens.j.adapter.application.OcrRuntimeNodeView;
import io.github.lvdaxianer.doclens.j.adapter.domain.OcrNode;
import io.github.lvdaxianer.doclens.j.adapter.domain.OcrNodeCreateRequest;
import io.github.lvdaxianer.doclens.j.adapter.domain.OcrNodeRepository;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/**
 * OCR 运行时节点池占槽测试。
 *
 * @author lvdaxianerplus
 * @date 2026-06-10
 */
class OcrRuntimeNodePoolSlotTest {

    private static final OffsetDateTime BASE_TIME = OffsetDateTime.parse("2026-06-10T10:00:00+08:00");

    /**
     * 占槽达到并发上限后应停止并记录排队数量。
     *
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    @Test
    void tryAcquireSlotStopsAtNodeConcurrencyLimitAndTracksQueue() {
        OcrRuntimeNodePool pool = new OcrRuntimeNodePool(repositoryWithSingleNode(2, 50));

        pool.initialize();

        assertThat(pool.tryAcquireSlot("node-1")).isPresent();
        assertThat(pool.tryAcquireSlot("node-1")).isPresent();
        assertThat(pool.tryAcquireSlot("node-1")).isEmpty();

        pool.incrementQueued("node-1");

        OcrRuntimeNodeView view = pool.snapshot().getFirst();
        assertThat(view.inflightImages()).isEqualTo(2);
        assertThat(view.queuedImages()).isEqualTo(1);
        assertThat(view.availableSlots()).isEqualTo(0);
    }

    /**
     * 创建单节点仓储。
     *
     * @param maxConcurrency 最大并发
     * @param weight 节点权重
     * @return OCR 节点仓储
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    private OcrNodeRepository repositoryWithSingleNode(int maxConcurrency, int weight) {
        OcrNode node = OcrNode.create(new OcrNodeCreateRequest("node-1", "paddle_ocr", "node-1",
                "127.0.0.1", 8080, true, true, weight, maxConcurrency, BASE_TIME));
        return new InMemoryOcrNodeRepository(node);
    }

    /**
     * 内存 OCR 节点仓储。
     *
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    private static final class InMemoryOcrNodeRepository implements OcrNodeRepository {

        private final OcrNode node;

        private InMemoryOcrNodeRepository(OcrNode node) {
            this.node = node;
        }

        @Override
        public void save(OcrNode node) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void saveAll(List<OcrNode> nodes) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void update(OcrNode node) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Optional<OcrNode> findById(String nodeId) {
            return Optional.of(node).filter(current -> current.id().equals(nodeId));
        }

        @Override
        public List<OcrNode> listByModelKey(String modelKey) {
            return List.of(node).stream().filter(current -> current.modelKey().equals(modelKey)).toList();
        }

        @Override
        public List<OcrNode> listEnabled() {
            return List.of(node).stream().filter(OcrNode::enabled).toList();
        }

        @Override
        public List<OcrNode> listAll() {
            return List.of(node);
        }

        @Override
        public Optional<OcrNode> findByModelHostPort(String modelKey, String host, int port) {
            return List.of(node).stream()
                    .filter(current -> current.modelKey().equals(modelKey))
                    .filter(current -> current.host().equals(host))
                    .filter(current -> current.port() == port)
                    .findFirst();
        }

        @Override
        public void deleteById(String nodeId) {
            throw new UnsupportedOperationException();
        }
    }
}
