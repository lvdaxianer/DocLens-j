package io.github.lvdaxianer.doclens.j.processing.application;

import io.github.lvdaxianer.doclens.j.processing.domain.DocumentPageResult;
import io.github.lvdaxianer.doclens.j.processing.domain.DocumentPageResultRepository;
import io.github.lvdaxianer.doclens.j.shared.application.TransactionRunner;
import io.github.lvdaxianer.doclens.j.storage.ObjectStorage;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * 文档页任务执行测试替身集合。
 *
 * @author lvdaxianerplus
 * @date 2026-06-11
 */
final class DocumentPageTaskExecutionTestDoubles {

    /**
     * 禁止实例化测试替身工具类。
     *
     * @author lvdaxianerplus
     * @date 2026-06-11
     */
    private DocumentPageTaskExecutionTestDoubles() {
    }

    /**
     * 记录读取内容的对象存储。
     *
     * @author lvdaxianerplus
     * @date 2026-06-11
     */
    static class RecordingObjectStorage implements ObjectStorage {

        /**
         * 当前测试不写入对象存储。
         *
         * @param objectKey 对象键
         * @param content 待存储字节
         * @return 存储 URI
         * @author lvdaxianerplus
         * @date 2026-06-11
         */
        @Override
        public String writeBytes(String objectKey, byte[] content) {
            return objectKey;
        }

        /**
         * 返回存储 URI 对应的测试字节。
         *
         * @param storageUri 存储 URI
         * @return 图片字节
         * @author lvdaxianerplus
         * @date 2026-06-11
         */
        @Override
        public byte[] readBytes(String storageUri) {
            return storageUri.getBytes();
        }
    }

    /**
     * 内存页结果仓储。
     *
     * @author lvdaxianerplus
     * @date 2026-06-11
     */
    static class InMemoryDocumentPageResultRepository implements DocumentPageResultRepository {

        private final Map<String, DocumentPageResult> results = new HashMap<>(4);

        /**
         * 新增或更新页结果。
         *
         * @param result 页结果
         * @author lvdaxianerplus
         * @date 2026-06-11
         */
        @Override
        public void upsert(DocumentPageResult result) {
            results.put(key(result.documentId(), result.pageNo()), result);
        }

        /**
         * 根据文档 ID 和页码查询页结果。
         *
         * @param documentId 文档 ID
         * @param pageNo 页码
         * @return 页结果
         * @author lvdaxianerplus
         * @date 2026-06-11
         */
        @Override
        public Optional<DocumentPageResult> findByDocumentIdAndPageNo(String documentId, int pageNo) {
            return Optional.ofNullable(results.get(key(documentId, pageNo)));
        }

        /**
         * 查询文档全部页结果。
         *
         * @param documentId 文档 ID
         * @return 页结果列表
         * @author lvdaxianerplus
         * @date 2026-06-11
         */
        @Override
        public List<DocumentPageResult> listByDocumentId(String documentId) {
            return results.values().stream().filter(result -> documentId.equals(result.documentId())).toList();
        }

        /**
         * 按文档集合批量查询页结果。
         *
         * @param documentIds 文档 ID 集合
         * @return 页结果列表
         * @author lvdaxianerplus
         * @date 2026-06-11
         */
        @Override
        public List<DocumentPageResult> listByDocumentIds(List<String> documentIds) {
            return results.values().stream().filter(result -> documentIds.contains(result.documentId())).toList();
        }

        /**
         * 生成页结果唯一键。
         *
         * @param documentId 文档 ID
         * @param pageNo 页码
         * @return 结果键
         * @author lvdaxianerplus
         * @date 2026-06-11
         */
        private String key(String documentId, int pageNo) {
            return documentId + "#" + pageNo;
        }
    }

    /**
     * 直接执行事务的测试事务器。
     *
     * @author lvdaxianerplus
     * @date 2026-06-11
     */
    static class InlineTransactionRunner implements TransactionRunner {

        /**
         * 直接执行并返回结果。
         *
         * @param action 事务动作
         * @param <T> 返回类型
         * @return 动作结果
         * @author lvdaxianerplus
         * @date 2026-06-11
         */
        @Override
        public <T> T requiredResult(Supplier<T> action) {
            return action.get();
        }

        /**
         * 直接执行无返回动作。
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

}
