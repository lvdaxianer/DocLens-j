package io.github.lvdaxianer.doclens.j.processing.domain;

import io.github.lvdaxianer.doclens.j.shared.domain.DocLensConstants;
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

    /**
     * 按批次 ID 集合批量列出文档。
     *
     * @param batchIds 批次 ID 集合
     * @return 文档任务集合
     * @author lvdaxianerplus
     * @date 2026-06-08
     */
    List<DocumentJob> listByBatchIds(List<String> batchIds);

    /**
     * 按更新时间倒序列出最近文档。
     *
     * @param limit 最大返回数量
     * @return 最近文档集合
     * @author lvdaxianerplus
     * @date 2026-06-08
     */
    List<DocumentJob> listRecent(int limit);

    /**
     * 按状态列出文档任务。
     *
     * @param status 目标状态
     * @param limit 最大返回数量
     * @return 目标状态文档集合
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    default List<DocumentJob> listByStatus(DocumentStatus status, int limit) {
        return listRecent(Math.max(limit, DocLensConstants.DEFAULT_QUERY_LIMIT)).stream()
                .filter(document -> document.status() == status)
                .toList();
    }

    /**
     * 根据 ID 删除文档任务。
     *
     * @param documentId 文档 ID
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    default void deleteById(String documentId) {
        throw new UnsupportedOperationException("document delete is not supported");
    }
}
