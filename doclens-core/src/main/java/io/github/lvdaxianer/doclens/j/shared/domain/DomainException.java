package io.github.lvdaxianer.doclens.j.shared.domain;

/**
 * Base exception for domain rule violations.
 *
 * @author lvdaxianerplus
 * @date 2026-06-07
 */
public class DomainException extends RuntimeException {

    /**
     * Creates a domain exception.
     *
     * @param message exception message
     * @author lvdaxianerplus
     * @date 2026-06-07
     */
    public DomainException(String message) {
        super(message);
    }
}
