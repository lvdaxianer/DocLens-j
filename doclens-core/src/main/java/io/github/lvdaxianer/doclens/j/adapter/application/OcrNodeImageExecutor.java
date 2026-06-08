package io.github.lvdaxianer.doclens.j.adapter.application;

import io.github.lvdaxianer.doclens.j.adapter.domain.ImageOcrRequest;
import io.github.lvdaxianer.doclens.j.adapter.domain.ImageOcrResult;

/**
 * 指定 OCR 节点图片识别端口。
 *
 * @author lvdaxianerplus
 * @date 2026-06-08
 */
public interface OcrNodeImageExecutor {

    /**
     * 在指定 OCR 运行时节点执行图片识别。
     *
     * @param node OCR 运行时节点视图
     * @param request 图片 OCR 请求
     * @return 图片 OCR 结果
     * @author lvdaxianerplus
     * @date 2026-06-08
     */
    ImageOcrResult recognize(OcrRuntimeNodeView node, ImageOcrRequest request);
}
