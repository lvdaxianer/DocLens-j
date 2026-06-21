package io.github.lvdaxianer.doclens.j.adapter.application;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 内存 OCR 运行中图片页任务追踪器。
 *
 * @author lvdaxianerplus
 * @date 2026-06-21
 */
public class InMemoryOcrRunningPageTaskTracker implements OcrRunningPageTaskTracker {

    private final Map<String, OcrRunningPageTask> runningTasksByTaskId = new ConcurrentHashMap<>();

    /**
     * 记录图片页任务开始被 worker 线程消费。
     *
     * @param command 运行中图片页任务命令
     * @author lvdaxianerplus
     * @date 2026-06-21
     */
    @Override
    public void recordStart(OcrRunningPageTaskCommand command) {
        runningTasksByTaskId.put(command.taskId(), new OcrRunningPageTask(command.taskId(), command.batchId(),
                command.documentId(), command.pageNo(), command.workerId(), command.threadName(), command.startedAt(),
                Optional.empty(), Optional.empty()));
    }

    /**
     * 记录图片页任务实际分配到的 OCR 节点。
     *
     * @param assignment OCR 节点分配
     * @author lvdaxianerplus
     * @date 2026-06-21
     */
    @Override
    public void recordAssignment(OcrRunningPageTaskAssignment assignment) {
        runningTasksByTaskId.computeIfPresent(assignment.taskId(), (taskId, current) ->
                new OcrRunningPageTask(current.taskId(), current.batchId(), current.documentId(), current.pageNo(),
                        current.workerId(), current.threadName(), current.startedAt(),
                        Optional.of(assignment.modelKey()), Optional.of(assignment.nodeId())));
    }

    /**
     * 记录图片页任务结束，移除运行中快照。
     *
     * @param taskId 页任务 ID
     * @author lvdaxianerplus
     * @date 2026-06-21
     */
    @Override
    public void recordCompletion(String taskId) {
        runningTasksByTaskId.remove(taskId);
    }

    /**
     * 获取指定批次当前运行中的图片页任务快照。
     *
     * @param batchId 批次 ID
     * @return 运行中图片页任务集合
     * @author lvdaxianerplus
     * @date 2026-06-21
     */
    @Override
    public List<OcrRunningPageTask> snapshotByBatch(String batchId) {
        return runningTasksByTaskId.values().stream()
                .filter(task -> task.batchId().equals(batchId))
                .sorted(Comparator.comparing(OcrRunningPageTask::documentId)
                        .thenComparingInt(OcrRunningPageTask::pageNo))
                .toList();
    }
}
