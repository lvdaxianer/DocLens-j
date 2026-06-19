package io.github.lvdaxianer.doclens.j.ingestion.application;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.lvdaxianer.doclens.j.adapter.domain.OcrRoutePolicy;
import io.github.lvdaxianer.doclens.j.adapter.domain.OcrRoutingMode;
import io.github.lvdaxianer.doclens.j.ingestion.domain.Batch;
import io.github.lvdaxianer.doclens.j.ingestion.domain.BatchRepository;
import io.github.lvdaxianer.doclens.j.ingestion.domain.CallerIdentity;
import io.github.lvdaxianer.doclens.j.processing.application.ChunkStrategy;
import io.github.lvdaxianer.doclens.j.processing.domain.DocumentJob;
import io.github.lvdaxianer.doclens.j.processing.domain.DocumentJobRepository;
import io.github.lvdaxianer.doclens.j.processing.domain.OcrEvent;
import io.github.lvdaxianer.doclens.j.processing.domain.OcrEventFactory;
import io.github.lvdaxianer.doclens.j.processing.domain.OcrEventRepository;
import io.github.lvdaxianer.doclens.j.shared.application.TransactionRunner;
import io.github.lvdaxianer.doclens.j.shared.config.DocLensProperties;
import io.github.lvdaxianer.doclens.j.shared.infrastructure.IdGenerator;
import io.github.lvdaxianer.doclens.j.storage.ObjectStorage;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/**
 * 创建批次用例测试。
 *
 * @author lvdaxianerplus
 * @date 2026-06-08
 */
class CreateBatchUseCaseTest {

    private static final int TEST_BATCH_CAPACITY = 2;
    private static final int TEST_DOCUMENT_CAPACITY = 4;
    private static final int TEST_EVENT_CAPACITY = 8;
    private static final int TEST_OBJECT_CAPACITY = 4;
    private static final String TEST_CLIENT_ID = "rag-flow";
    private static final String TEST_SOURCE_APP = "knowledge-base";
    private static final String TEST_TENANT_KEY = "tenant-east";

    private InMemoryBatchRepository lastBatchRepository;
    private InMemoryDocumentJobRepository lastDocumentRepository;

    /**
     * 自动处理应只提交后台调度，避免上传请求等待 OCR 完成。
     *
     * @author lvdaxianerplus
     * @date 2026-06-08
     */
    @Test
    void createSchedulesProcessingWithoutRunningOcrInline() {
        RecordingBatchProcessingScheduler scheduler = new RecordingBatchProcessingScheduler();
        CreateBatchUseCase useCase = createUseCase(scheduler, true);
        CreateBatchCommand command = commandWithTextFile();

        Map<String, Object> response = useCase.create(command);

        assertThat(response).containsEntry("status", "queued");
        assertThat(scheduler.batchIds).containsExactly(String.valueOf(response.get("batch_id")));
        assertThat(scheduler.wasRunInline).isFalse();
    }

    /**
     * 上传时选择的 OCR 路由策略应快照到每个文档任务。
     *
     * @author lvdaxianerplus
     * @date 2026-06-09
     */
    @Test
    void createPersistsUploadOcrRoutePolicyOnDocuments() {
        RecordingBatchProcessingScheduler scheduler = new RecordingBatchProcessingScheduler();
        CreateBatchUseCase useCase = createUseCase(scheduler, false);
        CreateBatchCommand command = modelRouteCommand();

        useCase.create(command);

        assertThat(lastDocumentRepository.documents.values())
                .extracting(DocumentJob::ocrRoutePolicy)
                .allSatisfy(policy -> {
                    assertThat(policy.routingMode()).isEqualTo(OcrRoutingMode.MODEL_LOAD_BALANCE);
                    assertThat(policy.modelKey()).contains("paddle_ocr");
                    assertThat(policy.loadBalanceStrategy()).contains("least-inflight");
                });
    }

    /**
     * 创建批次应保存可信调用方归因，并在上传响应中返回调用方字段。
     *
     * @author lvdaxianerplus
     * @date 2026-06-17
     */
    @Test
    void createPersistsCallerIdentityAndReturnsIt() {
        RecordingBatchProcessingScheduler scheduler = new RecordingBatchProcessingScheduler();
        CreateBatchUseCase useCase = createUseCase(scheduler, false);
        CreateBatchCommand command = callerAttributedCommand();

        Map<String, Object> response = useCase.create(command);
        String batchId = String.valueOf(response.get("batch_id"));

        assertThat(lastBatchRepository.batches.get(batchId).callerIdentity().clientId()).isEqualTo(TEST_CLIENT_ID);
        assertThat(lastBatchRepository.batches.get(batchId).callerIdentity().sourceApp()).isEqualTo(TEST_SOURCE_APP);
        assertThat(lastBatchRepository.batches.get(batchId).callerIdentity().tenantKey()).contains(TEST_TENANT_KEY);
        assertThat(response)
                .containsEntry("client_id", TEST_CLIENT_ID)
                .containsEntry("source_app", TEST_SOURCE_APP)
                .containsEntry("tenant_key", TEST_TENANT_KEY);
    }

