package io.github.lvdaxianer.doclens.j.adapter.application;

import java.util.Optional;

/**
 * OCR 待派发请求队列。
 *
 * @author lvdaxianerplus
 * @date 2026-06-10
 */
public interface OcrPendingRequestQueue {

    /**
     * 将待派发请求加入队列。
     *
     * @param request 待派发请求
     * @return 是否成功入队
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    boolean enqueue(OcrPendingRequest request);

    /**
     * 从队列头部取出一个待派发请求。
     *
     * @return 待派发请求
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    Optional<OcrPendingRequest> poll();
}
