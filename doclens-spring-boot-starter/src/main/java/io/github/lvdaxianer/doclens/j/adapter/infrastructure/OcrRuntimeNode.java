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
     * 转换为选择器使用的运行时节点视图。
     *
     * @return OCR 运行时节点视图
     * @author lvdaxianerplus
     * @date 2026-06-08
     */
    public OcrRuntimeNodeView toView() {
        return new OcrRuntimeNodeView(node.id(), node.modelKey(), node.enabled(), node.participateGlobal(),
                node.status(), node.maxConcurrency(), inflightImages.get(), node.avgLatencyMs());
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
