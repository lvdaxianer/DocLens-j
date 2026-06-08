package io.github.lvdaxianer.doclens.j.adapter.application;

/**
 * OCR 调用记录 ID 生成端口。
 *
 * @author lvdaxianerplus
 * @date 2026-06-08
 */
public interface OcrCallIdGenerator {

    /**
     * 生成 OCR 调用记录 ID。
     *
     * @return OCR 调用记录 ID
     * @author lvdaxianerplus
     * @date 2026-06-08
     */
    String newOcrCallId();
}
