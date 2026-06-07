package io.github.lvdaxianer.doclens.j.shared.application;

import java.util.function.Supplier;

/**
 * Direct transaction runner for embedded non-Spring usage and tests.
 *
 * @author lvdaxianerplus
 * @date 2026-06-07
 */
public class DirectTransactionRunner implements TransactionRunner {

    @Override
    public <T> T requiredResult(Supplier<T> action) {
        return action.get();
    }

    @Override
    public void requiredVoid(Runnable action) {
        action.run();
    }
}
