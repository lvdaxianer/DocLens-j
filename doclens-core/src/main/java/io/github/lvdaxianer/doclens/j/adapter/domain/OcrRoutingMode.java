package io.github.lvdaxianer.doclens.j.adapter.domain;

/**
 * OCR 请求路由模式。
 *
 * @author lvdaxianerplus
 * @date 2026-06-08
 */
public enum OcrRoutingMode {
    DEFAULT,
    GLOBAL_LOAD_BALANCE,
    MODEL_LOAD_BALANCE,
    SPECIFIC_NODE
}
