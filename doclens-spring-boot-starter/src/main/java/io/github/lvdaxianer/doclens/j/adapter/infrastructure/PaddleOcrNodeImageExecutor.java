package io.github.lvdaxianer.doclens.j.adapter.infrastructure;

import io.github.lvdaxianer.doclens.j.adapter.application.OcrNodeImageExecutor;
import io.github.lvdaxianer.doclens.j.adapter.application.OcrRuntimeNodeView;
import io.github.lvdaxianer.doclens.j.adapter.domain.ImageOcrRequest;
import io.github.lvdaxianer.doclens.j.adapter.domain.ImageOcrResult;
import io.github.lvdaxianer.doclens.j.adapter.domain.OcrNodeDeploymentType;
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
    private static final String OLLAMA_CHANNEL_KEY = "ollama";
    private static final String OLLAMA_MODEL_PREFIX = "ollama";

    private final OcrRuntimeNodePool nodePool;
    private final OcrNodeProtocolClients protocolClients;
    private final ExecutorService ocrRequestExecutor;

    /**
     * 创建 PaddleOCR 节点图片识别执行器。
     *
     * @param nodePool OCR 运行时节点池
     * @param protocolClients OCR 协议客户端集合
     * @param ocrRequestExecutor OCR 请求线程池
     * @author lvdaxianerplus
     * @date 2026-06-12
     */
    public PaddleOcrNodeImageExecutor(
            OcrRuntimeNodePool nodePool,
            OcrNodeProtocolClients protocolClients,
            ExecutorService ocrRequestExecutor
    ) {
        this.nodePool = nodePool;
        this.protocolClients = protocolClients;
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
        ImageOcrResult result = recognizeByDeployment(runtimeNode, request);
        LOGGER.info("[OCR请求] OCR 节点请求完成, nodeId={}, documentId={}, pageNo={}",
                runtimeNode.node().id(), request.documentId(), request.pageNo());
        return result;
    }

    /**
     * 按节点部署类型分发 OCR 请求。
     *
     * @param runtimeNode OCR 运行时节点
     * @param request 图片 OCR 请求
     * @return 图片 OCR 结果
     * @author lvdaxianerplus
     * @date 2026-06-09
     */
    private ImageOcrResult recognizeByDeployment(OcrRuntimeNode runtimeNode, ImageOcrRequest request) {
        if (runtimeNode.node().deploymentType() == OcrNodeDeploymentType.ONLINE) {
            // 在线托管 OCR 走 DashScope compatible 协议。
            return protocolClients.onlineClient().recognizeImage(runtimeNode, request);
        } else if (isOllamaNode(runtimeNode)) {
            // Ollama DeepSeek-OCR 节点走 /api/generate 协议。
            return protocolClients.ollamaClient().recognizeImage(runtimeNode, request);
        } else {
            // 其他离线节点保持 PaddleOCR 原生 /ocr 协议。
            return protocolClients.paddleResponseMapper().map(request.pageNo(),
                    protocolClients.paddleClient().recognizeImage(runtimeNode, request.imageContent()));
        }
    }

    /**
     * 判断节点是否为 Ollama OCR 节点。
     *
     * @param runtimeNode OCR 运行时节点
     * @return 是否 Ollama OCR 节点
     * @author lvdaxianerplus
     * @date 2026-06-12
     */
    private boolean isOllamaNode(OcrRuntimeNode runtimeNode) {
        if (runtimeNode.node().channelKey().filter(OLLAMA_CHANNEL_KEY::equals).isPresent()) {
            return true;
        } else {
            return runtimeNode.node().modelKey().startsWith(OLLAMA_MODEL_PREFIX);
        }
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
