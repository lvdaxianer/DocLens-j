package com.doclens.shared.application;

import java.util.Optional;
import java.util.function.Supplier;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Executes application use case fragments inside explicit Spring transactions.
 *
 * @author lvdaxianerplus
 * @date 2026-06-07
 */
@Component
public class TransactionRunner {

    private final TransactionTemplate transactionTemplate;

    /**
     * Creates transaction runner.
     *
     * @param transactionTemplate Spring transaction template
     * @author lvdaxianerplus
     * @date 2026-06-07
     */
    public TransactionRunner(TransactionTemplate transactionTemplate) {
        this.transactionTemplate = transactionTemplate;
    }

    /**
     * Executes a value-returning action in a transaction.
     *
     * @param action transactional action
     * @param <T> result type
     * @return action result
     * @author lvdaxianerplus
     * @date 2026-06-07
     */
    public <T> T requiredResult(Supplier<T> action) {
        return Optional.ofNullable(transactionTemplate.execute(status -> action.get()))
                .orElseThrow(() -> new IllegalStateException("transaction returned empty result"));
    }

    /**
     * Executes a void action in a transaction.
     *
     * @param action transactional action
     * @author lvdaxianerplus
     * @date 2026-06-07
     */
    public void requiredVoid(Runnable action) {
        transactionTemplate.executeWithoutResult(status -> action.run());
    }
}
