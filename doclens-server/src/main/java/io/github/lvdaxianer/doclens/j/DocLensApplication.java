package io.github.lvdaxianer.doclens.j;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * DocLens OCR 服务应用入口。
 *
 * @author lvdaxianerplus
 * @date 2026-06-07
 */
@EnableAsync
@EnableScheduling
@SpringBootApplication
public class DocLensApplication {

    /**
     * 启动 DocLens OCR 服务。
     *
     * @param args 命令行参数
     * @author lvdaxianerplus
     * @date 2026-06-07
     */
    public static void main(String[] args) {
        SpringApplication.run(DocLensApplication.class, args);
    }
}
