package io.github.lvdaxianer.doclens.j.processing.infrastructure.conversion;

import io.github.lvdaxianer.doclens.j.shared.config.DocLensProperties;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Comparator;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 基于 LibreOffice headless 命令的 Word 转 PDF 转换器。
 *
 * @author lvdaxianerplus
 * @date 2026-06-08
 */
public class LibreOfficeWordToPdfConverter implements WordToPdfConverter {

    private static final Logger LOGGER = LoggerFactory.getLogger(LibreOfficeWordToPdfConverter.class);
    private static final String DEFAULT_WORD_FILE_NAME = "document.docx";

    private final DocLensProperties properties;

    /**
     * 创建 Word 转 PDF 转换器。
     *
     * @param properties DocLens 配置
     * @author lvdaxianerplus
     * @date 2026-06-08
     */
    public LibreOfficeWordToPdfConverter(DocLensProperties properties) {
        this.properties = properties;
    }

    /**
     * 执行 Word 转 PDF。
     *
     * @param fileName 原始文件名
     * @param content Word 文件字节
     * @return PDF 文件字节
     * @author lvdaxianerplus
     * @date 2026-06-08
     */
    @Override
    public byte[] convert(String fileName, byte[] content) {
        Path tempDir = createTempDir();
        try {
            Path input = tempDir.resolve(safeFileName(fileName));
            Files.write(input, content);
            Process process = new ProcessBuilder(properties.wordConversion().command(), "--headless", "--convert-to",
                    "pdf", "--outdir", tempDir.toString(), input.toString())
                    .redirectErrorStream(true)
                    .start();
            waitFor(process, Duration.ofSeconds(properties.wordConversion().timeoutSeconds()));
            Path output = pdfOutputPath(tempDir, input);
            return Files.readAllBytes(output);
        } catch (IOException ex) {
            throw new IllegalStateException("Word to PDF conversion failed", ex);
        } finally {
            deleteTempDir(tempDir);
        }
    }

    /**
     * 等待 LibreOffice 转换进程结束。
     *
     * @param process 转换进程
     * @param timeout 超时时间
     * @author lvdaxianerplus
     * @date 2026-06-08
     */
    private void waitFor(Process process, Duration timeout) {
        try {
            boolean finished = process.waitFor(timeout.toMillis(), TimeUnit.MILLISECONDS);
            if (!finished) {
                process.destroyForcibly();
                throw new IllegalStateException("Word to PDF conversion timed out");
            } else if (process.exitValue() != 0) {
                throw new IllegalStateException("Word to PDF conversion failed");
            } else {
                // 转换成功。
            }
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Word to PDF conversion interrupted", ex);
        }
    }

    /**
     * 定位转换后的 PDF 输出文件。
     *
     * @param tempDir 临时目录
     * @param input 输入文件路径
     * @return PDF 输出路径
     * @author lvdaxianerplus
     * @date 2026-06-08
     */
    private Path pdfOutputPath(Path tempDir, Path input) {
        String inputName = input.getFileName().toString();
        int extensionIndex = inputName.lastIndexOf('.');
        String baseName;
        if (extensionIndex > 0) {
            baseName = inputName.substring(0, extensionIndex);
        } else {
            baseName = inputName;
        }
        Path output = tempDir.resolve(baseName + ".pdf");
        if (Files.exists(output)) {
            return output;
        } else {
            throw new IllegalStateException("Word to PDF conversion did not produce PDF output");
        }
    }

    /**
     * 创建 Word 转换临时目录。
     *
     * @return 临时目录路径
     * @author lvdaxianerplus
     * @date 2026-06-08
     */
    private Path createTempDir() {
        try {
            return Files.createTempDirectory("doclens-word-");
        } catch (IOException ex) {
            throw new IllegalStateException("failed to create Word conversion temp directory", ex);
        }
    }

    /**
     * 清理原始文件名中的路径和控制字符。
     *
     * @param fileName 原始文件名
     * @return 安全文件名
     * @author lvdaxianerplus
     * @date 2026-06-08
     */
    private String safeFileName(String fileName) {
        String name;
        if (fileName == null || fileName.isBlank()) {
            name = DEFAULT_WORD_FILE_NAME;
        } else {
            name = fileName;
        }
        return name.replaceAll("[/\\\\\\p{Cntrl}]", "_");
    }

    /**
     * 删除 Word 转换临时目录。
     *
     * @param tempDir 临时目录
     * @author lvdaxianerplus
     * @date 2026-06-08
     */
    private void deleteTempDir(Path tempDir) {
        try (var stream = Files.walk(tempDir)) {
            stream.sorted(Comparator.reverseOrder()).forEach(path -> {
                try {
                    Files.deleteIfExists(path);
                } catch (IOException ex) {
                    LOGGER.warn("[Word转换] 临时文件清理失败 path={}, error={}", path, ex.getMessage(), ex);
                }
            });
        } catch (IOException ex) {
            LOGGER.warn("[Word转换] 临时目录遍历失败 path={}, error={}", tempDir, ex.getMessage(), ex);
        }
    }
}
