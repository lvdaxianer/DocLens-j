package io.github.lvdaxianer.doclens.j.processing.domain;

import java.util.List;
import java.util.Optional;

/**
 * 文档单页 OCR 结果仓储接口。
 *
 * @author lvdaxianerplus
 * @date 2026-06-10
 */
public interface DocumentPageResultRepository {

    /**
     * 新增或更新页 OCR 结果。
     *
     * @param result 页 OCR 结果
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    void upsert(DocumentPageResult result);

    /**
     * 按文档与页码查询页 OCR 结果。
     *
     * @param documentId 文档 ID
     * @param pageNo 页码
     * @return 可选页 OCR 结果
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    Optional<DocumentPageResult> findByDocumentIdAndPageNo(String documentId, int pageNo);

    /**
     * 按文档查询全部页 OCR 结果。
     *
     * @param documentId 文档 ID
     * @return 页 OCR 结果集合
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    List<DocumentPageResult> listByDocumentId(String documentId);
}
