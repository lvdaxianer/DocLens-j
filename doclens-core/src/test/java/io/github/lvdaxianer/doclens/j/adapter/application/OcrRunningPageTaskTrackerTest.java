package io.github.lvdaxianer.doclens.j.adapter.application;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.OffsetDateTime;
import org.junit.jupiter.api.Test;

/**
 * OCR 运行中图片页任务追踪器测试。
 *
 * @author lvdaxianerplus
 * @date 2026-06-21
 */
class OcrRunningPageTaskTrackerTest {

    /**
     * 追踪器应记录页码、线程、worker，并在完成后清理运行中行。
     *
     * @author lvdaxianerplus
     * @date 2026-06-21
     */
    @Test
    void trackerRecordsThreadAndRemovesTaskAfterCompletion() {
        OcrRunningPageTaskTracker tracker = new InMemoryOcrRunningPageTaskTracker();
        OffsetDateTime startedAt = OffsetDateTime.parse("2026-06-21T10:15:30+08:00");
        OcrRunningPageTaskCommand command = new OcrRunningPageTaskCommand("task-1", "batch-1",
                "doc-1", 2, "worker-a", "doclens-page-task-1", startedAt);

        tracker.recordStart(command);
        tracker.recordAssignment(new OcrRunningPageTaskAssignment("task-1", "paddle", "node-a"));

        assertThat(tracker.snapshotByBatch("batch-1"))
                .singleElement()
                .satisfies(row -> {
                    assertThat(row.taskId()).isEqualTo("task-1");
                    assertThat(row.documentId()).isEqualTo("doc-1");
                    assertThat(row.pageNo()).isEqualTo(2);
                    assertThat(row.workerId()).isEqualTo("worker-a");
                    assertThat(row.threadName()).isEqualTo("doclens-page-task-1");
                    assertThat(row.startedAt()).isEqualTo(startedAt);
                    assertThat(row.modelKey()).contains("paddle");
                    assertThat(row.nodeId()).contains("node-a");
                });

        tracker.recordCompletion("task-1");

        assertThat(tracker.snapshotByBatch("batch-1")).isEmpty();
    }
}
