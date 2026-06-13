package io.github.lvdaxianer.doclens.j.adapter.application;

import io.github.lvdaxianer.doclens.j.adapter.domain.OcrNode;
import io.github.lvdaxianer.doclens.j.adapter.domain.OcrNodeCreateRequest;
import io.github.lvdaxianer.doclens.j.adapter.domain.OcrNodeDeploymentType;
import java.time.OffsetDateTime;
import java.util.regex.Pattern;

/**
 * OCR 节点配置请求工厂。
 *
 * @author lvdaxianerplus
 * @date 2026-06-11
 */
final class OcrNodeSettingsRequestFactory {

    private static final int DEFAULT_WEIGHT = 100;
    private static final int DEFAULT_MAX_CONCURRENCY = 4;
    private static final Pattern ENV_VAR_NAME = Pattern.compile("[A-Z_][A-Z0-9_]*");

    /**
     * 创建 OCR 节点配置请求工厂。
     *
     * @author lvdaxianerplus
     * @date 2026-06-11
     */
    OcrNodeSettingsRequestFactory() {
    }

    /**
     * 创建新增节点请求。
     *
     * @param nodeId OCR 节点 ID
     * @param modelKey OCR 模型标识
     * @param settings 节点配置
     * @return OCR 节点创建请求
     * @author lvdaxianerplus
     * @date 2026-06-11
     */
    OcrNodeCreateRequest createRequest(String nodeId, String modelKey, OcrNodeSettings settings) {
        return createRequest(nodeId, modelKey, settings, credentialForCreate(settings));
    }

    /**
     * 创建更新节点请求。
     *
     * @param current 当前节点
     * @param settings 节点配置
     * @return OCR 节点创建请求
     * @author lvdaxianerplus
     * @date 2026-06-11
     */
    OcrNodeCreateRequest updateRequest(OcrNode current, OcrNodeSettings settings) {
        return createRequest(current.id(), current.modelKey(), settings, credentialForUpdate(current, settings));
    }

    /**
     * 获取节点部署类型。
     *
     * @param settings 节点配置
     * @return 节点部署类型
     * @author lvdaxianerplus
     * @date 2026-06-11
     */
    OcrNodeDeploymentType deploymentType(OcrNodeSettings settings) {
        return settings.deploymentType() == null ? OcrNodeDeploymentType.OFFLINE : settings.deploymentType();
    }

    /**
     * 获取节点地址配置。
     *
     * @param settings 节点配置
     * @return 节点地址配置
     * @author lvdaxianerplus
     * @date 2026-06-11
     */
    OcrNodeSettings.Endpoint endpoint(OcrNodeSettings settings) {
        return settings.endpoint() == null ? new OcrNodeSettings.Endpoint("", "", 0) : settings.endpoint();
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
     * @date 2026-06-11
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
     * 创建在线节点凭证决策。
     *
     * @param settings 节点配置
     * @return 凭证决策
     * @author lvdaxianerplus
     * @date 2026-06-11
     */
    private OcrNodeCredential credentialForCreate(OcrNodeSettings settings) {
        if (deploymentType(settings) == OcrNodeDeploymentType.ONLINE) {
            // 新增在线节点必须显式提供 API Key 环境变量名。
            return new OcrNodeCredential(requiredCredentialEnvVar(online(settings).credentialEnvVar()), true);
        } else {
            // 离线节点不保存在线凭证。
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
     * @date 2026-06-11
     */
    private OcrNodeCredential credentialForUpdate(OcrNode current, OcrNodeSettings settings) {
        if (deploymentType(settings) == OcrNodeDeploymentType.ONLINE) {
            // 在线节点编辑时允许复用旧凭证。
            return credentialFromUpdate(current, settings);
        } else {
            // 切换为离线节点时清理在线凭证。
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
     * @date 2026-06-11
     */
    private OcrNodeCredential credentialFromUpdate(OcrNode current, OcrNodeSettings settings) {
        String credentialEnvVar = normalized(online(settings).credentialEnvVar());
        if (!credentialEnvVar.isBlank()) {
            // 请求带了新环境变量名时覆盖旧凭证引用。
            return new OcrNodeCredential(requiredCredentialEnvVar(credentialEnvVar), true);
        } else if (current.credentialConfigured() && current.credentialRef().isPresent()) {
            // 未输入新环境变量名时复用当前已配置凭证引用。
            return new OcrNodeCredential(current.credentialRef().orElse(""), true);
        } else {
            // 当前没有可复用凭证引用时仍要求显式输入。
            return new OcrNodeCredential(requiredCredentialEnvVar(credentialEnvVar), false);
        }
    }

    /**
     * 获取在线节点配置。
     *
     * @param settings 节点配置
     * @return 在线节点配置
     * @author lvdaxianerplus
     * @date 2026-06-11
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
     * @date 2026-06-11
     */
    private OcrNodeSettings.Scheduling scheduling(OcrNodeSettings settings) {
        return settings.scheduling() == null ? new OcrNodeSettings.Scheduling(true, true, DEFAULT_WEIGHT,
                DEFAULT_MAX_CONCURRENCY)
                : settings.scheduling();
    }

    /**
     * 校验在线节点 API Key 环境变量名。
     *
     * @param credentialEnvVar API Key 环境变量名
     * @return 标准化 API Key 环境变量名
     * @author lvdaxianerplus
     * @date 2026-06-13
     */
    private String requiredCredentialEnvVar(String credentialEnvVar) {
        String normalized = normalized(credentialEnvVar);
        if (normalized.isBlank()) {
            // 在线节点没有可用环境变量名时不能创建或更新。
            throw new IllegalArgumentException("ocr online credential env var is required");
        } else if (ENV_VAR_NAME.matcher(normalized).matches()) {
            // 标准化后合法的环境变量名可以持久化为凭证引用。
            return normalized;
        } else {
            // 环境变量名必须可被服务器进程稳定读取。
            throw new IllegalArgumentException("credential env var must match [A-Z_][A-Z0-9_]*");
        }
    }

    /**
     * 标准化可选文本。
     *
     * @param value 可选文本
     * @return 标准化后的文本
     * @author lvdaxianerplus
     * @date 2026-06-11
     */
    private String normalized(String value) {
        return value == null ? "" : value.trim();
    }

    /**
     * OCR 节点凭证持久化决策。
     *
     * @param credentialRef 凭证引用
     * @param credentialConfigured 是否已配置凭证
     * @author lvdaxianerplus
     * @date 2026-06-11
     */
    private record OcrNodeCredential(String credentialRef, boolean credentialConfigured) {
    }
}
