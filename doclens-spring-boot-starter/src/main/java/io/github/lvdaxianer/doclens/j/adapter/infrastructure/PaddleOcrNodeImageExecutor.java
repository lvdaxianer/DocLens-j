package io.github.lvdaxianer.doclens.j.adapter.infrastructure;

import io.github.lvdaxianer.doclens.j.adapter.application.OcrNodeImageExecutor;
import io.github.lvdaxianer.doclens.j.adapter.application.OcrRuntimeNodeView;
import io.github.lvdaxianer.doclens.j.adapter.domain.ImageOcrRequest;
import io.github.lvdaxianer.doclens.j.adapter.domain.ImageOcrResult;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * PaddleOCR 运行时节点图片识别执行器。
 *
 * @author lvdaxianerplus
 * @date 2026-06-08
 */
public class PaddleOcrNodeImageExecutor implements OcrNodeImageExecutor {

    private static final Logger LOGGER = LoggerFactory.getLogger(PaddleOcrNodeImageExecutor.class);

    private final OcrRuntimeNodePool nodePool;
    private final PaddleOcrNativeClient client;
    private final PaddleOcrNativeResponseMapper responseMapper;
    private final ExecutorService ocrRequestExecutor;

    /**
     * 创建 PaddleOCR 节点图片识别执行器。
     *
     * @param nodePool OCR 运行时节点池
     * @param client PaddleOCR 客户端
     * @param responseMapper PaddleOCR 响应映射器
     * @param ocrRequestExecutor OCR 请求线程池
     * @author lvdaxianerplus
     * @date 2026-06-08
     */
    public PaddleOcrNodeImageExecutor(
            OcrRuntimeNodePool nodePool,
            PaddleOcrNativeClient client,
            PaddleOcrNativeResponseMapper responseMapper,
            ExecutorService ocrRequestExecutor
    ) {
        this.nodePool = nodePool;
        this.client = client;
        this.responseMapper = responseMapper;
        this.ocrRequestExecutor = ocrRequestExecutor;
    }

    @Override
    public ImageOcrResult recognize(OcrRuntimeNodeView node, ImageOcrRequest request) {
        OcrRuntimeNode runtimeNode = nodePool.find(node.nodeId())
                .orElseThrow(() -> new IllegalArgumentException("ocr runtime node not found: " + node.nodeId()));
        return waitForResult(ocrRequestExecutor.submit(() -> recognizeOnNode(runtimeNode, request)));
    }

    /**
     * 在线程池中执行 OCR 节点请求。
     *
     * @param runtimeNode OCR 运行时节点
     * @param request 图片 OCR 请求
     * @return 图片 OCR 结果
     * @author lvdaxianerplus
     * @date 2026-06-09
     */
    private ImageOcrResult recognizeOnNode(OcrRuntimeNode runtimeNode, ImageOcrRequest request) {
        LOGGER.info("[OCR请求] 开始请求 OCR 节点, nodeId={}, documentId={}, pageNo={}",
                runtimeNode.node().id(), request.documentId(), request.pageNo());
        ImageOcrResult result = responseMapper.map(request.pageNo(),
                client.recognizeImage(runtimeNode, request.imageContent()));
        LOGGER.info("[OCR请求] OCR 节点请求完成, nodeId={}, documentId={}, pageNo={}",
                runtimeNode.node().id(), request.documentId(), request.pageNo());
        return result;
    }

    /**
     * 等待 OCR 请求线程池执行结果。
     *
     * @param future OCR 请求 Future
     * @return 图片 OCR 结果
     * @author lvdaxianerplus
     * @date 2026-06-09
     */
    private ImageOcrResult waitForResult(Future<ImageOcrResult> future) {
        try {
            return future.get();
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("OCR request interrupted", ex);
        } catch (ExecutionException ex) {
            throw executionFailure(ex);
        }
    }

    /**
     * 转换 OCR 请求线程池执行异常。
     *
     * @param ex Future 执行异常
     * @return 运行时异常
     * @author lvdaxianerplus
     * @date 2026-06-09
     */
    private RuntimeException executionFailure(ExecutionException ex) {
        if (ex.getCause() instanceof RuntimeException runtimeException) {
            return runtimeException;
        } else {
            return new IllegalStateException("OCR request failed", ex);
        }
    }
}
