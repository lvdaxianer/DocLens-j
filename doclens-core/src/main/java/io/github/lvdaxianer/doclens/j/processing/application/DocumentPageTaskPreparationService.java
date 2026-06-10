package io.github.lvdaxianer.doclens.j.processing.application;

import io.github.lvdaxianer.doclens.j.processing.domain.DocumentJob;
import io.github.lvdaxianer.doclens.j.processing.domain.DocumentJobRepository;
import io.github.lvdaxianer.doclens.j.processing.domain.DocumentPageTask;
import io.github.lvdaxianer.doclens.j.processing.domain.DocumentPageTaskCreateRequest;
import io.github.lvdaxianer.doclens.j.processing.domain.DocumentPageTaskRepository;
import io.github.lvdaxianer.doclens.j.shared.infrastructure.IdGenerator;
import java.time.OffsetDateTime;
import java.util.Comparator;
import java.util.List;

/**
 * 文档页任务预处理服务。
 *
 * @author lvdaxianerplus
 * @date 2026-06-10
 */
public class DocumentPageTaskPreparationService {

    private final DocumentJobRepository documentRepository;
    private final DocumentPageTaskRepository pageTaskRepository;
    private final PageImagePreparation pageImagePreparation;
    private final IdGenerator idGenerator;

    /**
     * 创建文档页任务预处理服务。
     *
     * @param documentRepository 文档仓储
     * @param pageTaskRepository 页任务仓储
     * @param pageImagePreparation 页图片准备器
     * @param idGenerator ID 生成器
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    public DocumentPageTaskPreparationService(
            DocumentJobRepository documentRepository,
            DocumentPageTaskRepository pageTaskRepository,
            PageImagePreparation pageImagePreparation,
            IdGenerator idGenerator
    ) {
        this.documentRepository = documentRepository;
        this.pageTaskRepository = pageTaskRepository;
        this.pageImagePreparation = pageImagePreparation;
        this.idGenerator = idGenerator;
    }

    /**
     * 准备文档页任务并进入 OCR 等待状态。
     *
     * @param document 文档任务
     * @return 文档页图片准备结果
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    public PreparedDocumentPages prepare(DocumentJob document) {
        OffsetDateTime now = OffsetDateTime.now();
        List<PageImageRef> pageImages = orderedPageImages(document);
        pageTaskRepository.saveAll(tasks(document, pageImages, now));
        documentRepository.update(document.markOcrQueued(pageImages.size(), now));
        return new PreparedDocumentPages(document.batchId(), document.documentId(), document.fileName(), pageImages);
    }

    /**
     * 准备并按页码排序页图片引用。
     *
     * @param document 文档任务
     * @return 有序页图片引用
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    private List<PageImageRef> orderedPageImages(DocumentJob document) {
        return pageImagePreparation.prepare(document).stream()
                .sorted(Comparator.comparingInt(PageImageRef::pageNo))
                .toList();
    }

    /**
     * 构建等待调度的页任务集合。
     *
     * @param document 文档任务
     * @param pageImages 有序页图片引用
     * @param now 当前时间
     * @return 页任务集合
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    private List<DocumentPageTask> tasks(DocumentJob document, List<PageImageRef> pageImages, OffsetDateTime now) {
        return pageImages.stream().map(pageImage -> task(document, pageImage, now)).toList();
    }

    /**
     * 构建单页 OCR 任务。
     *
     * @param document 文档任务
     * @param pageImage 页图片引用
     * @param now 当前时间
     * @return 单页 OCR 任务
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    private DocumentPageTask task(DocumentJob document, PageImageRef pageImage, OffsetDateTime now) {
        return DocumentPageTask.create(new DocumentPageTaskCreateRequest(idGenerator.newPageTaskId(),
                document.batchId(), document.documentId(), pageImage.pageNo(), pageImage.imageStorageUri(), now));
    }
}
