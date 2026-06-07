package io.github.lvdaxianer.doclens.j.adapter.infrastructure;

import io.github.lvdaxianer.doclens.j.api.AdapterCapability;
import io.github.lvdaxianer.doclens.j.adapter.domain.OcrAdapter;
import io.github.lvdaxianer.doclens.j.processing.domain.DocumentJob;
import io.github.lvdaxianer.doclens.j.shared.domain.DocLensConstants;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * 阶段一可执行垂直切片使用的 Stub OCR 适配器。
 *
 * @author lvdaxianerplus
 * @date 2026-06-07
 */
@Component
public class StubOcrAdapter implements OcrAdapter {

    @Override
    public AdapterCapability capability() {
        return new AdapterCapability(
                DocLensConstants.DEFAULT_ADAPTER_KEY,
                List.of("image", "pdf", "word"),
                false,
                false,
                true,
                true,
                true,
                null,
                null,
                50,
                200,
                "Phase-1 stub adapter compatible with the Python adapter contract"
        );
    }

    @Override
    public Map<String, Object> parse(DocumentJob document) {
        return Map.of(
                "adapter", DocLensConstants.DEFAULT_ADAPTER_KEY,
                "documentId", document.documentId(),
                "fileName", document.fileName(),
                "pages", List.of(Map.of(
                        "pageNo", DocLensConstants.DEFAULT_PAGE_NO,
                        "text", "Stub OCR text for " + document.fileName(),
                        "confidence", DocLensConstants.STUB_CONFIDENCE
                ))
        );
    }
}
