package io.github.lvdaxianer.doclens.j.shared.application;

import java.util.function.Supplier;

/**
 * 在显式事务中执行应用用例片段。
 *
 * @author lvdaxianerplus
 * @date 2026-06-07
 */
public interface TransactionRunner {

    /**
     * 在事务中执行有返回值的动作。
     *
     * @param action 事务动作
     * @param <T> 结果类型
     * @return 动作结果
     * @author lvdaxianerplus
     * @date 2026-06-07
     */
    <T> T requiredResult(Supplier<T> action);

    /**
     * 在事务中执行无返回值的动作。
     *
     * @param action 事务动作
     * @author lvdaxianerplus
     * @date 2026-06-07
     */
    void requiredVoid(Runnable action);
}
