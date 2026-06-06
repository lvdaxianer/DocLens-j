package com.doclens.shared.domain;

/**
 * Exception thrown when a uniqueness constraint is violated.
 *
 * @author lvdaxianerplus
 * @date 2026-06-07
 */
public class DuplicateResourceException extends RuntimeException {

    /**
     * Creates a duplicate-resource exception.
     *
     * @param message exception message
     * @author lvdaxianerplus
     * @date 2026-06-07
     */
    public DuplicateResourceException(String message) {
        super(message);
    }
}
