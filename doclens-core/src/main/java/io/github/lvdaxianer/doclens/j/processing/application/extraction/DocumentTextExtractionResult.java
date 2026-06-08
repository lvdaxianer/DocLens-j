package io.github.lvdaxianer.doclens.j.processing.application.extraction;

import io.github.lvdaxianer.doclens.j.adapter.domain.ImageOcrResult;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 文档纯文本提取结果。
 *
 * @param finalText 最终纯文本
 * @param rawOutput 原始输出
 * @param structuredDocument 结构化文档
 * @param pageText 页面文本
 * @param layoutBlocks 版面块
 * @param tables 表格
 * @param images 图片
 * @param confidence 平均置信度
 * @param warnings 警告信息
 * @author lvdaxianerplus
 * @date 2026-06-08
 */
public record DocumentTextExtractionResult(
        String finalText,
        Map<String, Object> rawOutput,
        Map<String, Object> structuredDocument,
        List<Map<String, Object>> pageText,
        List<Map<String, Object>> layoutBlocks,
        List<Map<String, Object>> tables,
        List<Map<String, Object>> images,
        double confidence,
        List<String> warnings
) {

    /**
     * 创建直通文本结果。
     *
     * @param documentId 文档 ID
     * @param fileName 文件名
     * @param text 文本内容
     * @return 提取结果
     * @author lvdaxianerplus
     * @date 2026-06-08
     */
    public static DocumentTextExtractionResult plainText(String documentId, String fileName, String text) {
        List<Map<String, Object>> pageText = List.of(Map.of("pageNo", 1, "text", text));
        return new DocumentTextExtractionResult(text, Map.of("source", "plain_text"),
                Map.of("documentId", documentId, "fileName", fileName, "pages", pageText),
                pageText, List.of(), List.of(), List.of(), 1D, List.of());
    }

    /**
     * 按页码合并多页图片 OCR 结果。
     *
     * @param documentId 文档 ID
     * @param fileName 文件名
     * @param pageResults 页面 OCR 结果
     * @param warnings 附加警告
     * @return 文档提取结果
     * @author lvdaxianerplus
     * @date 2026-06-08
     */
    public static DocumentTextExtractionResult fromPageResults(
            String documentId,
            String fileName,
            List<ImageOcrResult> pageResults,
            List<String> warnings
    ) {
        List<ImageOcrResult> ordered = pageResults.stream()
                .sorted(Comparator.comparingInt(ImageOcrResult::pageNo))
                .toList();
        List<Map<String, Object>> pageText = ordered.stream().flatMap(result -> result.pageText().stream()).toList();
        List<Map<String, Object>> layoutBlocks = ordered.stream()
                .flatMap(result -> result.layoutBlocks().stream())
                .toList();
        String finalText = pageText.stream()
                .map(page -> String.valueOf(page.getOrDefault("text", "")))
                .filter(text -> !text.isBlank())
                .collect(Collectors.joining("\n"));
        double confidence = ordered.stream().mapToDouble(ImageOcrResult::confidence).average().orElse(0D);
        List<Map<String, Object>> rawPages = ordered.stream()
                .map(result -> Map.of("pageNo", result.pageNo(), "rawOutput", result.rawOutput()))
                .toList();
        List<String> mergedWarnings = ordered.stream()
                .flatMap(result -> result.warnings().stream())
                .collect(Collectors.toList());
        mergedWarnings.addAll(warnings);
        return new DocumentTextExtractionResult(finalText, Map.of("pages", rawPages),
                Map.of("documentId", documentId, "fileName", fileName, "pages", pageText),
                pageText, layoutBlocks, List.of(), List.of(), confidence, mergedWarnings);
    }
}
