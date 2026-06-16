package io.github.lvdaxianer.doclens.j.processing.application;

import io.github.lvdaxianer.doclens.j.processing.domain.CallbackJob;
import io.github.lvdaxianer.doclens.j.processing.domain.CallbackJobCreateRequest;
import io.github.lvdaxianer.doclens.j.processing.domain.CallbackJobRepository;
import io.github.lvdaxianer.doclens.j.processing.domain.OcrEvent;
import io.github.lvdaxianer.doclens.j.shared.domain.DocLensConstants;
import io.github.lvdaxianer.doclens.j.shared.infrastructure.IdGenerator;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

/**
 * 文档完成回调任务创建服务。
 *
 * @author lvdaxianerplus
 * @date 2026-06-16
 */
class CallbackJobCreationService {

    private static final String CALLBACK_BODY_FIELD = "callback_body";

    private final CallbackJobRepository repository;
    private final IdGenerator idGenerator;

    /**
     * 创建文档完成回调任务创建服务。
     *
     * @param repository 回调任务仓储
     * @param idGenerator ID 生成器
     * @author lvdaxianerplus
     * @date 2026-06-16
     */
    CallbackJobCreationService(CallbackJobRepository repository, IdGenerator idGenerator) {
        this.repository = repository;
        this.idGenerator = idGenerator;
    }

    /**
     * 为文档完成事件保存回调任务。
     *
     * @param events OCR 事件集合
     * @param callbackUrl 回调地址
     * @author lvdaxianerplus
     * @date 2026-06-16
     */
    void saveForCompletedEvents(List<OcrEvent> events, String callbackUrl) {
        events.stream()
                .filter(event -> DocLensConstants.EVENT_DOCUMENT_COMPLETED.equals(event.eventType()))
                .findFirst()
                .map(event -> callbackJob(event, callbackUrl))
                .ifPresent(repository::save);
    }

    /**
     * 为单个文档完成事件保存回调任务。
     *
     * @param event OCR 完成事件
     * @param callbackUrl 回调地址
     * @author lvdaxianerplus
     * @date 2026-06-16
     */
    void saveForCompletedEvent(OcrEvent event, String callbackUrl) {
        if (DocLensConstants.EVENT_DOCUMENT_COMPLETED.equals(event.eventType())) {
            // 文档完成事件可以直接转换为待投递回调任务。
            repository.save(callbackJob(event, callbackUrl));
        } else {
            // 非完成事件缺少回调契约，调用方应先筛选事件类型。
            throw new IllegalArgumentException("callback job requires completed event: " + event.eventId());
        }
    }

    /**
     * 从文档完成事件创建回调任务。
     *
     * @param event 文档完成事件
     * @param callbackUrl 回调地址
     * @return 回调任务
     * @author lvdaxianerplus
     * @date 2026-06-16
     */
    private CallbackJob callbackJob(OcrEvent event, String callbackUrl) {
        return CallbackJob.create(new CallbackJobCreateRequest(idGenerator.newCallbackJobId(), event.eventId(),
                event.batchId(), event.documentId().orElse(null), callbackUrl, callbackPayload(event),
                OffsetDateTime.now()));
    }

    /**
     * 读取完成事件中的回调载荷。
     *
     * @param event 文档完成事件
     * @return 回调载荷
     * @author lvdaxianerplus
     * @date 2026-06-16
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> callbackPayload(OcrEvent event) {
        Object callbackBody = event.resultSummary().get(CALLBACK_BODY_FIELD);
        if (callbackBody instanceof Map<?, ?> payload) {
            // 完成事件摘要中存在回调 body 时直接作为投递载荷。
            return (Map<String, Object>) payload;
        }
        throw new IllegalStateException("document completed event missing callback_body: " + event.eventId());
    }
}
