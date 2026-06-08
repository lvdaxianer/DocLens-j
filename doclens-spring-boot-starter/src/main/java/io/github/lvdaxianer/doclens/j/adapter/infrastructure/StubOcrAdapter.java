package io.github.lvdaxianer.doclens.j.adapter.infrastructure;

import io.github.lvdaxianer.doclens.j.api.AdapterCapability;
import io.github.lvdaxianer.doclens.j.adapter.domain.ImageOcrRequest;
import io.github.lvdaxianer.doclens.j.adapter.domain.ImageOcrResult;
import io.github.lvdaxianer.doclens.j.adapter.domain.OcrBlock;
import io.github.lvdaxianer.doclens.j.adapter.domain.OcrAdapter;
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
                DocLensConstants.STUB_ADAPTER_KEY,
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
    public ImageOcrResult recognize(ImageOcrRequest request) {
        OcrBlock block = new OcrBlock(request.pageNo(), "Stub OCR text for " + request.fileName(),
                DocLensConstants.STUB_CONFIDENCE, List.of(), List.of(), DocLensConstants.STUB_ADAPTER_KEY);
        return ImageOcrResult.fromBlocks(request.pageNo(),
                Map.of("adapter", DocLensConstants.STUB_ADAPTER_KEY, "pageNo", request.pageNo()), List.of(block),
                List.of());
    }
}
