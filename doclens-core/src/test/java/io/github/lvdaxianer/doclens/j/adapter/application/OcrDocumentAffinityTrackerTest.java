package io.github.lvdaxianer.doclens.j.adapter.application;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/**
 * OCR 文档级模型亲和力跟踪器测试。
 *
 * @author lvdaxianerplus
 * @date 2026-06-12
 */
class OcrDocumentAffinityTrackerTest {

    private static final String DOCUMENT_ID = "doc-1";
    private static final String OTHER_DOCUMENT_ID = "doc-2";
    private static final String PADDLE_MODEL_KEY = "paddle_ocr";
    private static final String OLLAMA_MODEL_KEY = "ollama_deepseek_ocr";

    /**
     * 首次绑定后，同一文档后续请求必须继续读取首次模型。
     *
     * @author lvdaxianerplus
     * @date 2026-06-12
     */
    @Test
    void bindsDocumentToFirstModelKeyOnly() {
        OcrDocumentAffinityTracker tracker = new InMemoryOcrDocumentAffinityTracker();

        String firstModelKey = tracker.bindIfAbsent(DOCUMENT_ID, PADDLE_MODEL_KEY);
        String laterModelKey = tracker.bindIfAbsent(DOCUMENT_ID, OLLAMA_MODEL_KEY);

        assertThat(firstModelKey).isEqualTo(PADDLE_MODEL_KEY);
        assertThat(laterModelKey).isEqualTo(PADDLE_MODEL_KEY);
        assertThat(tracker.boundModelKey(DOCUMENT_ID)).contains(PADDLE_MODEL_KEY);
    }

    /**
     * 文档亲和力必须按 documentId 隔离，不能按 batch 共享。
     *
     * @author lvdaxianerplus
     * @date 2026-06-12
     */
    @Test
    void keepsBindingIsolatedPerDocument() {
        OcrDocumentAffinityTracker tracker = new InMemoryOcrDocumentAffinityTracker();

        tracker.bindIfAbsent(DOCUMENT_ID, PADDLE_MODEL_KEY);
        tracker.bindIfAbsent(OTHER_DOCUMENT_ID, OLLAMA_MODEL_KEY);

        assertThat(tracker.boundModelKey(DOCUMENT_ID)).contains(PADDLE_MODEL_KEY);
        assertThat(tracker.boundModelKey(OTHER_DOCUMENT_ID)).contains(OLLAMA_MODEL_KEY);
    }

    /**
     * 文档完成后释放绑定，下一轮解析可重新选择模型。
     *
     * @author lvdaxianerplus
     * @date 2026-06-12
     */
    @Test
    void releaseRemovesDocumentBinding() {
        OcrDocumentAffinityTracker tracker = new InMemoryOcrDocumentAffinityTracker();

        tracker.bindIfAbsent(DOCUMENT_ID, PADDLE_MODEL_KEY);
        tracker.release(DOCUMENT_ID);

        assertThat(tracker.boundModelKey(DOCUMENT_ID)).isEmpty();
    }
}
