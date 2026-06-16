package io.github.lvdaxianer.doclens.j.processing.application;

import static io.github.lvdaxianer.doclens.j.processing.application.BatchProcessingUseCaseTestSupport.CONCURRENT_TEST_TIMEOUT_SECONDS;
import static io.github.lvdaxianer.doclens.j.processing.application.BatchProcessingUseCaseTestSupport.TEST_DOCUMENT_CAPACITY;

import io.github.lvdaxianer.doclens.j.processing.domain.CallbackJob;
import io.github.lvdaxianer.doclens.j.processing.domain.CallbackJobFailureRequest;
import io.github.lvdaxianer.doclens.j.processing.domain.CallbackJobRepository;
import io.github.lvdaxianer.doclens.j.processing.domain.DocumentJob;
import io.github.lvdaxianer.doclens.j.processing.domain.DocumentJobRepository;
import io.github.lvdaxianer.doclens.j.processing.domain.OcrResult;
import io.github.lvdaxianer.doclens.j.processing.domain.OcrResultRepository;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * 批次处理测试内存仓储集合。
 *
 * @author lvdaxianerplus
 * @date 2026-06-11
 */
final class BatchProcessingUseCaseRepositories {

    /**
     * 禁止实例化测试仓储集合。
     *
     * @author lvdaxianerplus
     * @date 2026-06-11
     */
    private BatchProcessingUseCaseRepositories() {
    }
}

/**
 * 内存文档仓储。
 *
 * @author lvdaxianerplus
 * @date 2026-06-08
 */
class InMemoryDocumentJobRepository implements DocumentJobRepository {

    private final Map<String, DocumentJob> documents = new ConcurrentHashMap<>(TEST_DOCUMENT_CAPACITY);

    /**
     * 保存文档任务。
     *
     * @param document 文档任务
     * @author lvdaxianerplus
     * @date 2026-06-11
     */
    @Override
    public void save(DocumentJob document) {
        documents.put(document.documentId(), document);
    }

    /**
     * 批量保存文档任务。
     *
     * @param documents 文档任务集合
     * @author lvdaxianerplus
     * @date 2026-06-11
     */
    @Override
    public void saveAll(List<DocumentJob> documents) {
        documents.forEach(this::save);
    }

    /**
     * 更新文档任务。
     *
     * @param document 文档任务
     * @author lvdaxianerplus
     * @date 2026-06-11
     */
    @Override
    public void update(DocumentJob document) {
        documents.put(document.documentId(), document);
    }

    /**
     * 批量更新文档任务。
     *
     * @param documents 文档任务集合
     * @author lvdaxianerplus
     * @date 2026-06-11
     */
    @Override
    public void updateAll(List<DocumentJob> documents) {
        documents.forEach(this::update);
    }

    /**
     * 按 ID 查询文档任务。
     *
     * @param documentId 文档 ID
     * @return 文档任务
     * @author lvdaxianerplus
     * @date 2026-06-11
     */
    @Override
    public Optional<DocumentJob> findById(String documentId) {
        return Optional.ofNullable(documents.get(documentId));
    }

    /**
     * 按批次 ID 查询文档任务。
     *
     * @param batchId 批次 ID
     * @return 文档任务列表
     * @author lvdaxianerplus
     * @date 2026-06-11
     */
    @Override
    public List<DocumentJob> listByBatchId(String batchId) {
        return documents.values().stream()
                .filter(document -> batchId.equals(document.batchId()))
                .sorted((left, right) -> Integer.compare(left.sortOrder(), right.sortOrder()))
                .toList();
    }

    /**
     * 按批次 ID 集合查询文档任务。
     *
     * @param batchIds 批次 ID 集合
     * @return 文档任务列表
     * @author lvdaxianerplus
     * @date 2026-06-11
     */
    @Override
    public List<DocumentJob> listByBatchIds(List<String> batchIds) {
        return documents.values().stream().filter(document -> batchIds.contains(document.batchId())).toList();
    }

    /**
     * 查询最近文档任务。
     *
     * @param limit 最大数量
     * @return 文档任务列表
     * @author lvdaxianerplus
     * @date 2026-06-11
     */
    @Override
    public List<DocumentJob> listRecent(int limit) {
        return documents.values().stream().limit(limit).toList();
    }
}

/**
 * 内存 OCR 结果仓储。
 *
 * @author lvdaxianerplus
 * @date 2026-06-08
 */
class InMemoryOcrResultRepository implements OcrResultRepository {

