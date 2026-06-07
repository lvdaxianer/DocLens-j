package io.github.lvdaxianer.doclens.j.shared.application;

import java.util.Optional;
import java.util.function.Supplier;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Spring transaction runner adapter for DocLens core.
 *
 * @author lvdaxianerplus
 * @date 2026-06-07
 */
public class SpringTransactionRunner implements TransactionRunner {

    private final TransactionTemplate transactionTemplate;

    /**
     * Creates Spring transaction runner.
     *
     * @param transactionTemplate Spring transaction template
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
