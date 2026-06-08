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
        String text = blocks.stream().map(OcrBlock::text).collect(Collectors.joining("\n"));
        double confidence = blocks.stream().mapToDouble(OcrBlock::confidence).average().orElse(0D);
        List<Map<String, Object>> pageText = List.of(Map.of("pageNo", pageNo, "text", text));
        List<Map<String, Object>> layoutBlocks = blocks.stream()
                .map(block -> Map.<String, Object>of(
                        "pageNo", block.pageNo(),
                        "type", "text",
                        "text", block.text(),
                        "confidence", block.confidence(),
                        "box", block.box(),
                        "polygon", block.polygon(),
                        "source", block.source()
                ))
                .toList();
        return new ImageOcrResult(pageNo, rawOutput, pageText, layoutBlocks, confidence, warnings);
    }
}
