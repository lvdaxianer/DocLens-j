package io.github.lvdaxianer.doclens.j.processing.application.extraction;

/**
 * 文档转最终纯文本的应用端口。
 *
 * @author lvdaxianerplus
 * @date 2026-06-08
 */
public interface DocumentTextExtractor {

    /**
     * 将文档内容提取为最终纯文本。
     *
     * @param request 提取请求
     * @return 提取结果
     * @author lvdaxianerplus
     * @date 2026-06-08
     */
    DocumentTextExtractionResult extract(DocumentTextExtractionRequest request);
}
