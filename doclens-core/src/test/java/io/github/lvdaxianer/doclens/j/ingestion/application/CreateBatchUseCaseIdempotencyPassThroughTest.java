package io.github.lvdaxianer.doclens.j.ingestion.application;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.lvdaxianer.doclens.j.ingestion.domain.Batch;
import io.github.lvdaxianer.doclens.j.ingestion.domain.BatchRepository;
import io.github.lvdaxianer.doclens.j.ingestion.domain.BatchStatus;
import io.github.lvdaxianer.doclens.j.processing.application.ChunkStrategy;
import io.github.lvdaxianer.doclens.j.processing.domain.DocumentJobRepository;
import io.github.lvdaxianer.doclens.j.processing.domain.OcrEventFactory;
import io.github.lvdaxianer.doclens.j.processing.domain.OcrEventRepository;
import io.github.lvdaxianer.doclens.j.shared.application.TransactionRunner;
import io.github.lvdaxianer.doclens.j.shared.config.DocLensProperties;
import io.github.lvdaxianer.doclens.j.shared.infrastructure.IdGenerator;
import io.github.lvdaxianer.doclens.j.storage.ObjectStorage;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/**
 * 创建批次幂等键透传语义测试。
 *
 * @author lvdaxianerplus
 * @date 2026-06-16
 */
class CreateBatchUseCaseIdempotencyPassThroughTest {

    private static final int EXPECTED_DUPLICATE_BATCHES = 2;
    private static final int TEST_REPOSITORY_CAPACITY = 2;
    private static final String IDEMPOTENCY_KEY = "idem-test";
    private static final String FILE_NAME = "hello.txt";
    private static final String FILE_CONTENT = "hello";

    /**
     * 重复幂等键应作为第三方透传值保存，不应阻止创建新的批次。
     *
     * @author lvdaxianerplus
     * @date 2026-06-16
     */
    @Test
    void createAcceptsDuplicateIdempotencyKeyAsPassThroughValue() {
        RecordingBatchRepository batchRepository = new RecordingBatchRepository();
        CreateBatchUseCase useCase = createUseCase(batchRepository);
        CreateBatchCommand command = commandWithTextFile();

        Map<String, Object> firstResponse = useCase.create(command);
        Map<String, Object> secondResponse = useCase.create(command);

        assertThat(secondResponse.get("batch_id")).isNotEqualTo(firstResponse.get("batch_id"));
        assertThat(batchRepository.batches).hasSize(EXPECTED_DUPLICATE_BATCHES);
        assertThat(batchRepository.batches)
                .allSatisfy(batch -> assertThat(batch.idempotencyKey()).contains(IDEMPOTENCY_KEY));
    }

    /**
     * 创建批次用例。
     *
     * @param batchRepository 批次仓储
     * @return 创建批次用例
     * @author lvdaxianerplus
     * @date 2026-06-16
     */
    private CreateBatchUseCase createUseCase(RecordingBatchRepository batchRepository) {
        IdGenerator idGenerator = new IdGenerator();
        CreateBatchDependencies dependencies = new CreateBatchDependencies(batchRepository,
                stub(DocumentJobRepository.class), stub(OcrEventRepository.class), objectStorage(), idGenerator,
                properties(), batchId -> { }, new OcrEventFactory(idGenerator));
        return new CreateBatchUseCase(dependencies, transactionRunner());
    }

    /**
     * 创建包含文本文件的命令。
     *
     * @return 创建批次命令
     * @author lvdaxianerplus
     * @date 2026-06-16
     */
    private CreateBatchCommand commandWithTextFile() {
        UploadFileCommand file = new UploadFileCommand(FILE_NAME, FILE_CONTENT.getBytes());
        return new CreateBatchCommand(List.of(file), Map.of("source", "test"), null, IDEMPOTENCY_KEY, null, null,
                ChunkStrategy.GENERAL);
    }

    /**
     * 创建测试配置。
     *
     * @return DocLens 配置
     * @author lvdaxianerplus
     * @date 2026-06-16
     */
    private DocLensProperties properties() {
        return new DocLensProperties("target/test-storage", false, "worker-test",
                new DocLensProperties.CallbackProperties(1, 5),
                new DocLensProperties.AdapterProperties("stub_ocr"),
                new DocLensProperties.PaddleOcrProperties(false, "http://127.0.0.1:8080/ocr", 5, false),
                new DocLensProperties.OcrHealthProperties(3, 2),
                new DocLensProperties.ExtractionProperties(1),
                new DocLensProperties.PdfRenderProperties(72, "png"),
                new DocLensProperties.WordConversionProperties("soffice", 5), threadPools());
    }

