package io.github.lvdaxianer.doclens.j.adapter.domain;

import java.util.List;
import java.util.Optional;

/**
 * OCR 节点仓储接口。
 *
 * @author lvdaxianerplus
 * @date 2026-06-08
 */
public interface OcrNodeRepository {

    /**
     * 保存新的 OCR 节点。
     *
     * @param node OCR 节点
     * @author lvdaxianerplus
     * @date 2026-06-08
     */
    void save(OcrNode node);

    /**
     * 批量保存新的 OCR 节点。
     *
     * @param nodes OCR 节点集合
     * @author lvdaxianerplus
     * @date 2026-06-09
     */
    void saveAll(List<OcrNode> nodes);

    /**
     * 更新已有 OCR 节点。
     *
     * @param node OCR 节点
     * @author lvdaxianerplus
     * @date 2026-06-08
     */
    void update(OcrNode node);

    /**
     * 根据节点 ID 查询 OCR 节点。
     *
     * @param nodeId OCR 节点 ID
     * @return OCR 节点
     * @author lvdaxianerplus
     * @date 2026-06-08
     */
    Optional<OcrNode> findById(String nodeId);

    /**
     * 按 OCR 模型标识列出节点。
     *
     * @param modelKey OCR 模型标识
     * @return OCR 节点集合
     * @author lvdaxianerplus
     * @date 2026-06-08
     */
    List<OcrNode> listByModelKey(String modelKey);

    /**
     * 列出所有启用节点。
     *
     * @return 启用 OCR 节点集合
     * @author lvdaxianerplus
     * @date 2026-06-08
     */
    List<OcrNode> listEnabled();

    /**
     * 列出所有 OCR 节点。
     *
     * @return OCR 节点集合
     * @author lvdaxianerplus
     * @date 2026-06-08
     */
    List<OcrNode> listAll();

    /**
     * 按模型和主机端口查询节点。
     *
     * @param modelKey OCR 模型标识
     * @param host 节点主机
     * @param port 节点端口
     * @return OCR 节点
     * @author lvdaxianerplus
     * @date 2026-06-08
     */
    Optional<OcrNode> findByModelHostPort(String modelKey, String host, int port);

    /**
     * 删除 OCR 节点。
     *
     * @param nodeId OCR 节点 ID
     * @author lvdaxianerplus
     * @date 2026-06-09
     */
    void deleteById(String nodeId);
}
