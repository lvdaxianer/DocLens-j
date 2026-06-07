package io.github.lvdaxianer.doclens.j.ingestion.application;

import java.util.List;
import java.util.Map;

/**
 * 创建批次命令。
 *
 * @param files 已上传文件集合
 * @param metadata 元数据载荷
 * @param callbackUrl 回调 URL
 * @param idempotencyKey 幂等键
 * @param adapterOverride 适配器覆盖值
 * @param pdfMode PDF 模式
 * @author lvdaxianerplus
 * @date 2026-06-07
 */
public record CreateBatchCommand(
        List<UploadFileCommand> files,
        Map<String, Object> metadata,
        String callbackUrl,
        String idempotencyKey,
        String adapterOverride,
        String pdfMode
) {
}
