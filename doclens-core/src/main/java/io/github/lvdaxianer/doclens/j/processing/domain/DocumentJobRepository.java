package io.github.lvdaxianer.doclens.j.processing.domain;

import java.util.List;
import java.util.Optional;

/**
 * 文档任务仓储接口。
 *
 * @author lvdaxianerplus
 * @date 2026-06-07
 */
public interface DocumentJobRepository {

    /**
     * 保存新的文档任务。
     *
     * @param document 文档任务
     * @author lvdaxianerplus
     * @date 2026-06-07
     */
    void save(DocumentJob document);

    /**
     * 批量保存文档任务。
     *
     * @param documents 文档任务集合
     * @author lvdaxianerplus
     * @date 2026-06-07
     */
    void saveAll(List<DocumentJob> documents);

    /**
     * 更新已有文档任务。
     *
     * @param document 文档任务
     * @author lvdaxianerplus
     * @date 2026-06-07
     */
    void update(DocumentJob document);

    /**
     * 批量更新文档任务。
     *
     * @param documents 文档任务集合
     * @author lvdaxianerplus
     * @date 2026-06-07
     */
    void updateAll(List<DocumentJob> documents);

    /**
     * 根据 ID 查找文档。
     *
     * @param documentId 文档 ID
     * @return 可选文档
     * @author lvdaxianerplus
     * @date 2026-06-07
     */
    Optional<DocumentJob> findById(String documentId);

    /**
     * 按上传顺序列出批次内文档。
     *
     * @param batchId 批次 ID
     * @return 有序文档集合
     * @author lvdaxianerplus
     * @date 2026-06-07
     */
    List<DocumentJob> listByBatchId(String batchId);
}
