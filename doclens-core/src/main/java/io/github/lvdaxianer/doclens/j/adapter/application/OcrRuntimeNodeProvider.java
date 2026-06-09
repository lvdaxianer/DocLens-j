package io.github.lvdaxianer.doclens.j.adapter.application;

import java.util.List;
import java.util.Optional;

/**
 * OCR 运行时节点池端口。
 *
 * @author lvdaxianerplus
 * @date 2026-06-08
 */
public interface OcrRuntimeNodeProvider {

    /**
     * 导出运行时节点快照。
     *
     * @return OCR 运行时节点视图集合
     * @author lvdaxianerplus
     * @date 2026-06-08
     */
    List<OcrRuntimeNodeView> snapshot();

    /**
     * 增加节点解析中图片数。
     *
     * @param nodeId OCR 节点 ID
     * @return 更新后的运行时节点视图
     * @author lvdaxianerplus
     * @date 2026-06-08
     */
    Optional<OcrRuntimeNodeView> incrementInflight(String nodeId);

    /**
     * 减少节点解析中图片数。
     *
     * @param nodeId OCR 节点 ID
     * @return 更新后的运行时节点视图
     * @author lvdaxianerplus
     * @date 2026-06-08
     */
    Optional<OcrRuntimeNodeView> decrementInflight(String nodeId);

    /**
     * 原子尝试占用节点槽位。
     *
     * @param nodeId OCR 节点 ID
     * @return 更新后的运行时节点视图
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    Optional<OcrRuntimeNodeView> tryAcquireSlot(String nodeId);

    /**
     * 释放节点槽位。
     *
     * @param nodeId OCR 节点 ID
     * @return 更新后的运行时节点视图
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    Optional<OcrRuntimeNodeView> releaseSlot(String nodeId);

    /**
     * 增加节点排队图片数。
     *
     * @param nodeId OCR 节点 ID
     * @return 更新后的运行时节点视图
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    Optional<OcrRuntimeNodeView> incrementQueued(String nodeId);

    /**
     * 减少节点排队图片数。
     *
     * @param nodeId OCR 节点 ID
     * @return 更新后的运行时节点视图
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    Optional<OcrRuntimeNodeView> decrementQueued(String nodeId);
}
