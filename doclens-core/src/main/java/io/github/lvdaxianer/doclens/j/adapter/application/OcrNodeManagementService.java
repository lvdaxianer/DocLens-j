package io.github.lvdaxianer.doclens.j.adapter.application;

import io.github.lvdaxianer.doclens.j.adapter.domain.OcrModelDefinition;
import io.github.lvdaxianer.doclens.j.adapter.domain.OcrModelRegistry;
import io.github.lvdaxianer.doclens.j.adapter.domain.OcrNode;
import io.github.lvdaxianer.doclens.j.adapter.domain.OcrNodeDeploymentType;
import io.github.lvdaxianer.doclens.j.adapter.domain.OcrNodeRepository;
import io.github.lvdaxianer.doclens.j.shared.domain.DuplicateResourceException;
import io.github.lvdaxianer.doclens.j.shared.domain.ResourceNotFoundException;
import io.github.lvdaxianer.doclens.j.shared.infrastructure.IdGenerator;
import java.time.OffsetDateTime;
import java.util.List;

/**
 * OCR 节点管理应用服务。
 *
 * @author lvdaxianerplus
 * @date 2026-06-09
 */
public class OcrNodeManagementService {

    private final OcrModelRegistry modelRegistry;
    private final OcrNodeRepository nodeRepository;
    private final IdGenerator idGenerator;
    private final OcrNodePoolRefresher nodePoolRefresher;
    private final OcrNodeSettingsRequestFactory requestFactory;

    /**
     * 创建 OCR 节点管理服务。
     *
     * @param modelRegistry OCR 模型注册表
     * @param nodeRepository OCR 节点仓储
     * @param idGenerator 标识生成器
     * @param nodePoolRefresher 节点池刷新端口
     * @author lvdaxianerplus
     * @date 2026-06-09
     */
    public OcrNodeManagementService(
            OcrModelRegistry modelRegistry,
            OcrNodeRepository nodeRepository,
            IdGenerator idGenerator,
            OcrNodePoolRefresher nodePoolRefresher
    ) {
        this.modelRegistry = modelRegistry;
        this.nodeRepository = nodeRepository;
        this.idGenerator = idGenerator;
        this.nodePoolRefresher = nodePoolRefresher;
        this.requestFactory = new OcrNodeSettingsRequestFactory();
    }

    /**
     * 列出系统支持的 OCR 模型。
     *
     * @return OCR 模型定义集合
     * @author lvdaxianerplus
     * @date 2026-06-09
     */
    public List<OcrModelDefinition> listModels() {
        return modelRegistry.list();
    }

    /**
     * 按模型列出 OCR 节点。
     *
     * @param modelKey OCR 模型标识
     * @return OCR 节点集合
     * @author lvdaxianerplus
     * @date 2026-06-09
     */
    public List<OcrNode> listNodes(String modelKey) {
        modelRegistry.requireSupported(modelKey);
        return nodeRepository.listByModelKey(modelKey);
    }

    /**
     * 创建 OCR 节点。
     *
     * @param modelKey OCR 模型标识
     * @param settings 节点配置
     * @return OCR 节点
     * @author lvdaxianerplus
     * @date 2026-06-09
     */
    public OcrNode createNode(String modelKey, OcrNodeSettings settings) {
        modelRegistry.requireSupported(modelKey);
        ensureUnique(modelKey, settings, "");
        OcrNode node = OcrNode.create(requestFactory.createRequest(newNodeId(), modelKey, settings));
        nodeRepository.save(node);
        refreshNodePool();
        return node;
    }

    /**
     * 更新 OCR 节点配置。
     *
     * @param nodeId OCR 节点 ID
     * @param settings 节点配置
     * @return OCR 节点
     * @author lvdaxianerplus
     * @date 2026-06-09
     */
    public OcrNode updateNode(String nodeId, OcrNodeSettings settings) {
        OcrNode current = requireNode(nodeId);
        ensureUnique(current.modelKey(), settings, nodeId);
        OcrNode updated = current.updateSettings(requestFactory.updateRequest(current, settings));
        nodeRepository.update(updated);
        refreshNodePool();
        return updated;
    }

