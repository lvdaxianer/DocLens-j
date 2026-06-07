package io.github.lvdaxianer.doclens.j.api;

import java.util.List;
import java.util.Map;

/**
 * DocLens OCR 能力的稳定嵌入式入口。
 *
 * @author lvdaxianerplus
 * @date 2026-06-07
 */
public interface DocLensEngine {

    /**
     * 根据嵌入式 SDK 输入创建 OCR 批次。
     *
     * @param request 创建批次请求
     * @return 批次创建响应
     * @author lvdaxianerplus
     * @date 2026-06-07
     */
    Map<String, Object> createBatch(CreateBatchRequest request);

    /**
     * 获取批次状态和进度。
     *
     * @param batchId 批次标识
     * @return 批次视图
     * @author lvdaxianerplus
     * @date 2026-06-07
     */
    Map<String, Object> getBatch(String batchId);

    /**
     * 获取单个文档的状态和进度。
     *
     * @param documentId 文档标识
     * @return 文档视图
     * @author lvdaxianerplus
     * @date 2026-06-07
     */
    Map<String, Object> getDocument(String documentId);

    /**
     * 获取单个文档的 OCR 结果。
     *
     * @param documentId 文档标识
     * @return OCR 结果视图
     * @author lvdaxianerplus
     * @date 2026-06-07
     */
    Map<String, Object> getDocumentResult(String documentId);

    /**
     * 获取单个批次的事件时间线。
     *
     * @param batchId 批次标识
     * @return 事件时间线
     * @author lvdaxianerplus
     * @date 2026-06-07
     */
    Map<String, Object> getEvents(String batchId);

    /**
     * 列出 OCR 适配器能力。
     *
     * @return 适配器响应
     * @author lvdaxianerplus
     * @date 2026-06-07
     */
    Map<String, List<AdapterCapability>> listAdapters();
}
