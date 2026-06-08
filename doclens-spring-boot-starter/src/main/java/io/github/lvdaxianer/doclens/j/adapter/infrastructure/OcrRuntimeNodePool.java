package io.github.lvdaxianer.doclens.j.adapter.infrastructure;

import io.github.lvdaxianer.doclens.j.adapter.application.OcrRuntimeNodeView;
import io.github.lvdaxianer.doclens.j.adapter.domain.OcrNode;
import io.github.lvdaxianer.doclens.j.adapter.domain.OcrNodeRepository;
import jakarta.annotation.PostConstruct;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

/**
 * OCR 运行时节点池。
 *
 * @author lvdaxianerplus
 * @date 2026-06-08
 */
@Component
public class OcrRuntimeNodePool {

    private final OcrNodeRepository nodeRepository;
    private final Map<String, OcrRuntimeNode> nodes = new ConcurrentHashMap<>();

    /**
     * 创建 OCR 运行时节点池。
     *
     * @param nodeRepository OCR 节点仓储
     * @author lvdaxianerplus
     * @date 2026-06-08
     */
    public OcrRuntimeNodePool(OcrNodeRepository nodeRepository) {
        this.nodeRepository = nodeRepository;
    }

    /**
     * 启动时刷新启用节点缓存。
     *
     * @author lvdaxianerplus
     * @date 2026-06-08
     */
    @PostConstruct
    public void initialize() {
        refresh();
    }

    /**
     * 从仓储刷新启用节点。
     *
     * @author lvdaxianerplus
     * @date 2026-06-08
     */
    public void refresh() {
        List<OcrNode> enabledNodes = nodeRepository.listEnabled();
        Map<String, OcrRuntimeNode> refreshedNodes = new LinkedHashMap<>(enabledNodes.size());
        enabledNodes.forEach(node -> refreshedNodes.put(node.id(), existingOrNew(node)));
        nodes.clear();
        nodes.putAll(refreshedNodes);
    }

    /**
     * 增加节点解析中图片数。
     *
     * @param nodeId OCR 节点 ID
     * @return 更新后的运行时节点
     * @author lvdaxianerplus
     * @date 2026-06-08
     */
    public Optional<OcrRuntimeNode> incrementInflight(String nodeId) {
        return find(nodeId).map(this::incrementNode);
    }

    /**
     * 减少节点解析中图片数。
     *
     * @param nodeId OCR 节点 ID
     * @return 更新后的运行时节点
     * @author lvdaxianerplus
     * @date 2026-06-08
     */
    public Optional<OcrRuntimeNode> decrementInflight(String nodeId) {
        return find(nodeId).map(this::decrementNode);
    }

    /**
     * 查询运行时节点。
     *
     * @param nodeId OCR 节点 ID
     * @return 运行时节点
     * @author lvdaxianerplus
     * @date 2026-06-08
     */
    public Optional<OcrRuntimeNode> find(String nodeId) {
        return Optional.ofNullable(nodes.get(nodeId));
    }

    /**
     * 导出节点选择视图快照。
     *
     * @return 运行时节点视图集合
     * @author lvdaxianerplus
     * @date 2026-06-08
     */
    public List<OcrRuntimeNodeView> snapshot() {
        return nodes.values().stream().map(OcrRuntimeNode::toView).toList();
    }

    /**
     * 复用已有运行时计数或创建新节点。
     *
     * @param node OCR 节点
     * @return 运行时节点
     * @author lvdaxianerplus
     * @date 2026-06-08
     */
    private OcrRuntimeNode existingOrNew(OcrNode node) {
        return Optional.ofNullable(nodes.get(node.id())).orElseGet(() -> new OcrRuntimeNode(node));
    }

    /**
     * 增加节点计数并返回节点。
     *
     * @param node 运行时节点
     * @return 运行时节点
     * @author lvdaxianerplus
     * @date 2026-06-08
     */
    private OcrRuntimeNode incrementNode(OcrRuntimeNode node) {
        node.incrementInflight();
        return node;
    }

    /**
     * 减少节点计数并返回节点。
     *
     * @param node 运行时节点
     * @return 运行时节点
     * @author lvdaxianerplus
     * @date 2026-06-08
     */
    private OcrRuntimeNode decrementNode(OcrRuntimeNode node) {
        node.decrementInflight();
        return node;
    }
}
