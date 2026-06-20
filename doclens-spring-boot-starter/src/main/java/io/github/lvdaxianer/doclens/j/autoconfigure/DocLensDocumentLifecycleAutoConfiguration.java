package io.github.lvdaxianer.doclens.j.autoconfigure;

import io.github.lvdaxianer.doclens.j.ingestion.application.BatchProcessingScheduler;
import io.github.lvdaxianer.doclens.j.ingestion.domain.BatchRepository;
import io.github.lvdaxianer.doclens.j.processing.application.BatchDeleteUseCase;
import io.github.lvdaxianer.doclens.j.processing.application.DocumentDeleteDependencies;
import io.github.lvdaxianer.doclens.j.processing.application.DocumentDeleteUseCase;
import io.github.lvdaxianer.doclens.j.processing.application.DocumentRetryDependencies;
import io.github.lvdaxianer.doclens.j.processing.application.DocumentRetryCleanupDependencies;
import io.github.lvdaxianer.doclens.j.processing.application.DocumentRetryUseCase;
import io.github.lvdaxianer.doclens.j.processing.domain.DocumentPageResultRepository;
import io.github.lvdaxianer.doclens.j.processing.domain.DocumentPageTaskRepository;
import io.github.lvdaxianer.doclens.j.processing.domain.DocumentJobRepository;
import io.github.lvdaxianer.doclens.j.processing.domain.OcrEventFactory;
import io.github.lvdaxianer.doclens.j.processing.domain.OcrEventRepository;
import io.github.lvdaxianer.doclens.j.processing.domain.OcrResultRepository;
import io.github.lvdaxianer.doclens.j.shared.application.TransactionRunner;
import io.github.lvdaxianer.doclens.j.storage.ObjectStorage;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;

/**
 * 文档重试与删除自动配置。
 *
 * @author lvdaxianerplus
 * @date 2026-06-11
 */
@AutoConfiguration(after = DocLensProcessingAutoConfiguration.class)
@ConditionalOnBean({
        DocumentJobRepository.class,
        BatchRepository.class,
        OcrResultRepository.class,
        OcrEventRepository.class,
        ObjectStorage.class,
        OcrEventFactory.class,
        BatchProcessingScheduler.class
})
public class DocLensDocumentLifecycleAutoConfiguration {

    /**
     * 创建文档级重试用例。
     *
     * @param dependencies 重试依赖
     * @param transactionRunner 事务执行器
     * @return 文档级重试用例
     * @author lvdaxianerplus
     * @date 2026-06-11
     */
    @Bean
    @ConditionalOnMissingBean
    DocumentRetryUseCase documentRetryUseCase(
            DocumentRetryDependencies dependencies,
            TransactionRunner transactionRunner
    ) {
        return new DocumentRetryUseCase(dependencies, transactionRunner);
    }

    /**
     * 创建文档重试用例依赖持有对象。
     *
     * @param context Spring 上下文
     * @return 文档重试依赖
     * @author lvdaxianerplus
     * @date 2026-06-11
     */
    @Bean
    @ConditionalOnMissingBean
    DocumentRetryDependencies documentRetryDependencies(ApplicationContext context) {
        return new DocumentRetryDependencies(context.getBean(DocumentJobRepository.class),
                context.getBean(BatchRepository.class), context.getBean(OcrEventRepository.class),
                context.getBean(BatchProcessingScheduler.class), context.getBean(OcrEventFactory.class),
                documentRetryCleanupDependencies(context));
    }

    /**
     * 创建文档重试页级清理依赖持有对象。
     *
     * @param context Spring 上下文
     * @return 页级清理依赖
     * @author lvdaxianerplus
     * @date 2026-06-19
     */
    @Bean
    @ConditionalOnMissingBean
    DocumentRetryCleanupDependencies documentRetryCleanupDependencies(ApplicationContext context) {
        return new DocumentRetryCleanupDependencies(context.getBean(DocumentPageTaskRepository.class),
                context.getBean(DocumentPageResultRepository.class));
    }

    /**
     * 创建文档级删除用例。
     *
     * @param dependencies 删除依赖
     * @param transactionRunner 事务执行器
     * @return 文档级删除用例
     * @author lvdaxianerplus
     * @date 2026-06-11
     */
    @Bean
    @ConditionalOnMissingBean
    DocumentDeleteUseCase documentDeleteUseCase(
            DocumentDeleteDependencies dependencies,
            TransactionRunner transactionRunner
    ) {
        return new DocumentDeleteUseCase(dependencies, transactionRunner);
    }

    /**
     * 创建批次级删除用例。
     *
     * @param dependencies 删除依赖
     * @param transactionRunner 事务执行器
     * @return 批次级删除用例
     * @author lvdaxianerplus
     * @date 2026-06-11
     */
    @Bean
    @ConditionalOnMissingBean
    BatchDeleteUseCase batchDeleteUseCase(
            DocumentDeleteDependencies dependencies,
            TransactionRunner transactionRunner
    ) {
        return new BatchDeleteUseCase(dependencies, transactionRunner);
    }

    /**
     * 创建文档删除用例依赖持有对象。
     *
     * @param context Spring 上下文
     * @return 文档删除依赖
     * @author lvdaxianerplus
     * @date 2026-06-11
     */
    @Bean
    @ConditionalOnMissingBean
    DocumentDeleteDependencies documentDeleteDependencies(ApplicationContext context) {
        return new DocumentDeleteDependencies(context.getBean(DocumentJobRepository.class),
                context.getBean(BatchRepository.class), context.getBean(OcrResultRepository.class),
                context.getBean(OcrEventRepository.class), context.getBean(ObjectStorage.class),
                context.getBean(OcrEventFactory.class));
    }
}