    /**
     * 创建批次测试用例。
     *
     * @param scheduler 批次处理调度器
     * @param autoProcessOnUpload 是否自动调度处理
     * @return 创建批次用例
     * @author lvdaxianerplus
     * @date 2026-06-08
     */
    private CreateBatchUseCase createUseCase(
            RecordingBatchProcessingScheduler scheduler,
            boolean autoProcessOnUpload
    ) {
        InMemoryBatchRepository batchRepository = new InMemoryBatchRepository();
        InMemoryDocumentJobRepository documentRepository = new InMemoryDocumentJobRepository();
        lastBatchRepository = batchRepository;
        lastDocumentRepository = documentRepository;
        InMemoryOcrEventRepository eventRepository = new InMemoryOcrEventRepository();
        CreateBatchDependencies dependencies = new CreateBatchDependencies(batchRepository, documentRepository,
                eventRepository, new InMemoryObjectStorage(), new IdGenerator(), properties(autoProcessOnUpload),
                scheduler, new OcrEventFactory(new IdGenerator()));
        return new CreateBatchUseCase(dependencies, new InlineTransactionRunner());
    }

    /**
     * 创建包含文本文件的命令。
     *
     * @return 创建批次命令
     * @author lvdaxianerplus
     * @date 2026-06-08
     */
    private CreateBatchCommand commandWithTextFile() {
        UploadFileCommand file = new UploadFileCommand("hello.txt", "hello".getBytes());
        return new CreateBatchCommand(List.of(file), Map.of("source", "test"), null, "idem-test", null, null,
                ChunkStrategy.GENERAL);
    }

    /**
     * 创建指定 OCR 模型负载均衡的测试命令。
     *
     * @return 创建批次命令
     * @author lvdaxianerplus
     * @date 2026-06-09
     */
    private CreateBatchCommand modelRouteCommand() {
        UploadFileCommand file = new UploadFileCommand("hello.png", "image".getBytes());
        OcrRoutePolicy routePolicy = OcrRoutePolicy.modelLoadBalance("paddle_ocr", "least-inflight");
        return new CreateBatchCommand(List.of(file), Map.of("source", "test"), null,
                "idem-route-test", null, null, ChunkStrategy.TECHNICAL, routePolicy);
    }

    /**
     * 创建带调用方归因的测试命令。
     *
     * @return 创建批次命令
     * @author lvdaxianerplus
     * @date 2026-06-17
     */
    private CreateBatchCommand callerAttributedCommand() {
        UploadFileCommand file = new UploadFileCommand("caller.txt", "hello".getBytes());
        CallerIdentity caller = new CallerIdentity(TEST_CLIENT_ID, TEST_SOURCE_APP, Optional.of(TEST_TENANT_KEY));
        return new CreateBatchCommand(List.of(file), Map.of("source", "test"), null,
                "idem-caller-test", null, null, ChunkStrategy.GENERAL, OcrRoutePolicy.defaultPolicy(), caller);
    }

    /**
     * 创建测试配置。
     *
     * @param autoProcessOnUpload 是否自动调度处理
     * @return DocLens 配置
     * @author lvdaxianerplus
     * @date 2026-06-08
     */
    private DocLensProperties properties(boolean autoProcessOnUpload) {
        return new DocLensProperties("target/test-storage", autoProcessOnUpload, "worker-test",
                new DocLensProperties.CallbackProperties(1, 5),
                new DocLensProperties.AdapterProperties("stub_ocr"),
                new DocLensProperties.PaddleOcrProperties(false, "http://127.0.0.1:8080/ocr", 5, false),
                new DocLensProperties.OcrHealthProperties(3, 2),
                new DocLensProperties.ExtractionProperties(1),
                new DocLensProperties.PdfRenderProperties(72, "png"),
                new DocLensProperties.WordConversionProperties("soffice", 5),
                threadPools());
    }

    /**
     * 创建测试线程池隔离配置。
     *
     * @return 线程池隔离配置
     * @author lvdaxianerplus
     * @date 2026-06-08
     */
    private DocLensProperties.ThreadPoolsProperties threadPools() {
        DocLensProperties.ThreadPoolProperties pool = new DocLensProperties.ThreadPoolProperties(1, 1, 1, 1,
                "doclens-test-");
        return new DocLensProperties.ThreadPoolsProperties(pool, pool, pool, pool);
    }

