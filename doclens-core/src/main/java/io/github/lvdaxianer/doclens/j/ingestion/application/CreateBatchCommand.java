package io.github.lvdaxianer.doclens.j.ingestion.application;

import io.github.lvdaxianer.doclens.j.adapter.domain.OcrRoutePolicy;
import io.github.lvdaxianer.doclens.j.ingestion.domain.CallerIdentity;
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
 * @param ocrRoutePolicy OCR 路由策略
 * @author lvdaxianerplus
 * @date 2026-06-09
 */
public record CreateBatchCommand(
        List<UploadFileCommand> files,
        Map<String, Object> metadata,
        String callbackUrl,
        String idempotencyKey,
        String adapterOverride,
        String pdfMode,
        OcrRoutePolicy ocrRoutePolicy,
        CallerIdentity callerIdentity
) {
    /**
     * 创建使用系统默认 OCR 路由策略的兼容命令。
     *
     * @param files 已上传文件集合
     * @param metadata 元数据载荷
     * @param callbackUrl 回调 URL
     * @param idempotencyKey 幂等键
     * @param adapterOverride 适配器覆盖值
     * @param pdfMode PDF 模式
     * @author lvdaxianerplus
     * @date 2026-06-09
     */
    public CreateBatchCommand(
            List<UploadFileCommand> files,
            Map<String, Object> metadata,
            String callbackUrl,
            String idempotencyKey,
            String adapterOverride,
            String pdfMode
    ) {
        this(files, metadata, callbackUrl, idempotencyKey, adapterOverride, pdfMode, OcrRoutePolicy.defaultPolicy(),
                CallerIdentity.anonymous());
    }

    /**
     * 创建带 OCR 路由策略的匿名调用方命令。
     *
     * @param files 已上传文件集合
     * @param metadata 元数据载荷
     * @param callbackUrl 回调 URL
     * @param idempotencyKey 幂等键
     * @param adapterOverride 适配器覆盖值
     * @param pdfMode PDF 模式
     * @param ocrRoutePolicy OCR 路由策略
     * @author lvdaxianerplus
     * @date 2026-06-17
     */
    public CreateBatchCommand(
            List<UploadFileCommand> files,
            Map<String, Object> metadata,
            String callbackUrl,
            String idempotencyKey,
            String adapterOverride,
            String pdfMode,
            OcrRoutePolicy ocrRoutePolicy
    ) {
        this(files, metadata, callbackUrl, idempotencyKey, adapterOverride, pdfMode, ocrRoutePolicy,
                CallerIdentity.anonymous());
    }

    /**
     * 创建带安全默认 OCR 路由策略的命令。
     *
     * @param files 已上传文件集合
     * @param metadata 元数据载荷
     * @param callbackUrl 回调 URL
     * @param idempotencyKey 幂等键
     * @param adapterOverride 适配器覆盖值
     * @param pdfMode PDF 模式
     * @param ocrRoutePolicy OCR 路由策略
     * @author lvdaxianerplus
     * @date 2026-06-09
     */
    public CreateBatchCommand {
        if (ocrRoutePolicy == null) {
            ocrRoutePolicy = OcrRoutePolicy.defaultPolicy();
        } else {
            // 调用方已经提供了显式 OCR 路由策略。
        }
        callerIdentity = callerIdentity == null ? CallerIdentity.anonymous() : callerIdentity;
    }
}
