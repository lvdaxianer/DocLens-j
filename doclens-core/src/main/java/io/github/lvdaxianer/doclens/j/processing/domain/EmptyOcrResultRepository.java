package io.github.lvdaxianer.doclens.j.processing.domain;

import java.util.List;
import java.util.Optional;

/**
 * 空 OCR 结果仓储。
 *
 * @author lvdaxianerplus
 * @date 2026-06-19
 */
public final class EmptyOcrResultRepository implements OcrResultRepository {

    /**
     * 保存 OCR 结果。
     *
     * @param result OCR 结果
     * @author lvdaxianerplus
     * @date 2026-06-19
     */
    @Override
    public void save(OcrResult result) {
        // 空仓储不持久化 OCR 结果。
    }

    /**
     * 批量保存 OCR 结果。
     *
     * @param results OCR 结果集合
     * @author lvdaxianerplus
     * @date 2026-06-19
     */
    @Override
    public void saveAll(List<OcrResult> results) {
        // 空仓储不持久化 OCR 结果。
    }

    /**
     * 根据文档 ID 查找 OCR 结果。
     *
     * @param documentId 文档 ID
     * @return 空结果
     * @author lvdaxianerplus
     * @date 2026-06-19
     */
    @Override
    public Optional<OcrResult> findByDocumentId(String documentId) {
        return Optional.empty();
    }

    /**
     * 根据文档 ID 集合批量查找 OCR 结果。
     *
     * @param documentIds 文档 ID 集合
     * @return 空集合
     * @author lvdaxianerplus
     * @date 2026-06-19
     */
    @Override
    public List<OcrResult> findByDocumentIds(List<String> documentIds) {
        return List.of();
    }
}