    private final Map<String, OcrResult> results = new ConcurrentHashMap<>(TEST_DOCUMENT_CAPACITY);
    private final Map<String, CountDownLatch> resultSignals = new ConcurrentHashMap<>(TEST_DOCUMENT_CAPACITY);

    /**
     * 保存 OCR 结果并发送结果可见信号。
     *
     * @param result OCR 结果
     * @author lvdaxianerplus
     * @date 2026-06-11
     */
    @Override
    public void save(OcrResult result) {
        results.put(result.documentId(), result);
        signalResult(result.documentId());
    }

    /**
     * 批量保存 OCR 结果。
     *
     * @param results OCR 结果集合
     * @author lvdaxianerplus
     * @date 2026-06-11
     */
    @Override
    public void saveAll(List<OcrResult> results) {
        results.forEach(this::save);
    }

    /**
     * 按文档 ID 查询 OCR 结果。
     *
     * @param documentId 文档 ID
     * @return OCR 结果
     * @author lvdaxianerplus
     * @date 2026-06-11
     */
    @Override
    public Optional<OcrResult> findByDocumentId(String documentId) {
        return Optional.ofNullable(results.get(documentId));
    }

    /**
     * 等待指定文档结果落库。
     *
     * @param documentId 文档 ID
     * @return 结果是否已落库
     * @throws InterruptedException 等待被中断时抛出
     * @author lvdaxianerplus
     * @date 2026-06-11
     */
    boolean awaitResult(String documentId) throws InterruptedException {
        if (results.containsKey(documentId)) {
            // 结果已存在时直接返回，避免错过先于等待注册发生的保存信号。
            return true;
        } else {
            // 结果尚未保存时注册等待信号，观察完成顺序持久化行为。
            return resultSignals.computeIfAbsent(documentId, ignored -> new CountDownLatch(1))
                    .await(CONCURRENT_TEST_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        }
    }

    /**
     * 通知指定文档结果已落库。
     *
     * @param documentId 文档 ID
     * @author lvdaxianerplus
     * @date 2026-06-11
     */
    private void signalResult(String documentId) {
        resultSignals.computeIfAbsent(documentId, ignored -> new CountDownLatch(1)).countDown();
    }
}

/**
 * 内存回调任务仓储。
 *
 * @author lvdaxianerplus
 * @date 2026-06-16
 */
class InMemoryCallbackJobRepository implements CallbackJobRepository {

    private final Map<String, CallbackJob> jobs = new ConcurrentHashMap<>(TEST_DOCUMENT_CAPACITY);

    /**
     * 保存回调任务。
     *
     * @param job 回调任务
     * @author lvdaxianerplus
     * @date 2026-06-16
     */
    @Override
    public void save(CallbackJob job) {
        jobs.put(job.callbackJobId(), job);
    }

    /**
     * 根据 ID 查询回调任务。
     *
     * @param callbackJobId 回调任务 ID
     * @return 回调任务
     * @author lvdaxianerplus
     * @date 2026-06-16
     */
    @Override
    public Optional<CallbackJob> findById(String callbackJobId) {
        return Optional.ofNullable(jobs.get(callbackJobId));
    }

    /**
     * 查询待投递回调任务。
     *
     * @param limit 最大返回数量
     * @return 待投递回调任务
     * @author lvdaxianerplus
     * @date 2026-06-16
     */
    @Override
    public List<CallbackJob> listPending(int limit) {
        return jobs.values().stream().limit(limit).toList();
    }

    /**
     * 根据批次查询回调任务。
     *
     * @param batchId 批次 ID
     * @return 回调任务
     * @author lvdaxianerplus
     * @date 2026-06-16
     */
    @Override
    public List<CallbackJob> listByBatchId(String batchId) {
        return jobs.values().stream()
                .filter(job -> batchId.equals(job.batchId()))
                .toList();
    }

    /**
     * 标记回调成功。
     *
     * @param callbackJobId 回调任务 ID
     * @author lvdaxianerplus
     * @date 2026-06-16
     */
    @Override
    public void markSucceeded(String callbackJobId) {
        throw new UnsupportedOperationException("not required by batch processing tests");
    }

    /**
     * 标记回调失败。
     *
     * @param request 失败更新请求
     * @author lvdaxianerplus
     * @date 2026-06-16
     */
    @Override
    public void markFailed(CallbackJobFailureRequest request) {
        throw new UnsupportedOperationException("not required by batch processing tests");
    }
}
