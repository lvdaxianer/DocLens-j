package io.github.lvdaxianer.doclens.j.processing.infrastructure;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.lvdaxianer.doclens.j.processing.application.MarkdownChunk;
import io.github.lvdaxianer.doclens.j.processing.application.MarkdownChunkPlan;
import io.github.lvdaxianer.doclens.j.processing.application.MarkdownChunker;
import io.github.lvdaxianer.doclens.j.processing.application.MarkdownPostProcessingRequest;
import io.github.lvdaxianer.doclens.j.processing.application.MarkdownPostProcessingResult;
import io.github.lvdaxianer.doclens.j.processing.application.MarkdownPostProcessor;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 支持大文本分片的 Markdown 后处理器。
 *
 * @author lvdaxianerplus
 * @date 2026-06-13
 */
public final class ChunkedMarkdownPostProcessor implements MarkdownPostProcessor {

    private static final Logger LOGGER = LoggerFactory.getLogger(ChunkedMarkdownPostProcessor.class);
    private static final String CHUNK_FAILED_WARNING = "llm_markdown_chunk_failed";
    private static final String CHUNK_SEPARATOR = "\n\n";

    private final MarkdownPostProcessor delegate;
    private final MarkdownChunker chunker;
    private final int maxContextTokens;
    private final ObjectMapper objectMapper;

    /**
     * 创建分片 Markdown 后处理器。
     *
     * @param delegate 实际 LLM 后处理器
     * @param chunker Markdown 分片器
     * @param maxContextTokens 最大上下文 Token 数
     * @author lvdaxianerplus
     * @date 2026-06-13
     */
    public ChunkedMarkdownPostProcessor(
            MarkdownPostProcessor delegate,
            MarkdownChunker chunker,
            int maxContextTokens
    ) {
        this.delegate = delegate;
        this.chunker = chunker;
        this.maxContextTokens = maxContextTokens;
        this.objectMapper = new ObjectMapper();
    }

    /**
     * 执行分片 Markdown 后处理。
     *
     * @param request Markdown 后处理请求
     * @return Markdown 后处理结果
     * @author lvdaxianerplus
     * @date 2026-06-13
     */
    @Override
    public MarkdownPostProcessingResult process(MarkdownPostProcessingRequest request) {
        MarkdownChunkPlan plan = chunker.plan(request.ocrText(), maxContextTokens);
        if (plan.chunked()) {
            // 大文本走分片处理，避免单次 LLM 请求超过上下文窗口。
            return processChunks(request, plan);
        } else {
            // 小文本保持原调用路径，避免引入额外提示词噪音。
            return delegate.process(request);
        }
    }

    /**
     * 顺序处理所有分片。
     *
     * @param request 原始 Markdown 后处理请求
     * @param plan 分片计划
     * @return Markdown 后处理结果
     * @author lvdaxianerplus
     * @date 2026-06-13
     */
    private MarkdownPostProcessingResult processChunks(MarkdownPostProcessingRequest request, MarkdownChunkPlan plan) {
        List<String> markdownParts = new ArrayList<>(plan.chunks().size());
        for (MarkdownChunk chunk : plan.chunks()) {
            MarkdownPostProcessingResult result = processChunk(request, chunk);
            if (result.markdownApplied()) {
                // 当前分片成功时按原始顺序暂存输出。
                markdownParts.add(result.markdown());
            } else {
                // 任一分片失败都回退完整 OCR 原文，避免输出半成品。
                return fallbackOriginal(request, chunk);
            }
        }
        return MarkdownPostProcessingResult.markdown(String.join(CHUNK_SEPARATOR, markdownParts));
    }

    /**
     * 处理单个分片。
     *
     * @param request 原始 Markdown 后处理请求
     * @param chunk Markdown 分片
     * @return 分片后处理结果
     * @author lvdaxianerplus
     * @date 2026-06-13
     */
    private MarkdownPostProcessingResult processChunk(MarkdownPostProcessingRequest request, MarkdownChunk chunk) {
        try {
            MarkdownPostProcessingRequest chunkRequest = chunkRequest(request, chunk);
            return delegate.process(chunkRequest);
        } catch (IOException ex) {
            LOGGER.warn("[LLM Markdown 分片] 分片提示词构建失败, documentId={}, chunkIndex={}, error={}",
                    request.documentId(), chunk.chunkIndex(), ex.getMessage(), ex);
            return MarkdownPostProcessingResult.passthrough(request.ocrText(), CHUNK_FAILED_WARNING);
        } catch (IllegalStateException ex) {
            LOGGER.warn("[LLM Markdown 分片] 分片处理失败, documentId={}, chunkIndex={}, error={}",
                    request.documentId(), chunk.chunkIndex(), ex.getMessage(), ex);
            return MarkdownPostProcessingResult.passthrough(request.ocrText(), CHUNK_FAILED_WARNING);
        }
    }

    /**
     * 创建分片请求。
     *
     * @param request 原始 Markdown 后处理请求
     * @param chunk Markdown 分片
     * @return 分片请求
     * @throws IOException 元数据序列化失败
     * @author lvdaxianerplus
     * @date 2026-06-13
     */
    private MarkdownPostProcessingRequest chunkRequest(MarkdownPostProcessingRequest request, MarkdownChunk chunk)
            throws IOException {
        String chunkPrompt = MarkdownPrompt.chunkUserPrompt(objectMapper, request, chunk);
        return new MarkdownPostProcessingRequest(request.documentId(), request.fileName(), request.metadata(),
                chunkPrompt);
    }

    /**
     * 创建完整原文回退结果。
     *
     * @param request 原始 Markdown 后处理请求
     * @param chunk 失败分片
     * @return 原文回退结果
     * @author lvdaxianerplus
     * @date 2026-06-13
     */
    private MarkdownPostProcessingResult fallbackOriginal(MarkdownPostProcessingRequest request, MarkdownChunk chunk) {
        LOGGER.warn("[LLM Markdown 分片] 分片未应用，回退原文, documentId={}, chunkIndex={}", request.documentId(),
                chunk.chunkIndex());
        return MarkdownPostProcessingResult.passthrough(request.ocrText(), CHUNK_FAILED_WARNING);
    }
}
