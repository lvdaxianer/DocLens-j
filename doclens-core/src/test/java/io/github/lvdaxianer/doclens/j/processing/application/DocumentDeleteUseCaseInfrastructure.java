package io.github.lvdaxianer.doclens.j.processing.application;

import io.github.lvdaxianer.doclens.j.shared.application.TransactionRunner;
import io.github.lvdaxianer.doclens.j.storage.ObjectStorage;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * 文档删除用例测试基础设施桩。
 *
 * @author lvdaxianerplus
 * @date 2026-06-11
 */
final class DocumentDeleteUseCaseInfrastructure {

    /** 测试集合初始容量。 */
    private static final int TEST_CAPACITY = 8;

    /**
     * 禁止实例化工具类。
     *
     * @author lvdaxianerplus
     * @date 2026-06-11
     */
    private DocumentDeleteUseCaseInfrastructure() {
    }

    /**
     * 记录删除请求的对象存储桩。
     *
     * @author lvdaxianerplus
     * @date 2026-06-11
     */
    static final class RecordingObjectStorage implements ObjectStorage {

        final List<String> deletedUris = new ArrayList<>(TEST_CAPACITY);

        /**
         * 写入字节内容。
         *
         * @param objectKey 对象键
         * @param content 字节内容
         * @return 存储地址
         * @author lvdaxianerplus
         * @date 2026-06-11
         */
        @Override
        public String writeBytes(String objectKey, byte[] content) {
            return "local://" + objectKey;
        }

        /**
         * 读取字节内容。
         *
         * @param storageUri 存储地址
         * @return 字节内容
         * @author lvdaxianerplus
         * @date 2026-06-11
         */
        @Override
        public byte[] readBytes(String storageUri) {
            return new byte[0];
        }

        /**
         * 删除单个对象。
         *
         * @param storageUri 存储地址
         * @author lvdaxianerplus
         * @date 2026-06-11
         */
        @Override
        public void delete(String storageUri) {
            deletedUris.add(storageUri);
        }

        /**
         * 批量删除对象。
         *
         * @param storageUris 存储地址列表
         * @author lvdaxianerplus
         * @date 2026-06-11
         */
        @Override
        public void deleteAll(List<String> storageUris) {
            deletedUris.addAll(storageUris);
        }
    }

    /**
     * 记录 checkpoint 删除请求的存储桩。
     *
     * @author lvdaxianerplus
     * @date 2026-06-20
     */
    static final class RecordingCheckpointStore implements MarkdownChunkCheckpointStore {

        final List<String> deletedDocumentIds = new ArrayList<>(TEST_CAPACITY);

        /**
         * 忽略 checkpoint 保存。
         *
         * @param checkpoint chunk checkpoint 内容
         * @author lvdaxianerplus
         * @date 2026-06-20
         */
        @Override
        public void save(MarkdownChunkCheckpoint checkpoint) {
        }

        /**
         * 测试桩不提供可复用 checkpoint。
         *
         * @param plan checkpoint 计划
         * @param chunk Markdown chunk
         * @return 空 checkpoint
         * @author lvdaxianerplus
         * @date 2026-06-20
         */
        @Override
        public Optional<String> load(MarkdownChunkCheckpointPlan plan, MarkdownChunk chunk) {
            return Optional.empty();
        }

        /**
         * 记录被请求清理的文档 checkpoint。
         *
         * @param documentId 文档 ID
         * @author lvdaxianerplus
         * @date 2026-06-20
         */
        @Override
        public void deleteByDocumentId(String documentId) {
            deletedDocumentIds.add(documentId);
        }
    }

    /**
     * 直接执行事务的测试事务器。
     *
     * @author lvdaxianerplus
     * @date 2026-06-11
     */
    static final class InlineTransactionRunner implements TransactionRunner {

        /**
         * 在事务中执行无返回值动作。
         *
         * @param action 事务动作
         * @author lvdaxianerplus
         * @date 2026-06-11
         */
        @Override
        public void requiredVoid(Runnable action) {
            action.run();
        }

        /**
         * 在事务中执行有返回值动作。
         *
         * @param action 事务动作
         * @param <T> 返回类型
         * @return 执行结果
         * @author lvdaxianerplus
         * @date 2026-06-11
         */
        @Override
        public <T> T requiredResult(java.util.function.Supplier<T> action) {
            return action.get();
        }
    }
}
