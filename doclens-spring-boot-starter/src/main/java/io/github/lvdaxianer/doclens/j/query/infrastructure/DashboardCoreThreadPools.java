package io.github.lvdaxianer.doclens.j.query.infrastructure;

import java.util.concurrent.ExecutorService;

/**
 * Dashboard 核心线程池集合。
 *
 * @param documentProcessing 文档处理线程池
 * @param ocrRequest OCR 请求线程池
 * @param ocrHealth OCR 健康检查线程池
 * @param callback 回调线程池
 * @param llmMarkdownChunk LLM Markdown 分块线程池
 * @author lvdaxianerplus
 * @date 2026-06-21
 */
public record DashboardCoreThreadPools(
        ExecutorService documentProcessing,
        ExecutorService ocrRequest,
        ExecutorService ocrHealth,
        ExecutorService callback,
        ExecutorService llmMarkdownChunk
) {
}
