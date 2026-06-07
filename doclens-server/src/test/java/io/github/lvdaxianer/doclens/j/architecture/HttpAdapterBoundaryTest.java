package io.github.lvdaxianer.doclens.j.architecture;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Architecture boundary tests for HTTP adapter module.
 *
 * @author lvdaxianerplus
 * @date 2026-06-07
 */
class HttpAdapterBoundaryTest {

    private static final Path CONTROLLER_SOURCE_ROOT = Path.of("src/main/java/io/github/lvdaxianer/doclens/j");
    private static final List<String> FORBIDDEN_IMPORTS = List.of(
            "ingestion.application.CreateBatchUseCase",
            "query.application.OcrQueryService",
            "adapter.domain.DefaultAdapterRegistry",
            "ingestion.domain.",
            "processing.domain.",
            "processing.application."
    );

    /**
     * Verifies HTTP controllers enter business capabilities through DocLensEngine.
     *
     * @throws IOException when source files cannot be read
     * @author lvdaxianerplus
     * @date 2026-06-07
     */
    @Test
    void controllersDoNotDependOnInternalApplicationOrDomainTypes() throws IOException {
        List<String> violations = controllerFiles().stream()
                .flatMap(path -> forbiddenImports(path).stream())
                .toList();

        assertThat(violations).isEmpty();
    }

    private List<Path> controllerFiles() throws IOException {
        try (var stream = Files.walk(CONTROLLER_SOURCE_ROOT)) {
            return stream.filter(path -> path.toString().endsWith("Controller.java")).toList();
        }
    }

    private List<String> forbiddenImports(Path path) {
        try {
            String content = Files.readString(path);
            return FORBIDDEN_IMPORTS.stream()
                    .filter(content::contains)
                    .map(token -> path + " contains " + token)
                    .toList();
        } catch (IOException ex) {
            throw new IllegalStateException("failed to read source file " + path, ex);
        }
    }
}
