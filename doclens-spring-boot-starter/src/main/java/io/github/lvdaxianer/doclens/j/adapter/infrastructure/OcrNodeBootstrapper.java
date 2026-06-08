package io.github.lvdaxianer.doclens.j.adapter.infrastructure;

import io.github.lvdaxianer.doclens.j.adapter.domain.OcrNode;
import io.github.lvdaxianer.doclens.j.adapter.domain.OcrNodeCreateRequest;
import io.github.lvdaxianer.doclens.j.adapter.domain.OcrNodeRepository;
import io.github.lvdaxianer.doclens.j.autoconfigure.DocLensSpringProperties.PaddleOcrNodeProperties;
import java.time.OffsetDateTime;
import java.util.List;

/**
 * OCR 节点启动初始化器。
 *
 * @author lvdaxianerplus
 * @date 2026-06-09
 */
public class OcrNodeBootstrapper {

    private static final String PADDLE_OCR_MODEL_KEY = "paddle_ocr";
    private static final String NODE_ID_SEPARATOR = "-";
    private static final int MIN_BOOTSTRAP_NODES = 1;

    private final OcrNodeRepository nodeRepository;
    private final List<PaddleOcrNodeProperties> bootstrapNodes;

    /**
     * 创建 OCR 节点启动初始化器。
     *
     * @param nodeRepository OCR 节点仓储
     * @param bootstrapNodes PaddleOCR 启动节点配置
     * @author lvdaxianerplus
     * @date 2026-06-09
     */
    public OcrNodeBootstrapper(
            OcrNodeRepository nodeRepository,
            List<PaddleOcrNodeProperties> bootstrapNodes
    ) {
        this.nodeRepository = nodeRepository;
        this.bootstrapNodes = List.copyOf(bootstrapNodes);
    }

    /**
     * 初始化 PaddleOCR 节点。
     *
     * @param now 当前时间
     * @author lvdaxianerplus
     * @date 2026-06-09
     */
    public void bootstrap(OffsetDateTime now) {
        if (shouldBootstrap()) {
            nodeRepository.saveAll(toNodes(now));
        } else {
            // 已有页面配置节点时保留用户配置。
        }
    }

    /**
     * 判断是否需要初始化节点。
     *
     * @return true 表示需要初始化
     * @author lvdaxianerplus
     * @date 2026-06-09
     */
    private boolean shouldBootstrap() {
        return !bootstrapNodes.isEmpty()
                && nodeRepository.listByModelKey(PADDLE_OCR_MODEL_KEY).isEmpty()
                && bootstrapNodes.size() >= MIN_BOOTSTRAP_NODES;
    }

    /**
     * 转换启动配置为 OCR 节点。
     *
     * @param node 启动节点配置
     * @param now 当前时间
     * @return OCR 节点
     * @author lvdaxianerplus
     * @date 2026-06-09
     */
    private OcrNode toNode(PaddleOcrNodeProperties node, OffsetDateTime now) {
        return OcrNode.create(new OcrNodeCreateRequest(nodeId(node), PADDLE_OCR_MODEL_KEY, node.name(),
                node.host(), node.port(), node.enabled(), node.participateGlobal(), node.weight(),
                node.maxConcurrency(), now));
    }

    /**
     * 转换所有启动配置为 OCR 节点。
     *
     * @param now 当前时间
     * @return OCR 节点集合
     * @author lvdaxianerplus
     * @date 2026-06-09
     */
    private List<OcrNode> toNodes(OffsetDateTime now) {
        return bootstrapNodes.stream().map(node -> toNode(node, now)).toList();
    }

    /**
     * 构造稳定节点 ID。
     *
     * @param node 启动节点配置
     * @return 节点 ID
     * @author lvdaxianerplus
     * @date 2026-06-09
     */
    private String nodeId(PaddleOcrNodeProperties node) {
        return String.join(NODE_ID_SEPARATOR, PADDLE_OCR_MODEL_KEY, sanitize(node.host()), String.valueOf(node.port()));
    }

    /**
     * 转换主机名为 ID 安全文本。
     *
     * @param value 主机名
     * @return ID 安全文本
     * @author lvdaxianerplus
     * @date 2026-06-09
     */
    private String sanitize(String value) {
        return value.replaceAll("[^A-Za-z0-9]+", NODE_ID_SEPARATOR).replaceAll("(^-+|-+$)", "");
    }
}
