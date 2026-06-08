package io.github.lvdaxianer.doclens.j.query.application;

/**
 * Dashboard 文档处理轨道节点状态定义。
 *
 * @author lvdaxianerplus
 * @date 2026-06-08
 */
enum ProcessingTrackState {
    DONE("done", "已完成", true),
    CURRENT("current", "当前步骤", true),
    PENDING("pending", "未开始", false),
    SKIPPED("skipped", "该文件类型不需要", false),
    FAILED("failed", "在此步骤失败", true);

    private final String wireValue;
    private final String description;
    private final boolean active;

    /**
     * 创建轨道节点状态。
     *
     * @param wireValue 接口输出值
     * @param description 状态描述
     * @param active 是否属于已激活状态
     * @author lvdaxianerplus
     * @date 2026-06-08
     */
    ProcessingTrackState(String wireValue, String description, boolean active) {
        this.wireValue = wireValue;
        this.description = description;
        this.active = active;
    }

    /**
     * 获取接口输出值。
     *
     * @return 接口输出值
     * @author lvdaxianerplus
     * @date 2026-06-08
     */
    String wireValue() {
        return wireValue;
    }

    /**
     * 获取状态描述。
     *
     * @return 状态描述
     * @author lvdaxianerplus
     * @date 2026-06-08
     */
    String description() {
        return description;
    }

    /**
     * 判断是否属于已激活状态。
     *
     * @return true 表示节点已激活
     * @author lvdaxianerplus
     * @date 2026-06-08
     */
    boolean active() {
        return active;
    }
}
