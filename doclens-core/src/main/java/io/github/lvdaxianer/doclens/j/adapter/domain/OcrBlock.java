package io.github.lvdaxianer.doclens.j.adapter.domain;

import java.util.List;

/**
 * 归一化 OCR 文本块。
 *
 * @param pageNo 页码
 * @param text 文本内容
 * @param confidence 置信度
 * @param box 矩形框坐标
 * @param polygon 多边形坐标
 * @param source 来源适配器
 * @author lvdaxianerplus
 * @date 2026-06-08
 */
public record OcrBlock(
        int pageNo,
        String text,
        double confidence,
        List<Integer> box,
        List<List<Integer>> polygon,
        String source
) {
}
