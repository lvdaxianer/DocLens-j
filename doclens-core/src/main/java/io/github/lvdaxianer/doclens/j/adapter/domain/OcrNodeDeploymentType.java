package io.github.lvdaxianer.doclens.j.adapter.domain;

/**
 * OCR 节点部署类型。
 *
 * @author lvdaxianerplus
 * @date 2026-06-09
 */
public enum OcrNodeDeploymentType {
    /**
     * 用户自建或内网部署的 OCR 服务。
     */
    OFFLINE,

    /**
     * 云厂商托管 OCR 或视觉模型。
     */
    ONLINE
}
