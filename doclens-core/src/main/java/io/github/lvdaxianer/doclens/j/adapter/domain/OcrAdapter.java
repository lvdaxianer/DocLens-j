package io.github.lvdaxianer.doclens.j.adapter.domain;

import io.github.lvdaxianer.doclens.j.api.AdapterCapability;

/**
 * OCR 适配器防腐接口。
 *
 * @author lvdaxianerplus
 * @date 2026-06-07
 */
public interface OcrAdapter {

    /**
     * 返回适配器能力。
     *
     * @return 适配器能力
     * @author lvdaxianerplus
     * @date 2026-06-07
     */
    AdapterCapability capability();

    /**
     * 识别单张图片或 PDF 页图片。
     *
     * @param request 图片 OCR 请求
     * @return 图片 OCR 结果
     * @author lvdaxianerplus
     * @date 2026-06-07
     */
    ImageOcrResult recognize(ImageOcrRequest request);
}
