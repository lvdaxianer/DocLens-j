package io.github.lvdaxianer.doclens.j.processing.infrastructure.conversion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.lvdaxianer.doclens.j.shared.config.DocLensProperties;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * LibreOffice Word 转 PDF 转换器测试。
 *
 * @author lvdaxianerplus
 * @date 2026-06-20
 */
class LibreOfficeWordToPdfConverterTest {

    private static final int SUCCESS_EXIT_CODE = 0;
    private static final int FAILURE_EXIT_CODE = 7;
    private static final int TEST_CALLBACK_MAX_ATTEMPTS = 1;
    private static final int TEST_CALLBACK_TIMEOUT_SECONDS = 5;
    private static final int TEST_PADDLE_TIMEOUT_SECONDS = 5;
    private static final int TEST_HEALTH_FAILURE_THRESHOLD = 3;
    private static final int TEST_HEALTH_CHECK_INTERVAL_SECONDS = 2;
    private static final int TEST_EXTRACTION_MAX_PAGES = 2;
    private static final int TEST_PDF_DPI = 36;
    private static final int TEST_WORD_TIMEOUT_SECONDS = 5;
    private static final int TEST_THREAD_POOL_CORE_SIZE = 1;
    private static final int TEST_THREAD_POOL_MAX_SIZE = 1;
    private static final int TEST_THREAD_POOL_QUEUE_CAPACITY = 1;
    private static final int TEST_THREAD_POOL_KEEP_ALIVE_SECONDS = 1;

    @TempDir
    private Path tempDir;

    /**
     * 每次转换都必须使用独立的 LibreOffice UserInstallation profile。
     *
     * @author lvdaxianerplus
     * @date 2026-06-20
     */
    @Test
    void convertUsesIsolatedLibreOfficeUserInstallation() throws IOException {
        Path argsFile = tempDir.resolve("args.txt");
        Path command = successCommand(argsFile);
        LibreOfficeWordToPdfConverter converter = new LibreOfficeWordToPdfConverter(properties(command));

        byte[] pdf = converter.convert("demo.docx", "word-content".getBytes(StandardCharsets.UTF_8));

        assertThat(new String(pdf, StandardCharsets.UTF_8)).isEqualTo("pdf-content");
        assertThat(Files.readString(argsFile))
                .contains("--headless")
                .contains("--convert-to")
                .contains("pdf")
                .containsPattern("-env:UserInstallation=file://.*/profile");
    }

