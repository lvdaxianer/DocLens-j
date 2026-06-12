package io.github.lvdaxianer.doclens.j.adapter.infrastructure;

/**
 * OCR 节点协议客户端集合。
 *
 * @param paddleClient PaddleOCR 原生客户端
 * @param paddleResponseMapper PaddleOCR 响应映射器
 * @param onlineClient 在线 OCR 客户端
 * @param ollamaClient Ollama OCR 客户端
 * @author lvdaxianerplus
 * @date 2026-06-12
 */
public record OcrNodeProtocolClients(
        PaddleOcrNativeClient paddleClient,
        PaddleOcrNativeResponseMapper paddleResponseMapper,
        DashScopeOnlineOcrClient onlineClient,
        OllamaOcrClient ollamaClient
) {
}
