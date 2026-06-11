package io.github.lvdaxianer.doclens.j.processing.application;

import io.github.lvdaxianer.doclens.j.processing.domain.OcrResult;
import io.github.lvdaxianer.doclens.j.processing.domain.OcrResultRepository;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 文档删除测试使用的内存 OCR 结果仓储。
 *
 * @author lvdaxianerplus
 * @date 2026-06-11
 */
final class DeleteUseCaseOcrResultRepository implements OcrResultRepository {

    /** 测试集合初始容量。 */
    private static final int TEST_CAPACITY = 8;
    /** 按文档 ID 保存的 OCR 结果。 */
    private final Map<String, OcrResult> results = new HashMap<>(TEST_CAPACITY);

    /**
     * 保存 OCR 结果。
     *
     * @param result OCR 结果
     * @author lvdaxianerplus
     * @date 2026-06-11
     */
    @Override
    public void save(OcrResult result) {
        results.put(result.documentId(), result);
    }

    /**
     * 批量保存 OCR 结果。
     *
     * @param results OCR 结果列表
     * @author lvdaxianerplus
     * @date 2026-06-11
     */
    @Override
    public void saveAll(List<OcrResult> results) {
        results.forEach(this::save);
    }

    /**
     * 根据文档 ID 查询 OCR 结果。
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
     * 批量查询 OCR 结果。
     *
     * @param documentIds 文档 ID 列表
     * @return OCR 结果列表
     * @author lvdaxianerplus
     * @date 2026-06-11
     */
    @Override
    public List<OcrResult> findByDocumentIds(List<String> documentIds) {
        return documentIds.stream().map(results::get).filter(java.util.Objects::nonNull).toList();
    }

    /**
     * 根据文档 ID 删除 OCR 结果。
     *
     * @param documentId 文档 ID
     * @author lvdaxianerplus
     * @date 2026-06-11
     */
    @Override
    public void deleteByDocumentId(String documentId) {
        results.remove(documentId);
    }

    /**
     * 批量删除 OCR 结果。
     *
     * @param documentIds 文档 ID 列表
     * @author lvdaxianerplus
     * @date 2026-06-11
     */
    @Override
    public void deleteByDocumentIds(List<String> documentIds) {
        documentIds.forEach(results::remove);
    }
}
