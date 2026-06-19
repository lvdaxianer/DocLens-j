package io.github.lvdaxianer.doclens.j.ingestion.interfaces;

import java.util.List;
import org.springframework.web.multipart.MultipartFile;

/**
 * 用于创建 OCR 批次的 multipart 表单对象。
 *
 * @param files 已上传文件集合
 * @param metadata 元数据 JSON
 * @param callbackUrl 回调 URL
 * @param idempotencyKey 幂等键
 * @param adapterOverride 适配器覆盖值
 * @param pdfMode PDF 模式
 * @param chunkStrategy 分块策略
 * @param ocrRoutingMode OCR 路由模式
 * @param ocrModelKey OCR 模型标识
 * @param ocrNodeId OCR 节点标识
 * @param ocrLoadBalanceStrategy OCR 负载均衡策略
 * @author lvdaxianerplus
 * @date 2026-06-09
 */
public record CreateBatchForm(
        List<MultipartFile> files,
        String metadata,
        String callbackUrl,
        String idempotencyKey,
        String adapterOverride,
        String pdfMode,
        String chunkStrategy,
        String ocrRoutingMode,
        String ocrModelKey,
        String ocrNodeId,
        String ocrLoadBalanceStrategy
) {
}
