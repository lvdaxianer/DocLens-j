package io.github.lvdaxianer.doclens.j.api;

/**
 * 宿主应用未订阅事件时使用的空操作事件接收器。
 *
 * @author lvdaxianerplus
 * @date 2026-06-07
 */
public final class NoopDocLensEventSink implements DocLensEventSink {

    @Override
    public void publish(DocLensEvent event) {
        if (event == null) {
            // 忽略空事件，确保空操作接收器没有副作用。
        } else {
            // 该实现会有意丢弃有效事件。
        }
    }
}
