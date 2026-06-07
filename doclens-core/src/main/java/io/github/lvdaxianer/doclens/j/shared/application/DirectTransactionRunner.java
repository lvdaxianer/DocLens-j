package io.github.lvdaxianer.doclens.j.shared.application;

import java.util.function.Supplier;

/**
 * 用于非 Spring 嵌入式使用和测试的直接事务执行器。
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
