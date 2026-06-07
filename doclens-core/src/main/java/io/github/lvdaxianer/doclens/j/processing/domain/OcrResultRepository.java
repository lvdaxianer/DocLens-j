package io.github.lvdaxianer.doclens.j.processing.domain;

import java.util.Optional;

/**
 * OCR 结果仓储接口。
 *
 * @author lvdaxianerplus
 * @date 2026-06-07
 */
public interface OcrResultRepository {

    /**
     * 保存 OCR 结果。
     *
     * @param result OCR 结果
     * @author lvdaxianerplus
     * @date 2026-06-07
     */
    void save(OcrResult result);

    /**
     * 批量保存 OCR 结果。
     *
     * @param results OCR 结果集合
     * @author lvdaxianerplus
     * @date 2026-06-07
     */
    void saveAll(java.util.List<OcrResult> results);

    /**
     * 根据文档 ID 查找 OCR 结果。
     *
     * @param documentId 文档 ID
     * @return 可选结果
     * @author lvdaxianerplus
     * @date 2026-06-07
     */
    Optional<OcrResult> findByDocumentId(String documentId);
}
