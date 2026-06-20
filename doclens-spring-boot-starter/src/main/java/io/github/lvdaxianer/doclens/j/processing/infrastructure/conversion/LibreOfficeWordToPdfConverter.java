package io.github.lvdaxianer.doclens.j.processing.infrastructure.conversion;

import io.github.lvdaxianer.doclens.j.shared.config.DocLensProperties;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
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
    private static final String TEMP_DIR_PREFIX = "doclens-word-";
    private static final String PROFILE_DIR_NAME = "profile";
    private static final String DIAGNOSTIC_FILE_NAME = "libreoffice-output.log";
    private static final String USER_INSTALLATION_ARGUMENT_PREFIX = "-env:UserInstallation=";
    private static final String HEADLESS_ARGUMENT = "--headless";
    private static final String CONVERT_TO_ARGUMENT = "--convert-to";
    private static final String PDF_FORMAT = "pdf";
    private static final String OUTDIR_ARGUMENT = "--outdir";
    private static final String PDF_EXTENSION = ".pdf";
    private static final String DIAGNOSTIC_TRUNCATED_SUFFIX = "...";
    private static final String SAFE_FILE_NAME_PATTERN = "[/\\\\\\p{Cntrl}]";
    private static final String SAFE_FILE_NAME_REPLACEMENT = "_";
    private static final int MAX_DIAGNOSTIC_LENGTH = 500;

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
            Path diagnosticFile = tempDir.resolve(DIAGNOSTIC_FILE_NAME);
            Files.write(input, content);
            Process process = startConversionProcess(input, tempDir, diagnosticFile);
            String diagnostic = waitFor(process, Duration.ofSeconds(properties.wordConversion().timeoutSeconds()),
                    diagnosticFile);
            Path output = pdfOutputPath(tempDir, input, diagnostic);
            return Files.readAllBytes(output);
        } catch (IOException ex) {
            throw new IllegalStateException("Word to PDF conversion failed", ex);
        } finally {
            deleteTempDir(tempDir);
        }
    }

    /**
     * 启动 LibreOffice 转换进程。
     *
     * @param input 输入文件路径
     * @param tempDir 临时输出目录
     * @param diagnosticFile LibreOffice 输出文件
     * @return LibreOffice 转换进程
     * @throws IOException 进程启动失败
     * @author lvdaxianerplus
     * @date 2026-06-20
     */
    private Process startConversionProcess(Path input, Path tempDir, Path diagnosticFile) throws IOException {
        Path profileDir = tempDir.resolve(PROFILE_DIR_NAME);
        return new ProcessBuilder(properties.wordConversion().command(), userInstallationArgument(profileDir),
                HEADLESS_ARGUMENT, CONVERT_TO_ARGUMENT, PDF_FORMAT, OUTDIR_ARGUMENT, tempDir.toString(),
                input.toString())
                .redirectErrorStream(true)
                .redirectOutput(diagnosticFile.toFile())
                .start();
    }

    /**
     * 等待 LibreOffice 转换进程结束。
     *
     * @param process 转换进程
     * @param timeout 超时时间
     * @param diagnosticFile LibreOffice 输出文件
     * @return LibreOffice 进程输出
     * @author lvdaxianerplus
     * @date 2026-06-20
     */
    private String waitFor(Process process, Duration timeout, Path diagnosticFile) {
        try {
            waitUntilFinished(process, timeout, diagnosticFile);
            String output = processOutput(diagnosticFile);
            return checkedProcessOutput(process, output);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Word to PDF conversion interrupted", ex);
        }
    }

    /**
     * 等待 LibreOffice 进程完成，超时时终止进程。
     *
     * @param process 转换进程
     * @param timeout 超时时间
     * @param diagnosticFile LibreOffice 输出文件
     * @throws InterruptedException 当前线程被中断
     * @author lvdaxianerplus
     * @date 2026-06-20
     */
    private void waitUntilFinished(Process process, Duration timeout, Path diagnosticFile) throws InterruptedException {
        boolean finished = process.waitFor(timeout.toMillis(), TimeUnit.MILLISECONDS);
        // 进程超时时，先终止进程再读取已落盘的诊断输出。
        if (!finished) {
            process.destroyForcibly();
            process.waitFor();
            String output = processOutput(diagnosticFile);
            throw conversionException("Word to PDF conversion timed out", output);
        }
    }

    /**
     * 校验 LibreOffice 退出状态并返回诊断输出。
     *
     * @param process 转换进程
     * @param output LibreOffice 诊断输出
     * @return LibreOffice 诊断输出
     * @author lvdaxianerplus
     * @date 2026-06-20
     */
    private String checkedProcessOutput(Process process, String output) {
        // 进程异常退出时，将 LibreOffice 输出带回给调用方排查。
        if (process.exitValue() != 0) {
            throw conversionException("Word to PDF conversion failed", output);
        } else {
            // 进程成功退出时，返回诊断输出供后续缺少 PDF 产物场景使用。
            return output;
        }
    }

    /**
     * 定位转换后的 PDF 输出文件。
     *
     * @param tempDir 临时目录
     * @param input 输入文件路径
     * @param diagnostic LibreOffice 诊断输出
     * @return PDF 输出路径
     * @author lvdaxianerplus
     * @date 2026-06-08
     */
    private Path pdfOutputPath(Path tempDir, Path input, String diagnostic) {
        String inputName = input.getFileName().toString();
        int extensionIndex = inputName.lastIndexOf('.');
        String baseName;
        // 文件名带扩展名时，去掉原扩展名后拼接 PDF 扩展名。
        if (extensionIndex > 0) {
            baseName = inputName.substring(0, extensionIndex);
        } else {
            // 文件名无扩展名时，直接使用完整文件名作为输出基名。
            baseName = inputName;
        }
        Path output = tempDir.resolve(baseName + PDF_EXTENSION);
        // LibreOffice 已生成 PDF 时，返回产物路径。
        if (Files.exists(output)) {
            return output;
        } else {
            // LibreOffice 成功退出但缺少产物时，附带诊断输出抛出异常。
            throw conversionException("Word to PDF conversion did not produce PDF output", diagnostic);
        }
    }

    /**
     * 构建独立 LibreOffice 用户配置参数。
     *
     * @param profileDir 当前转换专属配置目录
     * @return LibreOffice UserInstallation 参数
     * @author lvdaxianerplus
     * @date 2026-06-20
     */
    private String userInstallationArgument(Path profileDir) {
        return USER_INSTALLATION_ARGUMENT_PREFIX + profileDir.toUri();
    }

    /**
     * 读取 LibreOffice 进程输出。
     *
     * @param diagnosticFile LibreOffice 输出文件
     * @return 标准输出和错误输出的合并文本
     * @author lvdaxianerplus
     * @date 2026-06-20
     */
    private String processOutput(Path diagnosticFile) {
        try {
            // 输出文件存在时，读取 LibreOffice stdout/stderr 合并结果。
            if (Files.exists(diagnosticFile)) {
                return Files.readString(diagnosticFile, StandardCharsets.UTF_8);
            } else {
                // 输出文件不存在时，返回空诊断，保留原异常分类。
                return "";
            }
        } catch (IOException ex) {
            LOGGER.warn("[Word转换] LibreOffice 输出读取失败 error={}", ex.getMessage(), ex);
            return "";
        }
    }

    /**
     * 创建带 LibreOffice 诊断信息的转换异常。
     *
     * @param message 转换失败分类
     * @param diagnostic LibreOffice 输出
     * @return 转换异常
     * @author lvdaxianerplus
     * @date 2026-06-20
     */
    private IllegalStateException conversionException(String message, String diagnostic) {
        String normalized = truncateDiagnostic(diagnostic);
        // 没有诊断输出时，保持简洁的原始异常消息。
        if (normalized.isBlank()) {
            return new IllegalStateException(message);
        } else {
            // 有诊断输出时，将原因摘要拼接到异常消息。
            return new IllegalStateException(message + ": " + normalized);
        }
    }

    /**
     * 截断 LibreOffice 诊断输出，避免错误消息过长。
     *
     * @param diagnostic 原始诊断输出
     * @return 规范化诊断输出
     * @author lvdaxianerplus
     * @date 2026-06-20
     */
    private String truncateDiagnostic(String diagnostic) {
        String normalized = diagnostic == null ? "" : diagnostic.strip();
        // 诊断输出未超过限制时，完整保留。
        if (normalized.length() <= MAX_DIAGNOSTIC_LENGTH) {
            return normalized;
        } else {
            // 诊断输出过长时，截断并追加省略标记。
            return normalized.substring(0, MAX_DIAGNOSTIC_LENGTH) + DIAGNOSTIC_TRUNCATED_SUFFIX;
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
            return Files.createTempDirectory(TEMP_DIR_PREFIX);
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
        // 原始文件名为空时，使用默认 Word 文件名保证转换输入稳定。
        if (fileName == null || fileName.isBlank()) {
            name = DEFAULT_WORD_FILE_NAME;
        } else {
            // 原始文件名可用时，保留调用方传入的文件名。
            name = fileName;
        }
        return name.replaceAll(SAFE_FILE_NAME_PATTERN, SAFE_FILE_NAME_REPLACEMENT);
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
