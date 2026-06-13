package io.github.lvdaxianer.doclens.j.processing.infrastructure.extraction;

import io.github.lvdaxianer.doclens.j.processing.application.PageImagePreparation;
import io.github.lvdaxianer.doclens.j.processing.application.PageImageRef;
import io.github.lvdaxianer.doclens.j.processing.domain.DocumentJob;
import io.github.lvdaxianer.doclens.j.processing.domain.DocumentType;
import io.github.lvdaxianer.doclens.j.processing.infrastructure.conversion.PdfPageImageRenderer;
import io.github.lvdaxianer.doclens.j.processing.infrastructure.conversion.RenderedPageImage;
import io.github.lvdaxianer.doclens.j.processing.infrastructure.conversion.WordToPdfConverter;
import io.github.lvdaxianer.doclens.j.shared.domain.DocLensConstants;
import io.github.lvdaxianer.doclens.j.storage.ObjectStorage;
import java.util.List;

/**
 * 默认文档页图片准备器。
 *
 * @author lvdaxianerplus
 * @date 2026-06-10
 */
public class DefaultPageImagePreparation implements PageImagePreparation {

    private static final String PAGE_IMAGE_OBJECT_KEY = "pages/%s/%s/page-%d.png";

    private final ObjectStorage objectStorage;
    private final PdfPageImageRenderer pdfPageImageRenderer;
    private final WordToPdfConverter wordToPdfConverter;

    /**
     * 创建默认文档页图片准备器。
     *
     * @param objectStorage 对象存储
     * @param pdfPageImageRenderer PDF 页图片渲染器
     * @param wordToPdfConverter Word 转 PDF 转换器
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    public DefaultPageImagePreparation(
            ObjectStorage objectStorage,
            PdfPageImageRenderer pdfPageImageRenderer,
            WordToPdfConverter wordToPdfConverter
    ) {
        this.objectStorage = objectStorage;
        this.pdfPageImageRenderer = pdfPageImageRenderer;
        this.wordToPdfConverter = wordToPdfConverter;
    }

    /**
     * 将文档准备为页图片引用。
     *
     * @param document 文档任务
     * @return 页图片引用集合
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    @Override
    public List<PageImageRef> prepare(DocumentJob document) {
        if (document.fileType() == DocumentType.IMAGE) {
            return List.of(new PageImageRef(DocLensConstants.DEFAULT_PAGE_NO, document.storageUri()));
        } else {
            return renderedPageRefs(document);
        }
    }

    /**
     * 渲染 PDF 或 Word 文档并持久化页图片。
     *
     * @param document 文档任务
     * @return 页图片引用集合
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    private List<PageImageRef> renderedPageRefs(DocumentJob document) {
        byte[] content = objectStorage.readBytes(document.storageUri());
        byte[] pdfContent = pdfContent(document, content);
        return pdfPageImageRenderer.render(pdfContent).stream()
                .map(pageImage -> pageImageRef(document, pageImage))
                .toList();
    }

    /**
     * 将原始文档转换为 PDF 内容。
     *
     * @param document 文档任务
     * @param content 原始文档内容
     * @return PDF 字节
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    private byte[] pdfContent(DocumentJob document, byte[] content) {
        if (document.fileType() == DocumentType.WORD) {
            return wordToPdfConverter.convert(document.fileName(), content);
        } else {
            return content;
        }
    }

    /**
     * 持久化单页图片并返回引用。
     *
     * @param document 文档任务
     * @param pageImage 渲染后的页图片
     * @return 页图片引用
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    private PageImageRef pageImageRef(DocumentJob document, RenderedPageImage pageImage) {
        String objectKey = PAGE_IMAGE_OBJECT_KEY.formatted(document.batchId(), document.documentId(),
                pageImage.pageNo());
        return new PageImageRef(pageImage.pageNo(), objectStorage.writeBytes(objectKey, pageImage.content()));
    }
}
