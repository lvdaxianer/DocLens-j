package io.github.lvdaxianer.doclens.j.processing.application;

import io.github.lvdaxianer.doclens.j.adapter.domain.ImageOcrRequest;
import io.github.lvdaxianer.doclens.j.adapter.domain.ImageOcrResult;
import io.github.lvdaxianer.doclens.j.adapter.domain.OcrAdapter;
import io.github.lvdaxianer.doclens.j.api.AdapterCapability;
import io.github.lvdaxianer.doclens.j.shared.application.TransactionRunner;
import io.github.lvdaxianer.doclens.j.storage.ObjectStorage;
import java.util.List;
import java.util.Map;
import java.util.concurrent.AbstractExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * 批次处理测试基础设施集合。
 *
 * @author lvdaxianerplus
 * @date 2026-06-11
 */
final class BatchProcessingUseCaseInfrastructure {

    /**
     * 禁止实例化测试基础设施集合。
     *
     * @author lvdaxianerplus
     * @date 2026-06-11
     */
    private BatchProcessingUseCaseInfrastructure() {
    }
}

/**
 * Stub OCR 适配器。
 *
 * @author lvdaxianerplus
 * @date 2026-06-08
 */
class StubAdapter implements OcrAdapter {

    /**
     * 返回测试适配器能力。
     *
     * @return 适配器能力
     * @author lvdaxianerplus
     * @date 2026-06-11
     */
    @Override
    public AdapterCapability capability() {
        return new AdapterCapability("stub_ocr", List.of("text"), false, false, true, false,
                false, null, null, 1, 1, "test");
    }

    /**
     * 返回空 OCR 块结果。
     *
     * @param request OCR 请求
     * @return OCR 结果
     * @author lvdaxianerplus
     * @date 2026-06-11
     */
    @Override
    public ImageOcrResult recognize(ImageOcrRequest request) {
        return ImageOcrResult.fromBlocks(request.pageNo(), Map.of(), List.of(), List.of());
    }
}

/**
 * 内存对象存储。
 *
 * @author lvdaxianerplus
 * @date 2026-06-08
 */
class InMemoryObjectStorage implements ObjectStorage {

    /**
     * 写入对象字节。
     *
     * @param objectKey 对象键
     * @param content 字节内容
     * @return 存储地址
     * @author lvdaxianerplus
     * @date 2026-06-11
     */
    @Override
    public String writeBytes(String objectKey, byte[] content) {
        return objectKey;
    }

    /**
     * 读取对象字节。
     *
     * @param storageUri 存储地址
     * @return 字节内容
     * @author lvdaxianerplus
     * @date 2026-06-11
     */
    @Override
    public byte[] readBytes(String storageUri) {
        return "text".getBytes();
    }
}

/**
 * 直接执行事务的测试事务器。
 *
 * @author lvdaxianerplus
 * @date 2026-06-08
 */
class InlineTransactionRunner implements TransactionRunner {

    /**
     * 在当前线程执行有返回值事务。
     *
     * @param action 事务动作
     * @return 动作返回值
     * @author lvdaxianerplus
     * @date 2026-06-11
     */
    @Override
    public <T> T requiredResult(java.util.function.Supplier<T> action) {
        return action.get();
    }

    /**
     * 在当前线程执行无返回值事务。
     *
     * @param action 事务动作
     * @author lvdaxianerplus
     * @date 2026-06-11
     */
    @Override
    public void requiredVoid(Runnable action) {
        action.run();
    }
}

/**
 * 直接执行任务的测试线程池，避免普通用例泄漏后台线程。
 *
 * @author lvdaxianerplus
 * @date 2026-06-11
 */
class InlineExecutorService extends AbstractExecutorService {

    private boolean shutdown;

    /**
     * 标记线程池关闭。
     *
     * @author lvdaxianerplus
     * @date 2026-06-11
     */
    @Override
    public void shutdown() {
        shutdown = true;
    }

    /**
     * 立即关闭线程池。
     *
     * @return 未执行任务列表
     * @author lvdaxianerplus
     * @date 2026-06-11
     */
    @Override
    public List<Runnable> shutdownNow() {
        shutdown = true;
        return List.of();
    }

    /**
     * 返回线程池是否关闭。
     *
     * @return 是否关闭
     * @author lvdaxianerplus
     * @date 2026-06-11
     */
    @Override
    public boolean isShutdown() {
        return shutdown;
    }

    /**
     * 返回线程池是否终止。
     *
     * @return 是否终止
     * @author lvdaxianerplus
     * @date 2026-06-11
     */
    @Override
    public boolean isTerminated() {
        return shutdown;
    }

    /**
     * 等待线程池终止。
     *
     * @param timeout 超时时间
     * @param unit 时间单位
     * @return 是否已终止
     * @author lvdaxianerplus
     * @date 2026-06-11
     */
    @Override
    public boolean awaitTermination(long timeout, TimeUnit unit) {
        return shutdown;
    }

    /**
     * 直接执行任务。
     *
     * @param command 待执行任务
     * @author lvdaxianerplus
     * @date 2026-06-11
     */
    @Override
    public void execute(Runnable command) {
        command.run();
    }
}