    /**
     * 创建测试线程池配置。
     *
     * @return 线程池配置
     * @author lvdaxianerplus
     * @date 2026-06-16
     */
    private DocLensProperties.ThreadPoolsProperties threadPools() {
        DocLensProperties.ThreadPoolProperties pool = new DocLensProperties.ThreadPoolProperties(1, 1, 1, 1,
                "doclens-test-");
        return new DocLensProperties.ThreadPoolsProperties(pool, pool, pool, pool);
    }

    /**
     * 创建对象存储测试替身。
     *
     * @return 对象存储
     * @author lvdaxianerplus
     * @date 2026-06-16
     */
    private ObjectStorage objectStorage() {
        return stub(ObjectStorage.class, (proxy, method, args) -> {
            if ("writeBytes".equals(method.getName())) {
                return args[0];
            } else {
                return new byte[0];
            }
        });
    }

    /**
     * 创建事务测试替身。
     *
     * @return 事务执行器
     * @author lvdaxianerplus
     * @date 2026-06-16
     */
    private TransactionRunner transactionRunner() {
        return stub(TransactionRunner.class, (proxy, method, args) -> {
            if ("requiredResult".equals(method.getName())) {
                return ((java.util.function.Supplier<?>) args[0]).get();
            } else {
                ((Runnable) args[0]).run();
                return null;
            }
        });
    }

    /**
     * 创建返回默认值的接口测试替身。
     *
     * @param type 接口类型
     * @param <T> 接口泛型
     * @return 测试替身
     * @author lvdaxianerplus
     * @date 2026-06-16
     */
    private <T> T stub(Class<T> type) {
        return stub(type, (proxy, method, args) -> defaultValue(method.getReturnType()));
    }

    /**
     * 创建自定义行为的接口测试替身。
     *
     * @param type 接口类型
     * @param handler 调用处理器
     * @param <T> 接口泛型
     * @return 测试替身
     * @author lvdaxianerplus
     * @date 2026-06-16
     */
    private <T> T stub(Class<T> type, InvocationHandler handler) {
        Object proxy = Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[] { type }, handler);
        return type.cast(proxy);
    }

    /**
     * 返回接口方法默认值。
     *
     * @param returnType 返回类型
     * @return 默认值
     * @author lvdaxianerplus
     * @date 2026-06-16
     */
    private Object defaultValue(Class<?> returnType) {
        if (returnType == Optional.class) {
            return Optional.empty();
        } else if (returnType == List.class) {
            return List.of();
        } else {
            return null;
        }
    }

    /**
     * 记录批次保存的测试仓储。
     *
     * @author lvdaxianerplus
     * @date 2026-06-16
     */
    private static class RecordingBatchRepository implements BatchRepository {

        private final List<Batch> batches = new ArrayList<>(TEST_REPOSITORY_CAPACITY);

        /**
         * 保存批次。
         *
         * @param batch 批次
         * @author lvdaxianerplus
         * @date 2026-06-16
         */
        @Override
        public void save(Batch batch) {
            batches.add(batch);
        }

        /**
         * 按批次 ID 查找。
         *
         * @param batchId 批次 ID
         * @return 匹配批次
         * @author lvdaxianerplus
         * @date 2026-06-16
         */
        @Override
        public Optional<Batch> findById(String batchId) {
            return batches.stream().filter(batch -> batch.batchId().equals(batchId)).findFirst();
        }

        /**
         * 按透传幂等键查找第一条批次。
         *
         * @param idempotencyKey 幂等键
         * @return 匹配批次
         * @author lvdaxianerplus
         * @date 2026-06-16
         */
        @Override
        public Optional<Batch> findByIdempotencyKey(String idempotencyKey) {
            return batches.stream()
                    .filter(batch -> batch.idempotencyKey().filter(idempotencyKey::equals).isPresent())
                    .findFirst();
        }

        /**
         * 列出最近批次。
         *
         * @param limit 最大数量
         * @return 最近批次
         * @author lvdaxianerplus
         * @date 2026-06-16
         */
        @Override
        public List<Batch> listRecent(int limit) {
            return batches.stream().limit(limit).toList();
        }

        /**
         * 更新批次摘要。
         *
         * @param batchId 批次 ID
         * @param completedFiles 完成文件数
         * @param failedFiles 失败文件数
         * @param status 批次状态
         * @author lvdaxianerplus
         * @date 2026-06-16
         */
        @Override
        public void updateSummary(String batchId, int completedFiles, int failedFiles, BatchStatus status) {
            // 当前测试只验证创建阶段保存行为。
        }
    }
}
