package io.github.lvdaxianer.doclens.j.adapter.infrastructure;

import io.github.lvdaxianer.doclens.j.adapter.application.OcrPendingRequest;
import io.github.lvdaxianer.doclens.j.adapter.application.OcrPendingRequestQueue;
import java.util.Optional;
import java.util.concurrent.ConcurrentLinkedQueue;
import org.springframework.stereotype.Component;

/**
 * 进程内 OCR 待派发请求队列。
 *
 * @author lvdaxianerplus
 * @date 2026-06-10
 */
@Component
public class InMemoryOcrPendingRequestQueue implements OcrPendingRequestQueue {

    private final ConcurrentLinkedQueue<OcrPendingRequest> requests = new ConcurrentLinkedQueue<>();

    /**
     * 将待派发请求加入队列尾部。
     *
     * @param request 待派发请求
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    @Override
    public void enqueue(OcrPendingRequest request) {
        requests.offer(request);
    }

    /**
     * 从队列头部取出一个待派发请求。
     *
     * @return 待派发请求
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    @Override
    public Optional<OcrPendingRequest> poll() {
        return Optional.ofNullable(requests.poll());
    }
}
