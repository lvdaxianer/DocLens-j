package io.github.lvdaxianer.doclens.j.adapter.domain;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 单张图片 OCR 归一化结果。
 *
 * @param pageNo 页码
 * @param rawOutput 原始厂商输出
 * @param pageText 页面文本
 * @param layoutBlocks 版面块
 * @param confidence 平均置信度
 * @param warnings 警告信息
 * @author lvdaxianerplus
 * @date 2026-06-08
 */
public record ImageOcrResult(
        int pageNo,
        Map<String, Object> rawOutput,
        List<Map<String, Object>> pageText,
        List<Map<String, Object>> layoutBlocks,
        double confidence,
        List<String> warnings
) {

    private static final String PLAIN_TEXT_SEPARATOR = "\n";
    private static final String MARKDOWN_PARAGRAPH_SEPARATOR = "\n\n";
    private static final String PAGE_NO_KEY = "pageNo";
    private static final String TYPE_KEY = "type";
    private static final String TEXT_KEY = "text";
    private static final String TEXT_TYPE = "text";
    private static final String CONFIDENCE_KEY = "confidence";
    private static final String BOX_KEY = "box";
    private static final String POLYGON_KEY = "polygon";
    private static final String SOURCE_KEY = "source";

    /**
     * Markdown 文本块结果创建参数。
     *
     * @param pageNo 页码
     * @param rawOutput 原始输出
     * @param blocks 文本块
     * @param warnings 警告
     * @author lvdaxianerplus
     * @date 2026-06-12
     */
    public record MarkdownBlocks(
            int pageNo,
            Map<String, Object> rawOutput,
            List<OcrBlock> blocks,
            List<String> warnings
    ) {
    }

    /**
     * 从 OCR 文本块创建图片 OCR 结果。
     *
     * @param pageNo 页码
     * @param rawOutput 原始输出
     * @param blocks 文本块
     * @param warnings 警告
     * @return 图片 OCR 结果
     * @author lvdaxianerplus
     * @date 2026-06-08
     */
    public static ImageOcrResult fromBlocks(
            int pageNo,
            Map<String, Object> rawOutput,
            List<OcrBlock> blocks,
            List<String> warnings
    ) {
        String text = blocks.stream().map(OcrBlock::text).collect(Collectors.joining(PLAIN_TEXT_SEPARATOR));
        double confidence = blocks.stream().mapToDouble(OcrBlock::confidence).average().orElse(0D);
        List<Map<String, Object>> pageText = List.of(Map.of(PAGE_NO_KEY, pageNo, TEXT_KEY, text));
        return new ImageOcrResult(pageNo, rawOutput, pageText, layoutBlocks(blocks), confidence, warnings);
    }

    /**
     * 从 OCR 文本块创建 Markdown 图片 OCR 结果。
     *
     * @param markdownBlocks Markdown 文本块结果创建参数
     * @return 图片 OCR 结果
     * @author lvdaxianerplus
     * @date 2026-06-12
     */
    public static ImageOcrResult fromMarkdownBlocks(MarkdownBlocks markdownBlocks) {
        String text = markdownText(markdownBlocks.blocks());
        double confidence = markdownBlocks.blocks().stream().mapToDouble(OcrBlock::confidence).average().orElse(0D);
        List<Map<String, Object>> pageText = List.of(Map.of(PAGE_NO_KEY, markdownBlocks.pageNo(), TEXT_KEY, text));
        List<Map<String, Object>> layoutBlocks = layoutBlocks(markdownBlocks.blocks());
        return new ImageOcrResult(markdownBlocks.pageNo(), markdownBlocks.rawOutput(),
                pageText, layoutBlocks, confidence, markdownBlocks.warnings());
    }

    /**
     * 将 OCR 文本块稳定转换为 Markdown 段落。
     *
     * @param blocks OCR 文本块
     * @return Markdown 文本
     * @author lvdaxianerplus
     * @date 2026-06-12
     */
    private static String markdownText(List<OcrBlock> blocks) {
        return blocks.stream()
                .map(OcrBlock::text)
                .filter(text -> !text.isBlank())
                .collect(Collectors.joining(MARKDOWN_PARAGRAPH_SEPARATOR));
    }

    /**
     * 将 OCR 文本块转换为版面块输出。
     *
     * @param blocks OCR 文本块
     * @return 版面块输出
     * @author lvdaxianerplus
     * @date 2026-06-12
     */
    private static List<Map<String, Object>> layoutBlocks(List<OcrBlock> blocks) {
        return blocks.stream()
                .map(block -> Map.<String, Object>of(
                        PAGE_NO_KEY, block.pageNo(),
                        TYPE_KEY, TEXT_TYPE,
                        TEXT_KEY, block.text(),
                        CONFIDENCE_KEY, block.confidence(),
                        BOX_KEY, block.box(),
                        POLYGON_KEY, block.polygon(),
                        SOURCE_KEY, block.source()
                ))
                .toList();
    }
}
