package io.github.lvdaxianer.doclens.j.shared.application;

import java.util.function.Supplier;

/**
 * Executes application use case fragments inside explicit transactions.
 *
 * @author lvdaxianerplus
 * @date 2026-06-07
 */
public interface TransactionRunner {

    /**
     * Executes a value-returning action in a transaction.
     *
     * @param action transactional action
     * @param <T> result type
     * @return action result
     * @author lvdaxianerplus
     * @date 2026-06-07
     */
    <T> T requiredResult(Supplier<T> action);

    /**
     * Executes a void action in a transaction.
     *
     * @param action transactional action
     * @author lvdaxianerplus
     * @date 2026-06-07
     */
    void requiredVoid(Runnable action);
}
