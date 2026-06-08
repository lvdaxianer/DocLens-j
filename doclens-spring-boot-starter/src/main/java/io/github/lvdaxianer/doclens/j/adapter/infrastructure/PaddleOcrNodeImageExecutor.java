package io.github.lvdaxianer.doclens.j.adapter.infrastructure;

import io.github.lvdaxianer.doclens.j.adapter.application.OcrNodeImageExecutor;
import io.github.lvdaxianer.doclens.j.adapter.application.OcrRuntimeNodeView;
import io.github.lvdaxianer.doclens.j.adapter.domain.ImageOcrRequest;
import io.github.lvdaxianer.doclens.j.adapter.domain.ImageOcrResult;

/**
 * PaddleOCR 运行时节点图片识别执行器。
 *
 * @author lvdaxianerplus
 * @date 2026-06-08
 */
public class PaddleOcrNodeImageExecutor implements OcrNodeImageExecutor {

    private final OcrRuntimeNodePool nodePool;
    private final PaddleOcrNativeClient client;
    private final PaddleOcrNativeResponseMapper responseMapper;

    /**
     * 创建 PaddleOCR 节点图片识别执行器。
     *
     * @param nodePool OCR 运行时节点池
     * @param client PaddleOCR 客户端
     * @param responseMapper PaddleOCR 响应映射器
     * @author lvdaxianerplus
     * @date 2026-06-08
     */
    public PaddleOcrNodeImageExecutor(
            OcrRuntimeNodePool nodePool,
            PaddleOcrNativeClient client,
            PaddleOcrNativeResponseMapper responseMapper
    ) {
        this.nodePool = nodePool;
        this.client = client;
        this.responseMapper = responseMapper;
    }

    @Override
    public ImageOcrResult recognize(OcrRuntimeNodeView node, ImageOcrRequest request) {
        OcrRuntimeNode runtimeNode = nodePool.find(node.nodeId())
                .orElseThrow(() -> new IllegalArgumentException("ocr runtime node not found: " + node.nodeId()));
        return responseMapper.map(request.pageNo(), client.recognizeImage(runtimeNode, request.imageContent()));
    }
}