    /**
     * 转换失败时需要保留 LibreOffice 进程输出，方便定位批量失败原因。
     *
     * @author lvdaxianerplus
     * @date 2026-06-20
     */
    @Test
    void convertIncludesProcessOutputWhenLibreOfficeFails() throws IOException {
        Path command = failingCommand("profile is already locked");
        LibreOfficeWordToPdfConverter converter = new LibreOfficeWordToPdfConverter(properties(command));

        assertThatThrownBy(() -> converter.convert("demo.docx", "word-content".getBytes(StandardCharsets.UTF_8)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Word to PDF conversion failed")
                .hasMessageContaining("profile is already locked");
    }

    /**
     * LibreOffice 成功退出但没有产物时，也要保留诊断输出。
     *
     * @author lvdaxianerplus
     * @date 2026-06-20
     */
    @Test
    void convertIncludesProcessOutputWhenPdfOutputIsMissing() throws IOException {
        Path command = missingOutputCommand("source file could not be loaded");
        LibreOfficeWordToPdfConverter converter = new LibreOfficeWordToPdfConverter(properties(command));

        assertThatThrownBy(() -> converter.convert("demo.docx", "word-content".getBytes(StandardCharsets.UTF_8)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Word to PDF conversion did not produce PDF output")
                .hasMessageContaining("source file could not be loaded");
    }

    /**
     * 创建能模拟成功转换的命令脚本。
     *
     * @param argsFile 参数记录文件
     * @return 命令脚本路径
     * @throws IOException 文件写入失败
     * @author lvdaxianerplus
     * @date 2026-06-20
     */
    private Path successCommand(Path argsFile) throws IOException {
        Path command = tempDir.resolve("success-soffice.sh");
        Files.writeString(command, """
                #!/bin/sh
                printf '%%s\\n' "$*" > "%s"
                while [ "$#" -gt 0 ]; do
                  if [ "$1" = "--outdir" ]; then
                    shift
                    outdir="$1"
                  fi
                  input="$1"
                  shift
                done
                base="$(basename "$input")"
                name="${base%%.*}"
                printf 'pdf-content' > "$outdir/$name.pdf"
                """.formatted(argsFile), StandardCharsets.UTF_8);
        command.toFile().setExecutable(true);
        return command;
    }

    /**
     * 创建模拟 LibreOffice 转换失败的命令脚本。
     *
     * @param diagnostic 诊断输出
     * @return 命令脚本路径
     * @throws IOException 文件写入失败
     * @author lvdaxianerplus
     * @date 2026-06-20
     */
    private Path failingCommand(String diagnostic) throws IOException {
        Path command = tempDir.resolve("failing-soffice.sh");
        Files.writeString(command, """
                #!/bin/sh
                printf '%%s\\n' "%s"
                exit %d
                """.formatted(diagnostic, FAILURE_EXIT_CODE), StandardCharsets.UTF_8);
        command.toFile().setExecutable(true);
        return command;
    }

    /**
     * 创建模拟 LibreOffice 未生成 PDF 但正常退出的命令脚本。
     *
     * @param diagnostic 诊断输出
     * @return 命令脚本路径
     * @throws IOException 文件写入失败
     * @author lvdaxianerplus
     * @date 2026-06-20
     */
    private Path missingOutputCommand(String diagnostic) throws IOException {
        Path command = tempDir.resolve("missing-output-soffice.sh");
        Files.writeString(command, """
                #!/bin/sh
                printf '%%s\\n' "%s"
                exit %d
                """.formatted(diagnostic, SUCCESS_EXIT_CODE), StandardCharsets.UTF_8);
        command.toFile().setExecutable(true);
        return command;
    }

    /**
     * 创建测试配置。
     *
     * @param command LibreOffice 命令路径
     * @return DocLens 配置
     * @author lvdaxianerplus
     * @date 2026-06-20
     */
    private DocLensProperties properties(Path command) {
        return new DocLensProperties("target/test-storage", true, "worker-test",
                new DocLensProperties.CallbackProperties(TEST_CALLBACK_MAX_ATTEMPTS, TEST_CALLBACK_TIMEOUT_SECONDS),
                new DocLensProperties.AdapterProperties("stub_ocr"),
                new DocLensProperties.PaddleOcrProperties(false, "http://127.0.0.1:8080/ocr",
                        TEST_PADDLE_TIMEOUT_SECONDS, false),
                new DocLensProperties.OcrHealthProperties(TEST_HEALTH_FAILURE_THRESHOLD,
                        TEST_HEALTH_CHECK_INTERVAL_SECONDS),
                new DocLensProperties.ExtractionProperties(TEST_EXTRACTION_MAX_PAGES),
                new DocLensProperties.PdfRenderProperties(TEST_PDF_DPI, "png"),
                new DocLensProperties.WordConversionProperties(command.toString(), TEST_WORD_TIMEOUT_SECONDS),
                threadPools());
    }

    /**
     * 创建测试线程池配置。
     *
     * @return 线程池配置
     * @author lvdaxianerplus
     * @date 2026-06-20
     */
    private DocLensProperties.ThreadPoolsProperties threadPools() {
        DocLensProperties.ThreadPoolProperties pool = new DocLensProperties.ThreadPoolProperties(
                TEST_THREAD_POOL_CORE_SIZE, TEST_THREAD_POOL_MAX_SIZE, TEST_THREAD_POOL_QUEUE_CAPACITY,
                TEST_THREAD_POOL_KEEP_ALIVE_SECONDS, "doclens-test-");
        return new DocLensProperties.ThreadPoolsProperties(pool, pool, pool, pool);
    }
}
