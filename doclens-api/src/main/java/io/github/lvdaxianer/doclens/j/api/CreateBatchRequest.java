package io.github.lvdaxianer.doclens.j.api;

import java.util.List;
import java.util.Map;

/**
 * 用于创建 OCR 批次的嵌入式 SDK 请求。
 *
 * @param files 上传文件集合
 * @param metadata 业务元数据
 * @param callbackUrl 可选回调 URL
 * @param idempotencyKey 可选幂等键
 * @param adapterOverride 可选 OCR 适配器键
 * @param pdfMode 可选 PDF 处理模式
 * @author lvdaxianerplus
 * @date 2026-06-07
 */
public record CreateBatchRequest(
        List<DocumentInput> files,
        Map<String, Object> metadata,
        String callbackUrl,
        String idempotencyKey,
        String adapterOverride,
        String pdfMode
) {
}
