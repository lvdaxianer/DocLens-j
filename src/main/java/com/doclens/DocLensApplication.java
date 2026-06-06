package com.doclens;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * DocLens OCR service application entrypoint.
 *
 * @author lvdaxianerplus
 * @date 2026-06-07
 */
@EnableAsync
@EnableScheduling
@MapperScan("com.doclens.**.infrastructure")
@SpringBootApplication
@ConfigurationPropertiesScan
public class DocLensApplication {

    /**
     * Starts the DocLens OCR service.
     *
     * @param args command line arguments
     * @author lvdaxianerplus
     * @date 2026-06-07
     */
    public static void main(String[] args) {
        SpringApplication.run(DocLensApplication.class, args);
    }
}
