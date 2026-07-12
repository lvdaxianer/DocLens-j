package io.github.lvdaxianer.doclens.j.adapter.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.lvdaxianer.doclens.j.adapter.application.OcrPendingRequest;
import io.github.lvdaxianer.doclens.j.adapter.application.OcrRuntimeNodeView;
import io.github.lvdaxianer.doclens.j.adapter.domain.ImageOcrRequest;
import io.github.lvdaxianer.doclens.j.adapter.domain.OcrRoutePolicy;
import io.github.lvdaxianer.doclens.j.shared.domain.JsonPayload;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.Test;

/**
 * 进程内 OCR 待派发请求队列测试。
 *
 * @author lvdaxianer@yeah.net
 * @date 2026-07-12
 */
class InMemoryOcrPendingRequestQueueTest {

    /**
     * 队列达到容量上限后应拒绝继续入队。
     *
     * @author lvdaxianer@yeah.net
     * @date 2026-07-12
     */
    @Test
    void rejectsEnqueueWhenCapacityIsFull() {
        InMemoryOcrPendingRequestQueue queue = new InMemoryOcrPendingRequestQueue(1);

        boolean firstAccepted = queue.enqueue(request("doc-1"));
        boolean secondAccepted = queue.enqueue(request("doc-2"));

        assertThat(firstAccepted).isTrue();
        assertThat(secondAccepted).isFalse();
        assertThat(queue.poll()).isPresent();
        assertThat(queue.poll()).isEmpty();
    }

    /**
     * 创建待派发请求。
     *
     * @param documentId 文档 ID
     * @return 待派发请求
     * @author lvdaxianer@yeah.net
     * @date 2026-07-12
     */
    private OcrPendingRequest request(String documentId) {
        ImageOcrRequest request = new ImageOcrRequest("batch-1", documentId, documentId + ".png", 1,
                "image".getBytes(), JsonPayload.empty());
        return OcrPendingRequest.from(request, OcrRoutePolicy.globalLoadBalance("least-inflight"), Set.of(),
                new CompletableFuture<OcrRuntimeNodeView>());
    }
}
