package io.github.lvdaxianer.doclens.j.adapter.application;

import io.github.lvdaxianer.doclens.j.adapter.domain.OcrModelDefinition;
import io.github.lvdaxianer.doclens.j.adapter.domain.OcrModelRegistry;
import io.github.lvdaxianer.doclens.j.adapter.domain.OcrNode;
import io.github.lvdaxianer.doclens.j.adapter.domain.OcrNodeCreateRequest;
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

    private static final int DEFAULT_WEIGHT = 100;
    private static final int DEFAULT_MAX_CONCURRENCY = 4;

    private final OcrModelRegistry modelRegistry;
    private final OcrNodeRepository nodeRepository;
    private final IdGenerator idGenerator;
    private final OcrNodePoolRefresher nodePoolRefresher;

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
        OcrNode node = OcrNode.create(createRequest(newNodeId(), modelKey, settings, credentialForCreate(settings)));
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
        OcrNode updated = current.updateSettings(
                createRequest(nodeId, current.modelKey(), settings, credentialForUpdate(current, settings)));
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
     * 创建领域节点请求。
     *
     * @param nodeId OCR 节点 ID
     * @param modelKey OCR 模型标识
     * @param settings 节点配置
     * @param credential 凭证持久化决策
     * @return OCR 节点创建请求
     * @author lvdaxianerplus
     * @date 2026-06-09
     */
    private OcrNodeCreateRequest createRequest(
            String nodeId,
            String modelKey,
            OcrNodeSettings settings,
            OcrNodeCredential credential
    ) {
        OcrNodeSettings.Endpoint endpoint = endpoint(settings);
        OcrNodeSettings.Online online = online(settings);
        OcrNodeSettings.Scheduling scheduling = scheduling(settings);
        return new OcrNodeCreateRequest(nodeId, modelKey, deploymentType(settings), endpoint.name(), endpoint.host(),
                endpoint.port(), online.channelKey(), online.providerModel(), credential.credentialRef(),
                credential.credentialConfigured(), scheduling.enabled(), scheduling.participateGlobal(),
                scheduling.weight(), scheduling.maxConcurrency(), OffsetDateTime.now());
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
        if (deploymentType(settings) == OcrNodeDeploymentType.OFFLINE) {
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
        OcrNodeSettings.Endpoint endpoint = endpoint(settings);
        return new OcrNodeUniqueCheck(modelKey, endpoint.host(), endpoint.port(), currentNodeId);
    }

    /**
     * 创建在线节点凭证决策。
     *
     * @param settings 节点配置
     * @return 凭证决策
     * @author lvdaxianerplus
     * @date 2026-06-09
     */
    private OcrNodeCredential credentialForCreate(OcrNodeSettings settings) {
        if (deploymentType(settings) == OcrNodeDeploymentType.ONLINE) {
            return new OcrNodeCredential(requiredApiKey(online(settings).apiKey()), true);
        } else {
            return new OcrNodeCredential("", false);
        }
    }

    /**
     * 创建编辑在线节点凭证决策。
     *
     * @param current 当前节点
     * @param settings 节点配置
     * @return 凭证决策
     * @author lvdaxianerplus
     * @date 2026-06-09
     */
    private OcrNodeCredential credentialForUpdate(OcrNode current, OcrNodeSettings settings) {
        if (deploymentType(settings) == OcrNodeDeploymentType.ONLINE) {
            return credentialFromUpdate(current, settings);
        } else {
            return new OcrNodeCredential("", false);
        }
    }

    /**
     * 从编辑请求中解析在线凭证。
     *
     * @param current 当前节点
     * @param settings 节点配置
     * @return 凭证决策
     * @author lvdaxianerplus
     * @date 2026-06-09
     */
    private OcrNodeCredential credentialFromUpdate(OcrNode current, OcrNodeSettings settings) {
        String apiKey = normalized(online(settings).apiKey());
        if (!apiKey.isBlank()) {
            return new OcrNodeCredential(apiKey, true);
        } else if (current.credentialConfigured() && current.credentialRef().isPresent()) {
            return new OcrNodeCredential(current.credentialRef().orElse(""), true);
        } else {
            return new OcrNodeCredential(requiredApiKey(apiKey), false);
        }
    }

    /**
     * 获取节点部署类型。
     *
     * @param settings 节点配置
     * @return 节点部署类型
     * @author lvdaxianerplus
     * @date 2026-06-09
     */
    private OcrNodeDeploymentType deploymentType(OcrNodeSettings settings) {
        return settings.deploymentType() == null ? OcrNodeDeploymentType.OFFLINE : settings.deploymentType();
    }

    /**
     * 获取节点地址配置。
     *
     * @param settings 节点配置
     * @return 节点地址配置
     * @author lvdaxianerplus
     * @date 2026-06-09
     */
    private OcrNodeSettings.Endpoint endpoint(OcrNodeSettings settings) {
        return settings.endpoint() == null ? new OcrNodeSettings.Endpoint("", "", 0) : settings.endpoint();
    }

    /**
     * 获取在线节点配置。
     *
     * @param settings 节点配置
     * @return 在线节点配置
     * @author lvdaxianerplus
     * @date 2026-06-09
     */
    private OcrNodeSettings.Online online(OcrNodeSettings settings) {
        return settings.online() == null ? new OcrNodeSettings.Online("", "", "") : settings.online();
    }

    /**
     * 获取节点调度配置。
     *
     * @param settings 节点配置
     * @return 节点调度配置
     * @author lvdaxianerplus
     * @date 2026-06-09
     */
    private OcrNodeSettings.Scheduling scheduling(OcrNodeSettings settings) {
        return settings.scheduling() == null ? new OcrNodeSettings.Scheduling(true, true, DEFAULT_WEIGHT,
                DEFAULT_MAX_CONCURRENCY)
                : settings.scheduling();
    }

    /**
     * 校验新增在线节点 API Key。
     *
     * @param apiKey API Key
     * @return 标准化 API Key
     * @author lvdaxianerplus
     * @date 2026-06-09
     */
    private String requiredApiKey(String apiKey) {
        String normalized = normalized(apiKey);
        if (!normalized.isBlank()) {
            return normalized;
        } else {
            throw new IllegalArgumentException("ocr online api key is required");
        }
    }

    /**
     * 标准化可选文本。
     *
     * @param value 可选文本
     * @return 标准化后的文本
     * @author lvdaxianerplus
     * @date 2026-06-09
     */
    private String normalized(String value) {
        return value == null ? "" : value.trim();
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

    /**
     * OCR 节点凭证持久化决策。
     *
     * @param credentialRef 凭证引用
     * @param credentialConfigured 是否已配置凭证
     * @author lvdaxianerplus
     * @date 2026-06-09
     */
    private record OcrNodeCredential(String credentialRef, boolean credentialConfigured) {
    }
}
