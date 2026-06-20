package io.github.lvdaxianer.doclens.j.query.infrastructure;

import io.github.lvdaxianer.doclens.j.adapter.domain.OcrNode;
import io.github.lvdaxianer.doclens.j.adapter.domain.OcrNodeRepository;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 查询侧测试用内存 OCR 节点仓储。
 *
 * @author lvdaxianerplus
 * @date 2026-06-21
 */
final class InMemoryOcrNodeRepository implements OcrNodeRepository {

    /** 内存节点仓储初始容量。 */
    private static final int TEST_NODE_CAPACITY = 8;

    private final Map<String, OcrNode> nodes = new HashMap<>(TEST_NODE_CAPACITY);

    /**
     * 创建内存节点仓储。
     *
     * @param seedNodes 初始节点
     * @author lvdaxianerplus
     * @date 2026-06-21
     */
    InMemoryOcrNodeRepository(List<OcrNode> seedNodes) {
        // 节点按 ID 建索引，匹配真实仓储的主键查询语义。
        seedNodes.forEach(node -> nodes.put(node.id(), node));
    }

    /**
     * 保存 OCR 节点。
     *
     * @param node OCR 节点
     * @author lvdaxianerplus
     * @date 2026-06-21
     */
    @Override
    public void save(OcrNode node) {
        nodes.put(node.id(), node);
    }

    /**
     * 批量保存 OCR 节点。
     *
     * @param seedNodes OCR 节点集合
     * @author lvdaxianerplus
     * @date 2026-06-21
     */
    @Override
    public void saveAll(List<OcrNode> seedNodes) {
        seedNodes.forEach(this::save);
    }

    /**
     * 更新 OCR 节点。
     *
     * @param node OCR 节点
     * @author lvdaxianerplus
     * @date 2026-06-21
     */
    @Override
    public void update(OcrNode node) {
        nodes.put(node.id(), node);
    }

    /**
     * 按节点 ID 查找节点。
     *
     * @param nodeId 节点 ID
     * @return OCR 节点
     * @author lvdaxianerplus
     * @date 2026-06-21
     */
    @Override
    public Optional<OcrNode> findById(String nodeId) {
        return Optional.ofNullable(nodes.get(nodeId));
    }

    /**
     * 按模型、host 和端口查找节点。
     *
     * @param modelKey OCR 模型 key
     * @param host 节点 host
     * @param port 节点端口
     * @return OCR 节点
     * @author lvdaxianerplus
     * @date 2026-06-21
     */
    @Override
    public Optional<OcrNode> findByModelHostPort(String modelKey, String host, int port) {
        // host/port 唯一性查询仅在节点池初始化路径中使用。
        return nodes.values().stream()
                .filter(node -> node.modelKey().equals(modelKey) && node.host().equals(host) && node.port() == port)
                .findFirst();
    }

    /**
     * 按模型 key 列出节点。
     *
     * @param modelKey OCR 模型 key
     * @return OCR 节点集合
     * @author lvdaxianerplus
     * @date 2026-06-21
     */
    @Override
    public List<OcrNode> listByModelKey(String modelKey) {
        return nodes.values().stream().filter(node -> node.modelKey().equals(modelKey)).toList();
    }

    /**
     * 列出启用节点。
     *
     * @return OCR 节点集合
     * @author lvdaxianerplus
     * @date 2026-06-21
     */
    @Override
    public List<OcrNode> listEnabled() {
        return nodes.values().stream().filter(OcrNode::enabled).toList();
    }

    /**
     * 列出全部节点。
     *
     * @return OCR 节点集合
     * @author lvdaxianerplus
     * @date 2026-06-21
     */
    @Override
    public List<OcrNode> listAll() {
        return List.copyOf(nodes.values());
    }

    /**
     * 按节点 ID 删除节点。
     *
     * @param nodeId 节点 ID
     * @author lvdaxianerplus
     * @date 2026-06-21
     */
    @Override
    public void deleteById(String nodeId) {
        nodes.remove(nodeId);
    }
}
