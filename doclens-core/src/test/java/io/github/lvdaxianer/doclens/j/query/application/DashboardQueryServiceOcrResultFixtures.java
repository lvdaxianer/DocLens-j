package io.github.lvdaxianer.doclens.j.query.application;

import io.github.lvdaxianer.doclens.j.processing.domain.OcrResult;
import io.github.lvdaxianer.doclens.j.processing.domain.OcrResultRepository;
import java.util.List;
import java.util.Optional;

/**
 * Dashboard 查询服务 OCR 结果测试夹具。
 *
 * @author lvdaxianerplus
 * @date 2026-06-19
 */
final class DashboardQueryServiceOcrResultFixtures {

    private DashboardQueryServiceOcrResultFixtures() {
    }

    /**
     * 创建固定返回 OCR 结果的仓储。
     *
     * @param results 固定返回的 OCR 结果
     * @return 固定 OCR 结果仓储
     * @author lvdaxianerplus
     * @date 2026-06-19
     */
    static OcrResultRepository fixedOcrResultRepository(List<OcrResult> results) {
        return new FixedOcrResultRepository(results);
    }

    /**
     * 固定返回 OCR 结果的仓储。
     *
     * @author lvdaxianerplus
     * @date 2026-06-19
     */
    private static final class FixedOcrResultRepository implements OcrResultRepository {

        private final List<OcrResult> results;

        private FixedOcrResultRepository(List<OcrResult> results) {
            this.results = results;
        }

        @Override
        public void save(OcrResult result) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void saveAll(List<OcrResult> results) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Optional<OcrResult> findByDocumentId(String documentId) {
            return results.stream().filter(result -> result.documentId().equals(documentId)).findFirst();
        }

        @Override
        public List<OcrResult> findByDocumentIds(List<String> documentIds) {
            return results.stream().filter(result -> documentIds.contains(result.documentId())).toList();
        }
    }
}
