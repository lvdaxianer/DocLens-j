package io.github.lvdaxianer.doclens.j.api;

/**
 * 由宿主应用或基础设施适配器实现的事件接收器。
 *
 * @author lvdaxianerplus
 * @date 2026-06-07
 */
public interface DocLensEventSink {

    /**
     * 发布一个 DocLens 生命周期事件。
     *
     * @param event 事件载荷
     * @author lvdaxianerplus
     * @date 2026-06-07
     */
    void publish(DocLensEvent event);
}
