package io.github.lvdaxianer.doclens.j.architecture;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Architecture boundary tests for DocLens core module.
 *
 * @author lvdaxianerplus
 * @date 2026-06-07
 */
class CoreDependencyBoundaryTest {

    private static final Path CORE_SOURCE_ROOT = Path.of("src/main/java");
    private static final List<String> FORBIDDEN_IMPORTS = List.of(
            "import org.springframework",
            "import jakarta.servlet",
            "import org.mybatis",
            "import com.baomidou"
    );

    /**
     * Verifies core module stays independent from Spring, Web, and MyBatis.
     *
     * @throws IOException when source files cannot be read
     * @author lvdaxianerplus
     * @date 2026-06-07
     */
    @Test
    void coreDoesNotDependOnSpringWebOrMybatis() throws IOException {
        List<Path> javaFiles = listJavaFiles();
        List<String> violations = javaFiles.stream()
                .flatMap(path -> forbiddenImports(path).stream())
                .toList();

        assertThat(violations).isEmpty();
    }

    private List<Path> listJavaFiles() throws IOException {
        try (var stream = Files.walk(CORE_SOURCE_ROOT)) {
            return stream.filter(path -> path.toString().endsWith(".java")).toList();
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
