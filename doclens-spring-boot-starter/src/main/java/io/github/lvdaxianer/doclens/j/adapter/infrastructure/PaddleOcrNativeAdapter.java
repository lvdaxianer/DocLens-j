package io.github.lvdaxianer.doclens.j.adapter.infrastructure;

import io.github.lvdaxianer.doclens.j.adapter.domain.ImageOcrRequest;
import io.github.lvdaxianer.doclens.j.adapter.domain.ImageOcrResult;
import io.github.lvdaxianer.doclens.j.adapter.domain.OcrAdapter;
import io.github.lvdaxianer.doclens.j.api.AdapterCapability;
import io.github.lvdaxianer.doclens.j.shared.domain.DocLensConstants;
import java.util.List;

/**
 * PaddleOCR 原生 HTTP API 图片 OCR 适配器。
 *
 * @author lvdaxianerplus
 * @date 2026-06-08
 */
public class PaddleOcrNativeAdapter implements OcrAdapter {

    private final PaddleOcrNativeClient client;
    private final PaddleOcrNativeResponseMapper responseMapper;

    /**
     * 创建 PaddleOCR 原生适配器。
     *
     * @param client HTTP 客户端
     * @param responseMapper 响应映射器
     * @author lvdaxianerplus
     * @date 2026-06-08
     */
    public PaddleOcrNativeAdapter(PaddleOcrNativeClient client, PaddleOcrNativeResponseMapper responseMapper) {
        this.client = client;
        this.responseMapper = responseMapper;
    }

    @Override
    public AdapterCapability capability() {
        return new AdapterCapability(DocLensConstants.DEFAULT_ADAPTER_KEY, List.of("image", "pdf", "word"), true,
                false, true, false, true, null, null, 50, 200, "PaddleOCR native HTTP API at /ocr");
    }

    @Override
    public ImageOcrResult recognize(ImageOcrRequest request) {
        return responseMapper.map(request.pageNo(), client.recognizeImage(request.imageContent()));
    }
}
