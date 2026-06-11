package io.github.lvdaxianer.doclens.j.processing.application;

import io.github.lvdaxianer.doclens.j.processing.domain.DocumentJob;
import io.github.lvdaxianer.doclens.j.processing.domain.DocumentJobRepository;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 文档删除测试使用的内存文档仓储。
 *
 * @author lvdaxianerplus
 * @date 2026-06-11
 */
final class DeleteUseCaseDocumentJobRepository implements DocumentJobRepository {

    /** 测试集合初始容量。 */
    private static final int TEST_CAPACITY = 8;
    /** 按文档 ID 保存的文档任务。 */
    private final Map<String, DocumentJob> documents = new HashMap<>(TEST_CAPACITY);

    /**
     * 保存单个文档任务。
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
     * @param documents 文档任务列表
     * @author lvdaxianerplus
     * @date 2026-06-11
     */
    @Override
    public void saveAll(List<DocumentJob> documents) {
        documents.forEach(this::save);
    }

    /**
     * 更新单个文档任务。
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
     * @param documents 文档任务列表
     * @author lvdaxianerplus
     * @date 2026-06-11
     */
    @Override
    public void updateAll(List<DocumentJob> documents) {
        documents.forEach(this::update);
    }

    /**
     * 根据文档 ID 查询文档任务。
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
     * 查询批次下的文档任务。
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
     * 查询多个批次下的文档任务。
     *
     * @param batchIds 批次 ID 列表
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
     * @param limit 查询上限
     * @return 文档任务列表
     * @author lvdaxianerplus
     * @date 2026-06-11
     */
    @Override
    public List<DocumentJob> listRecent(int limit) {
        return documents.values().stream().limit(limit).toList();
    }

    /**
     * 根据文档 ID 删除文档任务。
     *
     * @param documentId 文档 ID
     * @author lvdaxianerplus
     * @date 2026-06-11
     */
    @Override
    public void deleteById(String documentId) {
        documents.remove(documentId);
    }

    /**
     * 批量删除文档任务。
     *
     * @param documentIds 文档 ID 列表
     * @author lvdaxianerplus
     * @date 2026-06-11
     */
    @Override
    public void deleteByIds(List<String> documentIds) {
        documentIds.forEach(documents::remove);
    }
}
