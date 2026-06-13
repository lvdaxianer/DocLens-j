package io.github.lvdaxianer.doclens.j.processing.infrastructure.extraction;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.lvdaxianer.doclens.j.processing.application.PageImageRef;
import io.github.lvdaxianer.doclens.j.processing.domain.DocumentJob;
import io.github.lvdaxianer.doclens.j.processing.domain.DocumentJobCreateRequest;
import io.github.lvdaxianer.doclens.j.processing.domain.DocumentType;
import io.github.lvdaxianer.doclens.j.shared.domain.JsonPayload;
import io.github.lvdaxianer.doclens.j.storage.ObjectStorage;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/**
 * 默认页图片准备器测试。
 *
 * @author lvdaxianerplus
 * @date 2026-06-10
 */
class DefaultPageImagePreparationTest {

    /**
     * 图片文档已经是页图片，应直接复用存储 URI，避免入队阶段多一次读存储。
     *
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    @Test
    void prepareImageReusesOriginalStorageUriWithoutReadingObjectStorage() {
        DefaultPageImagePreparation preparation = new DefaultPageImagePreparation(
                new FailingReadObjectStorage(), null, null);

        List<PageImageRef> pageImages = preparation.prepare(document());

        assertThat(pageImages).containsExactly(new PageImageRef(1, "local://doc-image"));
    }

    /**
     * 创建图片文档任务。
     *
     * @return 图片文档任务
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    private DocumentJob document() {
        DocumentJobCreateRequest request = new DocumentJobCreateRequest("doc-image", "batch-test", "image.png",
                DocumentType.IMAGE, 1, 1, "local://doc-image", "stub_ocr", Optional.empty(), JsonPayload.empty(), 0,
                OffsetDateTime.now());
        return DocumentJob.create(request);
    }

    /**
     * 读取即失败的对象存储，用于证明图片入队不依赖读取原始内容。
     *
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    private static class FailingReadObjectStorage implements ObjectStorage {

        /**
         * 当前测试不写入对象存储。
         *
         * @param objectKey 对象键
         * @param content 待存储字节
         * @return 存储 URI
         * @author lvdaxianerplus
         * @date 2026-06-10
         */
        @Override
        public String writeBytes(String objectKey, byte[] content) {
            return objectKey;
        }

        /**
         * 图片入队不应读取对象存储。
         *
         * @param storageUri 存储 URI
         * @return 不返回
         * @author lvdaxianerplus
         * @date 2026-06-10
         */
        @Override
        public byte[] readBytes(String storageUri) {
            throw new AssertionError("image preparation must not read object storage");
        }
    }
}
