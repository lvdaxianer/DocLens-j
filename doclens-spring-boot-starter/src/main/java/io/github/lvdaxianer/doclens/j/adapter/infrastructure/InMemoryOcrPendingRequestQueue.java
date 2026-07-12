package io.github.lvdaxianer.doclens.j.adapter.infrastructure;

import io.github.lvdaxianer.doclens.j.adapter.application.OcrPendingRequest;
import io.github.lvdaxianer.doclens.j.adapter.application.OcrPendingRequestQueue;
import java.util.Optional;
import java.util.concurrent.ArrayBlockingQueue;

/**
 * 进程内 OCR 待派发请求队列。
 *
 * @author lvdaxianerplus
 * @date 2026-06-10
 */
public class InMemoryOcrPendingRequestQueue implements OcrPendingRequestQueue {

    public static final int DEFAULT_CAPACITY = 1000;

    private final ArrayBlockingQueue<OcrPendingRequest> requests;

    /**
     * 创建默认容量的进程内 OCR 待派发请求队列。
     *
     * @author lvdaxianer@yeah.net
     * @date 2026-07-12
     */
    public InMemoryOcrPendingRequestQueue() {
        this(DEFAULT_CAPACITY);
    }

    /**
     * 创建指定容量的进程内 OCR 待派发请求队列。
     *
     * @param capacity 队列容量
     * @author lvdaxianer@yeah.net
     * @date 2026-07-12
     */
    public InMemoryOcrPendingRequestQueue(int capacity) {
        this.requests = new ArrayBlockingQueue<>(validCapacity(capacity));
    }

    /**
     * 将待派发请求加入队列尾部。
     *
     * @param request 待派发请求
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    @Override
    public boolean enqueue(OcrPendingRequest request) {
        return requests.offer(request);
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

    /**
     * 校验队列容量配置。
     *
     * @param capacity 队列容量
     * @return 合法队列容量
     * @author lvdaxianer@yeah.net
     * @date 2026-07-12
     */
    private static int validCapacity(int capacity) {
        if (capacity > 0) {
            return capacity;
        } else {
            throw new IllegalArgumentException("ocr dispatch queue capacity must be positive");
        }
    }
}
