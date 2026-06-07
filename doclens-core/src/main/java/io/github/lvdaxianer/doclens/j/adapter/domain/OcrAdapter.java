package io.github.lvdaxianer.doclens.j.adapter.domain;

import io.github.lvdaxianer.doclens.j.api.AdapterCapability;
import io.github.lvdaxianer.doclens.j.processing.domain.DocumentJob;
import java.util.Map;

/**
 * OCR 适配器防腐接口。
 *
 * @author lvdaxianerplus
 * @date 2026-06-07
 */
public interface OcrAdapter {

    /**
     * 返回适配器能力。
     *
     * @return 适配器能力
     * @author lvdaxianerplus
     * @date 2026-06-07
     */
    AdapterCapability capability();

    /**
     * 将文档任务解析为厂商无关的原始输出。
     *
     * @param document 文档任务
     * @return 厂商输出
     * @author lvdaxianerplus
     * @date 2026-06-07
     */
    Map<String, Object> parse(DocumentJob document);
}
