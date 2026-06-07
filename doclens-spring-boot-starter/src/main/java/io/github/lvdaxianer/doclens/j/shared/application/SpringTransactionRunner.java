package io.github.lvdaxianer.doclens.j.shared.application;

import java.util.Optional;
import java.util.function.Supplier;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * DocLens core 的 Spring 事务执行器适配器。
 *
 * @author lvdaxianerplus
 * @date 2026-06-07
 */
public class SpringTransactionRunner implements TransactionRunner {

    private final TransactionTemplate transactionTemplate;

    /**
     * 创建 Spring 事务执行器。
     *
     * @param transactionTemplate Spring 事务模板
     * @author lvdaxianerplus
     * @date 2026-06-07
     */
    public SpringTransactionRunner(TransactionTemplate transactionTemplate) {
        this.transactionTemplate = transactionTemplate;
    }

    @Override
    public <T> T requiredResult(Supplier<T> action) {
        return Optional.ofNullable(transactionTemplate.execute(status -> action.get()))
                .orElseThrow(() -> new IllegalStateException("transaction returned empty result"));
    }

    @Override
    public void requiredVoid(Runnable action) {
        transactionTemplate.executeWithoutResult(status -> action.run());
    }
}
