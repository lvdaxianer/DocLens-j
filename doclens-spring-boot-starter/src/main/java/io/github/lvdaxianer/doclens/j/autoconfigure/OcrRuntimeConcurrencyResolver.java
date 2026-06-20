package io.github.lvdaxianer.doclens.j.autoconfigure;

import io.github.lvdaxianer.doclens.j.adapter.domain.OcrNode;
import io.github.lvdaxianer.doclens.j.adapter.domain.OcrNodeRepository;
import io.github.lvdaxianer.doclens.j.adapter.domain.OcrNodeStatus;
import java.util.Optional;

/**
 * OCR 本地运行时并发解析器。
 *
 * @author lvdaxianerplus
 * @date 2026-06-20
 */
class OcrRuntimeConcurrencyResolver {

    private final DocLensSpringProperties properties;
    private final Optional<OcrNodeRepository> nodeRepository;

    /**
     * 创建仅使用配置文件回退的 OCR 并发解析器。
     *
     * @param properties DocLens 配置属性
     * @author lvdaxianerplus
     * @date 2026-06-20
     */
    OcrRuntimeConcurrencyResolver(DocLensSpringProperties properties) {
        this(properties, null);
    }

    /**
     * 创建 OCR 并发解析器。
     *
     * @param properties DocLens 配置属性
     * @param nodeRepository OCR 节点仓储
     * @author lvdaxianerplus
     * @date 2026-06-20
     */
    OcrRuntimeConcurrencyResolver(DocLensSpringProperties properties, OcrNodeRepository nodeRepository) {
        this.properties = properties;
        this.nodeRepository = Optional.ofNullable(nodeRepository);
    }

    /**
     * 解析默认 OCR 本地并发。
     *
     * @return 默认 OCR 本地并发
     * @author lvdaxianerplus
     * @date 2026-06-20
     */
    int defaultConcurrency() {
        int dashboardConcurrency = dashboardNodeConcurrency();
        // 页面存在可参与调度的健康节点时，以页面配置的并发为准。
        if (dashboardConcurrency > 0) {
            return dashboardConcurrency;
        } else {
            // 页面没有可用健康节点时，回退到配置文件 bootstrap 节点并发。
            return DocLensPageTaskWorkerAutoConfiguration.derivedNodeConcurrency(properties);
        }
    }

    /**
     * 统计页面持久化健康节点并发。
     *
     * @return 页面持久化健康节点并发
     * @author lvdaxianerplus
     * @date 2026-06-20
     */
    private int dashboardNodeConcurrency() {
        return nodeRepository.map(repository -> repository.listAll().stream()
                .filter(this::canParticipateInDefaultConcurrency)
                .mapToInt(OcrNode::maxConcurrency)
                .sum()).orElse(0);
    }

    /**
     * 判断节点是否可计入默认本地并发。
     *
     * @param node OCR 节点
     * @return 是否可计入默认本地并发
     * @author lvdaxianerplus
     * @date 2026-06-20
     */
    private boolean canParticipateInDefaultConcurrency(OcrNode node) {
        return node.enabled() && node.participateGlobal() && node.status() == OcrNodeStatus.UP;
    }
}
