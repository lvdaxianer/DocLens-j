package io.github.lvdaxianer.doclens.j.api;

/**
 * No-op event sink used when host applications do not subscribe to events.
 *
 * @author lvdaxianerplus
 * @date 2026-06-07
 */
public final class NoopDocLensEventSink implements DocLensEventSink {

    @Override
    public void publish(DocLensEvent event) {
        if (event == null) {
            // Null events are ignored to keep the no-op sink side-effect free.
        } else {
            // Valid events are intentionally dropped by this implementation.
        }
    }
}
