package io.github.lvdaxianer.doclens.j.processing.application;

import io.github.lvdaxianer.doclens.j.processing.domain.DocumentJob;
import io.github.lvdaxianer.doclens.j.processing.domain.DocumentJobRepository;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 测试用内存文档仓储。
 *
 * @author lvdaxianerplus
 * @date 2026-06-10
 */
class InMemoryDocumentJobRepository implements DocumentJobRepository {

    private static final int TEST_DOCUMENT_CAPACITY = 2;

    private final Map<String, DocumentJob> documents = new HashMap<>(TEST_DOCUMENT_CAPACITY);

    /**
     * 保存文档。
     *
     * @param document 文档任务
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    @Override
    public void save(DocumentJob document) {
        documents.put(document.documentId(), document);
    }

    /**
     * 批量保存文档。
     *
     * @param documents 文档任务集合
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    @Override
    public void saveAll(List<DocumentJob> documents) {
        documents.forEach(this::save);
    }

    /**
     * 更新文档。
     *
     * @param document 文档任务
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    @Override
    public void update(DocumentJob document) {
        documents.put(document.documentId(), document);
    }

    /**
     * 批量更新文档。
     *
     * @param documents 文档任务集合
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    @Override
    public void updateAll(List<DocumentJob> documents) {
        documents.forEach(this::update);
    }

    /**
     * 按 ID 查询文档。
     *
     * @param documentId 文档 ID
     * @return 可选文档
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    @Override
    public Optional<DocumentJob> findById(String documentId) {
        return Optional.ofNullable(documents.get(documentId));
    }

    /**
     * 按批次查询文档。
     *
     * @param batchId 批次 ID
     * @return 文档集合
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    @Override
    public List<DocumentJob> listByBatchId(String batchId) {
        return documents.values().stream().filter(document -> batchId.equals(document.batchId())).toList();
    }

    /**
     * 按批次集合查询文档。
     *
     * @param batchIds 批次 ID 集合
     * @return 文档集合
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    @Override
    public List<DocumentJob> listByBatchIds(List<String> batchIds) {
        return documents.values().stream().filter(document -> batchIds.contains(document.batchId())).toList();
    }

    /**
     * 查询最近文档。
     *
     * @param limit 最大返回数量
     * @return 文档集合
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    @Override
    public List<DocumentJob> listRecent(int limit) {
        return documents.values().stream().limit(limit).toList();
    }
}
