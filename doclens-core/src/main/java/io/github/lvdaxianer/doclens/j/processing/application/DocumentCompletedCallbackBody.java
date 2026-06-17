package io.github.lvdaxianer.doclens.j.processing.application;

import io.github.lvdaxianer.doclens.j.ingestion.domain.Batch;
import io.github.lvdaxianer.doclens.j.ingestion.domain.CallerIdentity;
import io.github.lvdaxianer.doclens.j.processing.domain.DocumentJob;
import io.github.lvdaxianer.doclens.j.processing.domain.OcrResult;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 文档解析完成后的回调 body 契约。
 *
 * @param meta 上传元数据
 * @param text 解析结果文本
 * @param idempotencyKey 上传幂等键
 * @param caller 调用方身份
 * @author lvdaxianerplus
 * @date 2026-06-09
 */
public record DocumentCompletedCallbackBody(
        Map<String, Object> meta,
        String text,
        String idempotencyKey,
        CallerIdentity caller
) {
    private static final String META_FIELD = "meta";
    private static final String TEXT_FIELD = "text";
    private static final String IDEMPOTENCY_KEY_FIELD = "idempotency_key";
    private static final String CALLER_FIELD = "caller";

    /**
     * 创建带安全默认值的回调 body。
     *
     * @author lvdaxianerplus
     * @date 2026-06-09
     */
    public DocumentCompletedCallbackBody {
        meta = meta == null ? Map.of() : Collections.unmodifiableMap(new LinkedHashMap<>(meta));
        text = text == null ? "" : text;
        idempotencyKey = idempotencyKey == null ? "" : idempotencyKey;
        caller = caller == null ? CallerIdentity.anonymous() : caller;
    }

    /**
     * 创建匿名调用方回调 body。
     *
     * @param meta 上传元数据
     * @param text 解析结果文本
     * @param idempotencyKey 上传幂等键
     * @author lvdaxianerplus
     * @date 2026-06-17
     */
    public DocumentCompletedCallbackBody(Map<String, Object> meta, String text, String idempotencyKey) {
        this(meta, text, idempotencyKey, CallerIdentity.anonymous());
    }

    /**
     * 基于批次、文档和结果创建回调 body。
     *
     * @param batch 批次聚合
     * @param document 文档任务
     * @param result OCR 结果
     * @return 回调 body
     * @author lvdaxianerplus
     * @date 2026-06-09
     */
    public static DocumentCompletedCallbackBody from(Batch batch, DocumentJob document, OcrResult result) {
        return new DocumentCompletedCallbackBody(document.metadata().values(), result.finalText(),
                batch.idempotencyKey().orElse(""), batch.callerIdentity());
    }

    /**
     * 转换为 JSON 对象 Map。
     *
     * @return JSON 对象 Map
     * @author lvdaxianerplus
     * @date 2026-06-09
     */
    public Map<String, Object> toMap() {
        return Map.of(META_FIELD, meta, TEXT_FIELD, text, IDEMPOTENCY_KEY_FIELD, idempotencyKey,
                CALLER_FIELD, caller.toMap());
    }
}