    /**
     * 更新 OCR 节点启用状态。
     *
     * @param nodeId OCR 节点 ID
     * @param enabled 是否启用
     * @return OCR 节点
     * @author lvdaxianerplus
     * @date 2026-06-09
     */
    public OcrNode changeEnabled(String nodeId, boolean enabled) {
        OcrNode updated = requireNode(nodeId).changeEnabled(enabled, OffsetDateTime.now());
        nodeRepository.update(updated);
        refreshNodePool();
        return updated;
    }

    /**
     * 删除 OCR 节点。
     *
     * @param nodeId OCR 节点 ID
     * @author lvdaxianerplus
     * @date 2026-06-09
     */
    public void deleteNode(String nodeId) {
        requireNode(nodeId);
        nodeRepository.deleteById(nodeId);
        refreshNodePool();
    }

    /**
     * 查询 OCR 节点。
     *
     * @param nodeId OCR 节点 ID
     * @return OCR 节点
     * @author lvdaxianerplus
     * @date 2026-06-09
     */
    public OcrNode requireNode(String nodeId) {
        return nodeRepository.findById(nodeId)
                .orElseThrow(() -> new ResourceNotFoundException("ocr node not found"));
    }

    /**
     * 校验同模型下离线主机端口不重复。
     *
     * @param modelKey OCR 模型标识
     * @param settings 节点配置
     * @param currentNodeId 当前节点 ID
     * @author lvdaxianerplus
     * @date 2026-06-09
     */
    private void ensureUnique(String modelKey, OcrNodeSettings settings, String currentNodeId) {
        if (requestFactory.deploymentType(settings) == OcrNodeDeploymentType.OFFLINE) {
            ensureOfflineUnique(uniqueCheck(modelKey, settings, currentNodeId));
        } else {
            // 在线节点不使用 host + port 作为资源地址，不执行离线地址唯一性校验。
        }
    }

    /**
     * 校验离线节点地址唯一。
     *
     * @param request 唯一性校验请求
     * @author lvdaxianerplus
     * @date 2026-06-09
     */
    private void ensureOfflineUnique(OcrNodeUniqueCheck request) {
        boolean duplicated = nodeRepository.findByModelHostPort(request.modelKey(), request.host(), request.port())
                .filter(node -> !node.id().equals(request.currentNodeId()))
                .isPresent();
        if (duplicated) {
            throw new DuplicateResourceException("duplicate ocr node host and port");
        } else {
            // 当前 host + port 在该模型下可用。
        }
    }

    /**
     * 创建离线唯一性校验请求。
     *
     * @param modelKey OCR 模型标识
     * @param settings 节点配置
     * @param currentNodeId 当前节点 ID
     * @return 唯一性校验请求
     * @author lvdaxianerplus
     * @date 2026-06-09
     */
    private OcrNodeUniqueCheck uniqueCheck(String modelKey, OcrNodeSettings settings, String currentNodeId) {
        OcrNodeSettings.Endpoint endpoint = requestFactory.endpoint(settings);
        return new OcrNodeUniqueCheck(modelKey, endpoint.host(), endpoint.port(), currentNodeId);
    }

    /**
     * 创建 OCR 节点 ID。
     *
     * @return OCR 节点 ID
     * @author lvdaxianerplus
     * @date 2026-06-09
     */
    private String newNodeId() {
        return "ocr_node_" + idGenerator.newDocumentId().substring("doc_".length());
    }

    /**
     * 刷新运行时节点池。
     *
     * @author lvdaxianerplus
     * @date 2026-06-09
     */
    private void refreshNodePool() {
        nodePoolRefresher.refresh();
    }

    /**
     * OCR 节点唯一性校验请求。
     *
     * @param modelKey OCR 模型标识
     * @param host 节点主机
     * @param port 节点端口
     * @param currentNodeId 当前节点 ID
     * @author lvdaxianerplus
     * @date 2026-06-09
     */
    private record OcrNodeUniqueCheck(String modelKey, String host, int port, String currentNodeId) {
    }

}
