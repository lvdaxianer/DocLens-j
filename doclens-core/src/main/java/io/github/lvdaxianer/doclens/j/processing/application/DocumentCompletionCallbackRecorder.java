package io.github.lvdaxianer.doclens.j.processing.application;

import io.github.lvdaxianer.doclens.j.ingestion.domain.Batch;
import io.github.lvdaxianer.doclens.j.ingestion.domain.BatchRepository;
import io.github.lvdaxianer.doclens.j.processing.application.DocumentProcessingEventBuilder.CompletionEventContext;
import io.github.lvdaxianer.doclens.j.processing.domain.DocumentJob;
import io.github.lvdaxianer.doclens.j.processing.domain.OcrEvent;
import io.github.lvdaxianer.doclens.j.processing.domain.OcrEventRepository;
import io.github.lvdaxianer.doclens.j.processing.domain.OcrResult;
import java.util.Optional;

/**
 * 文档完成回调记录器。
 *
 * @author lvdaxianerplus
 * @date 2026-06-16
 */
class DocumentCompletionCallbackRecorder {

    private final BatchRepository batchRepository;
    private final OcrEventRepository eventRepository;
    private final DocumentProcessingEventBuilder eventBuilder;
    private final CallbackJobCreationService callbackJobCreationService;

    /**
     * 创建文档完成回调记录器。
     *
     * @param dependencies 聚合依赖
     * @author lvdaxianerplus
     * @date 2026-06-16
     */
    DocumentCompletionCallbackRecorder(DocumentPageTaskAggregationDependencies dependencies) {
        this.batchRepository = dependencies.batchRepository();
        this.eventRepository = dependencies.eventRepository();
        this.eventBuilder = new DocumentProcessingEventBuilder(dependencies.eventFactory());
        this.callbackJobCreationService = new CallbackJobCreationService(dependencies.callbackJobRepository(),
                dependencies.idGenerator());
    }

    /**
     * 保存文档完成事件并按批次回调地址创建回调任务。
     *
     * @param completed 已完成文档
     * @param result OCR 结果
     * @author lvdaxianerplus
     * @date 2026-06-16
     */
    void record(DocumentJob completed, OcrResult result) {
        Optional<Batch> batch = batchRepository.findById(completed.batchId());
        CompletionEventContext context = new CompletionEventContext(completed, completed, completed,
                completed, result, batch);
        OcrEvent event = eventBuilder.completedEvent(context);
        eventRepository.save(event);
        batch.flatMap(Batch::callbackUrl)
                .ifPresent(callbackUrl -> callbackJobCreationService.saveForCompletedEvent(event, callbackUrl));
    }
}