    /**
     * 记录批次调度调用。
     *
     * @author lvdaxianerplus
     * @date 2026-06-08
     */
    private static class RecordingBatchProcessingScheduler implements BatchProcessingScheduler {

        private final List<String> batchIds = new ArrayList<>(TEST_BATCH_CAPACITY);
        private boolean wasRunInline;

        @Override
        public void schedule(String batchId) {
            batchIds.add(batchId);
        }
    }

    /**
     * 内存批次仓储。
     *
     * @author lvdaxianerplus
     * @date 2026-06-08
     */
    private static class InMemoryBatchRepository implements BatchRepository {

        private final Map<String, Batch> batches = new HashMap<>(TEST_BATCH_CAPACITY);

        @Override
        public void save(Batch batch) {
            batches.put(batch.batchId(), batch);
        }

        @Override
        public Optional<Batch> findById(String batchId) {
            return Optional.ofNullable(batches.get(batchId));
        }

        @Override
        public Optional<Batch> findByIdempotencyKey(String idempotencyKey) {
            return batches.values().stream()
                    .filter(batch -> batch.idempotencyKey().filter(idempotencyKey::equals).isPresent())
                    .findFirst();
        }

        @Override
        public List<Batch> listRecent(int limit) {
            return batches.values().stream().limit(limit).toList();
        }

        @Override
        public void updateSummary(String batchId, int completedFiles, int failedFiles, io.github.lvdaxianer.doclens.j.ingestion.domain.BatchStatus status) {
            // 当前测试只验证创建与调度。
        }
    }

    /**
     * 内存文档仓储。
     *
     * @author lvdaxianerplus
     * @date 2026-06-08
     */
    private static class InMemoryDocumentJobRepository implements DocumentJobRepository {

        private final Map<String, DocumentJob> documents = new HashMap<>(TEST_DOCUMENT_CAPACITY);

        @Override
        public void save(DocumentJob document) {
            documents.put(document.documentId(), document);
        }

        @Override
        public void saveAll(List<DocumentJob> documents) {
            documents.forEach(this::save);
        }

        @Override
        public void update(DocumentJob document) {
            documents.put(document.documentId(), document);
        }

        @Override
        public void updateAll(List<DocumentJob> documents) {
            documents.forEach(this::update);
        }

        @Override
        public Optional<DocumentJob> findById(String documentId) {
            return Optional.ofNullable(documents.get(documentId));
        }

        @Override
        public List<DocumentJob> listByBatchId(String batchId) {
            return documents.values().stream().filter(document -> batchId.equals(document.batchId())).toList();
        }

        @Override
        public List<DocumentJob> listByBatchIds(List<String> batchIds) {
            return documents.values().stream().filter(document -> batchIds.contains(document.batchId())).toList();
        }

        @Override
        public List<DocumentJob> listRecent(int limit) {
            return documents.values().stream().limit(limit).toList();
        }
    }

    /**
     * 内存事件仓储。
     *
     * @author lvdaxianerplus
     * @date 2026-06-08
     */
    private static class InMemoryOcrEventRepository implements OcrEventRepository {

        private final List<OcrEvent> events = new ArrayList<>(TEST_EVENT_CAPACITY);

        @Override
        public void save(OcrEvent event) {
            events.add(event);
        }

        @Override
        public void saveAll(List<OcrEvent> events) {
            this.events.addAll(events);
        }

        @Override
        public List<OcrEvent> listByBatchId(String batchId) {
            return events.stream().filter(event -> batchId.equals(event.batchId())).toList();
        }

        @Override
        public List<OcrEvent> listRecent(int limit) {
            return events.stream().limit(limit).toList();
        }
    }

    /**
     * 内存对象存储。
     *
     * @author lvdaxianerplus
     * @date 2026-06-08
     */
    private static class InMemoryObjectStorage implements ObjectStorage {

        private final Map<String, byte[]> objects = new HashMap<>(TEST_OBJECT_CAPACITY);

        @Override
        public String writeBytes(String objectKey, byte[] content) {
            objects.put(objectKey, content);
            return objectKey;
        }

        @Override
        public byte[] readBytes(String storageUri) {
            return objects.get(storageUri);
        }
    }

    /**
     * 直接执行事务动作的测试事务器。
     *
     * @author lvdaxianerplus
     * @date 2026-06-08
     */
    private static class InlineTransactionRunner implements TransactionRunner {

        @Override
        public <T> T requiredResult(java.util.function.Supplier<T> action) {
            return action.get();
        }

        @Override
        public void requiredVoid(Runnable action) {
            action.run();
        }
    }
}
