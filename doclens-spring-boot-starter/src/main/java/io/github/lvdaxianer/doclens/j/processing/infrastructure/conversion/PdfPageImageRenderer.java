package io.github.lvdaxianer.doclens.j.processing.infrastructure.conversion;

import io.github.lvdaxianer.doclens.j.shared.config.DocLensProperties;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import javax.imageio.ImageIO;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;

/**
 * 使用 PDFBox 将 PDF 页渲染为图片。
 *
 * @author lvdaxianerplus
 * @date 2026-06-08
 */
public class PdfPageImageRenderer {

    private final DocLensProperties properties;

    /**
     * 创建 PDF 页图片渲染器。
     *
     * @param properties DocLens 配置
     * @author lvdaxianerplus
     * @date 2026-06-08
     */
    public PdfPageImageRenderer(DocLensProperties properties) {
        this.properties = properties;
    }

    /**
     * 将 PDF 渲染为按页排序的图片。
     *
     * @param pdfContent PDF 字节
     * @return 页图片集合
     * @author lvdaxianerplus
     * @date 2026-06-08
     */
    public List<RenderedPageImage> render(byte[] pdfContent) {
        try (PDDocument document = Loader.loadPDF(pdfContent)) {
            PDFRenderer renderer = new PDFRenderer(document);
            List<RenderedPageImage> images = new ArrayList<>(document.getNumberOfPages());
            for (int index = 0; index < document.getNumberOfPages(); index++) {
                BufferedImage image = renderer.renderImageWithDPI(index, properties.pdfRender().dpi());
                images.add(new RenderedPageImage(index + 1, encode(image)));
            }
            return images;
        } catch (IOException ex) {
            throw new IllegalStateException("failed to render PDF pages", ex);
        }
    }

    private byte[] encode(BufferedImage image) throws IOException {
        try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            ImageIO.write(image, properties.pdfRender().imageFormat(), output);
            return output.toByteArray();
        }
    }
}
