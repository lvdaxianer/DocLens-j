package io.github.lvdaxianer.doclens.j.api;

/**
 * Event sink implemented by host applications or infrastructure adapters.
 *
 * @author lvdaxianerplus
 * @date 2026-06-07
 */
public interface DocLensEventSink {

    /**
     * Publishes one DocLens lifecycle event.
     *
     * @param event event payload
     * @author lvdaxianerplus
     * @date 2026-06-07
     */
    void publish(DocLensEvent event);
}
