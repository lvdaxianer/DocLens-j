package io.github.lvdaxianer.doclens.j.query.infrastructure;

import io.github.lvdaxianer.doclens.j.adapter.domain.OcrNodeCallRepository;
import io.github.lvdaxianer.doclens.j.adapter.domain.OcrNodeRepository;

/**
 * OCR Dashboard 查询侧仓储集合。
 *
 * @param nodeRepository OCR 节点仓储
 * @param callRepository OCR 调用仓储
 * @author lvdaxianerplus
 * @date 2026-06-21
 */
public record OcrDashboardDataSources(
        OcrNodeRepository nodeRepository,
        OcrNodeCallRepository callRepository
) {
}
