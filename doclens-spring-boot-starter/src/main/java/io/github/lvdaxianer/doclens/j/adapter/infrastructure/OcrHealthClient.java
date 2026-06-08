package io.github.lvdaxianer.doclens.j.adapter.infrastructure;

import io.github.lvdaxianer.doclens.j.adapter.domain.OcrNode;

/**
 * OCR 节点健康检查客户端。
 *
 * @author lvdaxianerplus
 * @date 2026-06-08
 */
public interface OcrHealthClient {

    /**
     * 判断 OCR 节点是否健康。
     *
     * @param node OCR 节点
     * @return 是否健康
     * @author lvdaxianerplus
     * @date 2026-06-08
     */
    boolean isHealthy(OcrNode node);
}
