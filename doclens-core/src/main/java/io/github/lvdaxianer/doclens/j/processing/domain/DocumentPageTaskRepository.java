package io.github.lvdaxianer.doclens.j.processing.domain;

import java.util.List;
import java.util.Optional;

/**
 * 文档页 OCR 任务仓储接口。
 *
 * @author lvdaxianerplus
 * @date 2026-06-10
 */
public interface DocumentPageTaskRepository {

    /**
     * 批量保存页任务。
     *
     * @param tasks 页任务集合
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    void saveAll(List<DocumentPageTask> tasks);

    /**
     * 查询等待调度的页任务。
     *
     * @param limit 最大返回数量
     * @return 等待调度页任务集合
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    List<DocumentPageTask> listQueued(int limit);

    /**
     * 原子抢占等待中的页任务。
     *
     * @param request 抢占请求
     * @return true 表示当前工作线程抢占成功
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    boolean tryMarkProcessing(DocumentPageTaskClaimRequest request);

    /**
     * 标记页任务已完成。
     *
     * @param request 完成请求
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    void markCompleted(DocumentPageTaskCompletionRequest request);

    /**
     * 标记页任务终态失败。
     *
     * @param request 失败请求
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    void markFailed(DocumentPageTaskFailureRequest request);

    /**
     * 按文档查询页任务。
     *
     * @param documentId 文档 ID
     * @return 页任务集合
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    List<DocumentPageTask> listByDocumentId(String documentId);

    /**
     * 按文档与页码查询页任务。
     *
     * @param documentId 文档 ID
     * @param pageNo 页码
     * @return 可选页任务
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    Optional<DocumentPageTask> findByDocumentIdAndPageNo(String documentId, int pageNo);
}
