package io.github.lvdaxianer.doclens.j.adapter.interfaces;

/**
 * OCR 节点连通性测试响应。
 *
 * @param healthy 节点是否健康
 * @param message 测试消息
 * @author lvdaxianerplus
 * @date 2026-06-09
 */
public record OcrNodeTestResponse(boolean healthy, String message) {
}
