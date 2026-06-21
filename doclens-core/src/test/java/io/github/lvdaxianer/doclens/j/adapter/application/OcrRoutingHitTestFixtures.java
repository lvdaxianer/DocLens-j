package io.github.lvdaxianer.doclens.j.adapter.application;

import static io.github.lvdaxianer.doclens.j.adapter.application.OcrRoutingTestFixtures.TEST_CALL_CAPACITY;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * OCR 路由批次命中测试夹具。
 *
 * @author lvdaxianerplus
 * @date 2026-06-11
 */
final class OcrRoutingHitTestFixtures {

    /*
     * 本夹具只放批次命中统计相关测试桩。
     * 这样普通 OCR 路由夹具可以保持轻量，
     * 并发运行态快照也能独立演进。
     */

    private OcrRoutingHitTestFixtures() {
    }

    /**
     * 内存批次运行时命中跟踪器。
     *
     * @author lvdaxianerplus
     * @date 2026-06-11
     */
    static class InMemoryBatchHitTracker implements OcrBatchHitTracker {

        /*
         * 使用扁平 key 保存 batch + document + model + node 四段信息。
         * 测试关注的是计数快照行为，不需要引入多层 Map 增加阅读成本。
         */

        private static final int KEY_SEGMENT_COUNT = 4;
        private static final String KEY_SEPARATOR = "|";
        private final Map<String, AtomicLong> hitCounts = new ConcurrentHashMap<>(TEST_CALL_CAPACITY);

        @Override
        public void recordDispatch(OcrBatchNodeHitCommand command) {
            hitCounts.computeIfAbsent(hitKey(command), ignored -> new AtomicLong(0L))
                    .incrementAndGet();
        }

        @Override
        public void recordCompletion(OcrBatchNodeHitCommand command) {
            String key = hitKey(command);
            AtomicLong counter = hitCounts.get(key);
            if (counter == null) {
                // 测试中未记录派发时收到完成回调，直接忽略避免负计数。
                return;
            }
            resetWhenEmpty(counter.decrementAndGet(), counter);
        }

        @Override
        public List<OcrBatchNodeHit> snapshotByBatch(String batchId) {
            return hitCounts.entrySet().stream()
                    .filter(entry -> entry.getValue().get() > 0L)
                    .map(entry -> toHit(entry.getKey(), entry.getValue().get()))
                    .filter(hit -> batchId.equals(hit.batchId()))
                    .sorted(Comparator.comparing(OcrBatchNodeHit::nodeId))
                    .toList();
        }

        /**
         * 归零已完成的命中计数。
         *
         * @param currentValue 当前计数值
         * @param counter 命中计数器
         * @author lvdaxianerplus
         * @date 2026-06-11
         */
        private void resetWhenEmpty(long currentValue, AtomicLong counter) {
            if (currentValue <= 0L) {
                // 保持与生产实现一致：归零但不删除计数器。
                counter.set(0L);
            }
        }

        /**
         * 组装批次命中键，避免维护多层并发结构。
         *
         * @param command 运行中节点命中计数命令
         * @return 命中键
         * @author lvdaxianerplus
         * @date 2026-06-21
         */
        private String hitKey(OcrBatchNodeHitCommand command) {
            return String.join(KEY_SEPARATOR, command.batchId(), command.documentId(),
                    command.modelKey(), command.nodeId());
        }

        /**
         * 将命中键转换为快照对象。
         *
         * @param key 命中键
         * @param imageCount 图片数
         * @return 命中快照
         * @author lvdaxianerplus
         * @date 2026-06-11
         */
        private OcrBatchNodeHit toHit(String key, long imageCount) {
            String[] segments = key.split("\\|", KEY_SEGMENT_COUNT);
            return new OcrBatchNodeHit(segments[0], segments[1], segments[2], segments[3], imageCount);
        }
    }
}
