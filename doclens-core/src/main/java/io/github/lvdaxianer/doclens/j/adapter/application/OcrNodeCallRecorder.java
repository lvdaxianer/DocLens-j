package io.github.lvdaxianer.doclens.j.adapter.application;

import io.github.lvdaxianer.doclens.j.adapter.domain.OcrNodeCall;
import io.github.lvdaxianer.doclens.j.adapter.domain.OcrNodeCallCreateRequest;
import io.github.lvdaxianer.doclens.j.adapter.domain.OcrNodeCallRepository;
import java.time.OffsetDateTime;
import java.util.Optional;

/**
 * OCR 节点调用记录器。
 *
 * @author lvdaxianerplus
 * @date 2026-06-08
 */
class OcrNodeCallRecorder {

    private final OcrNodeCallRepository callRepository;
    private final OcrCallIdGenerator callIdGenerator;

    /**
     * 创建 OCR 节点调用记录器。
     *
     * @param callRepository OCR 调用记录仓储
     * @param callIdGenerator OCR 调用记录 ID 生成器
     * @author lvdaxianerplus
     * @date 2026-06-08
     */
    OcrNodeCallRecorder(OcrNodeCallRepository callRepository, OcrCallIdGenerator callIdGenerator) {
        this.callRepository = callRepository;
        this.callIdGenerator = callIdGenerator;
    }

    /**
     * 保存 OCR 节点调用记录。
     *
     * @param request 调用记录请求
     * @author lvdaxianerplus
     * @date 2026-06-08
     */
    void save(OcrNodeCallRecordRequest request) {
        callRepository.save(OcrNodeCall.create(new OcrNodeCallCreateRequest(
                callIdGenerator.newOcrCallId(), request.imageRequest().batchId(),
                request.imageRequest().documentId(), request.imageRequest().pageNo(), request.node().modelKey(),
                request.node().nodeId(), request.policy().routingMode(), request.status(), request.retryCount(),
                request.elapsedMs(), request.errorCode(), request.errorMessage(), request.startedAt(),
                Optional.of(OffsetDateTime.now()))));
    }
}
