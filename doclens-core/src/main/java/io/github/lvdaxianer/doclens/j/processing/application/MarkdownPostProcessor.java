package io.github.lvdaxianer.doclens.j.processing.application;

/**
 * OCR 合并文本的 Markdown 后处理器。
 *
 * @author lvdaxianerplus
 * @date 2026-06-09
 */
public interface MarkdownPostProcessor {

    /**
     * 将 OCR 合并文本整理为 Markdown。
     *
     * @param request Markdown 后处理请求
     * @return Markdown 后处理结果
     * @author lvdaxianerplus
     * @date 2026-06-09
     */
    MarkdownPostProcessingResult process(MarkdownPostProcessingRequest request);

    /**
     * 创建不启用 LLM 的直通后处理器。
     *
     * @return 直通 Markdown 后处理器
     * @author lvdaxianerplus
     * @date 2026-06-09
     */
    static MarkdownPostProcessor noop() {
        return request -> MarkdownPostProcessingResult.markdown(request.ocrText());
    }
}
