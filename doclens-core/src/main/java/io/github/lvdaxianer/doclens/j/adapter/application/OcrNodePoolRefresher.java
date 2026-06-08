package io.github.lvdaxianer.doclens.j.adapter.application;

/**
 * OCR 运行时节点池刷新端口。
 *
 * @author lvdaxianerplus
 * @date 2026-06-09
 */
@FunctionalInterface
public interface OcrNodePoolRefresher {

    /**
     * 刷新运行时 OCR 节点池。
     *
     * @author lvdaxianerplus
     * @date 2026-06-09
     */
    void refresh();
}
