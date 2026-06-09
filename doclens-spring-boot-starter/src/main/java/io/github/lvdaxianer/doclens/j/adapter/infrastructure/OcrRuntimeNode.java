package io.github.lvdaxianer.doclens.j.adapter.infrastructure;

import io.github.lvdaxianer.doclens.j.adapter.application.OcrRuntimeNodeView;
import io.github.lvdaxianer.doclens.j.adapter.domain.OcrNode;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * OCR 运行时节点状态。
 *
 * @author lvdaxianerplus
 * @date 2026-06-08
 */
public class OcrRuntimeNode {

    private volatile OcrNode node;
    private final AtomicInteger inflightImages = new AtomicInteger(0);
    private final AtomicInteger queuedImages = new AtomicInteger(0);

    /**
     * 创建 OCR 运行时节点。
     *
     * @param node OCR 节点领域对象
     * @author lvdaxianerplus
     * @date 2026-06-08
     */
    public OcrRuntimeNode(OcrNode node) {
        this.node = node;
    }

    /**
     * 使用最新节点元数据更新运行时节点。
     *
     * @param latestNode 最新 OCR 节点领域对象
     * @return 当前运行时节点
     * @author lvdaxianerplus
     * @date 2026-06-09
     */
    public OcrRuntimeNode refresh(OcrNode latestNode) {
        this.node = latestNode;
        return this;
    }

    /**
     * 增加解析中图片数。
     *
     * @return 增加后的解析中图片数
     * @author lvdaxianerplus
     * @date 2026-06-08
     */
    public int incrementInflight() {
        return inflightImages.incrementAndGet();
    }

    /**
     * 减少解析中图片数。
     *
     * @return 减少后的解析中图片数
     * @author lvdaxianerplus
     * @date 2026-06-08
     */
    public int decrementInflight() {
        return inflightImages.updateAndGet(value -> Math.max(0, value - 1));
    }

    /**
     * 判断当前节点是否仍有可用槽位。
     *
     * @return 是否存在可用槽位
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    public boolean hasAvailableSlot() {
        return inflightImages.get() < node.maxConcurrency();
    }

    /**
     * 原子尝试占用一个运行槽位。
     *
     * @return 是否成功占用
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    public boolean tryAcquireSlot() {
        while (true) {
            int current = inflightImages.get();
            if (current >= node.maxConcurrency()) {
                return false;
            } else if (inflightImages.compareAndSet(current, current + 1)) {
                return true;
            } else {
                // CAS 失败说明有并发更新，继续重试直到成功或无槽位。
            }
        }
    }

    /**
     * 释放一个运行槽位。
     *
     * @return 释放后的运行中图片数
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    public int releaseSlot() {
        return decrementInflight();
    }

    /**
     * 增加排队图片数。
     *
     * @return 增加后的排队图片数
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    public int incrementQueued() {
        return queuedImages.incrementAndGet();
    }

    /**
     * 减少排队图片数。
     *
     * @return 减少后的排队图片数
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    public int decrementQueued() {
        return queuedImages.updateAndGet(value -> Math.max(0, value - 1));
    }

    /**
     * 转换为选择器使用的运行时节点视图。
     *
     * @return OCR 运行时节点视图
     * @author lvdaxianerplus
     * @date 2026-06-08
     */
    public OcrRuntimeNodeView toView() {
        int inflight = inflightImages.get();
        int queued = queuedImages.get();
        int maxConcurrency = node.maxConcurrency();
        return new OcrRuntimeNodeView(node.id(), node.modelKey(), node.enabled(), node.participateGlobal(),
                node.status(), node.weight(), maxConcurrency, inflight, queued,
                Math.max(0, maxConcurrency - inflight), node.avgLatencyMs(), node.circuitOpenUntil(),
                node.failureCount(), node.successCount());
    }

    /**
     * 返回节点领域对象。
     *
     * @return OCR 节点
     * @author lvdaxianerplus
     * @date 2026-06-08
     */
    public OcrNode node() {
        return node;
    }
}
