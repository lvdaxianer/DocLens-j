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
 * @param ocrRoutingMode 可选 OCR 路由模式
 * @param ocrModelKey 可选 OCR 模型标识
 * @param ocrNodeId 可选 OCR 节点标识
 * @param ocrLoadBalanceStrategy 可选 OCR 负载均衡策略
 * @param clientId 接入方标识
 * @param sourceApp 来源应用
 * @param tenantKey 租户或业务分区键
 * @author lvdaxianerplus
 * @date 2026-06-09
 */
public record CreateBatchRequest(
        List<DocumentInput> files,
        Map<String, Object> metadata,
        String callbackUrl,
        String idempotencyKey,
        String adapterOverride,
        String pdfMode,
        String ocrRoutingMode,
        String ocrModelKey,
        String ocrNodeId,
        String ocrLoadBalanceStrategy,
        String clientId,
        String sourceApp,
        String tenantKey
) {
    /**
     * 创建不指定调用方身份的兼容请求。
     *
     * @param files 上传文件集合
     * @param metadata 业务元数据
     * @param callbackUrl 可选回调 URL
     * @param idempotencyKey 可选幂等键
     * @param adapterOverride 可选 OCR 适配器键
     * @param pdfMode 可选 PDF 处理模式
     * @param ocrRoutingMode 可选 OCR 路由模式
     * @param ocrModelKey 可选 OCR 模型标识
     * @param ocrNodeId 可选 OCR 节点标识
     * @param ocrLoadBalanceStrategy 可选 OCR 负载均衡策略
     * @author lvdaxianerplus
     * @date 2026-06-17
     */
    public CreateBatchRequest(
            List<DocumentInput> files,
            Map<String, Object> metadata,
            String callbackUrl,
            String idempotencyKey,
            String adapterOverride,
            String pdfMode,
            String ocrRoutingMode,
            String ocrModelKey,
            String ocrNodeId,
            String ocrLoadBalanceStrategy
    ) {
        this(files, metadata, callbackUrl, idempotencyKey, adapterOverride, pdfMode, ocrRoutingMode,
                ocrModelKey, ocrNodeId, ocrLoadBalanceStrategy, null, null, null);
    }

    /**
     * 创建不指定 OCR 路由策略的兼容请求。
     *
     * @param files 上传文件集合
     * @param metadata 业务元数据
     * @param callbackUrl 可选回调 URL
     * @param idempotencyKey 可选幂等键
     * @param adapterOverride 可选 OCR 适配器键
     * @param pdfMode 可选 PDF 处理模式
     * @author lvdaxianerplus
     * @date 2026-06-09
     */
    public CreateBatchRequest(
            List<DocumentInput> files,
            Map<String, Object> metadata,
            String callbackUrl,
            String idempotencyKey,
            String adapterOverride,
            String pdfMode
    ) {
        this(files, metadata, callbackUrl, idempotencyKey, adapterOverride, pdfMode, null, null, null, null,
                null, null, null);
    }
}
