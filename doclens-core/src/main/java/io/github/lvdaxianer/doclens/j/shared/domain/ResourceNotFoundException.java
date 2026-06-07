package io.github.lvdaxianer.doclens.j.shared.domain;

/**
 * Exception thrown when a requested resource does not exist.
 *
 * @author lvdaxianerplus
 * @date 2026-06-07
 */
public class ResourceNotFoundException extends RuntimeException {

    /**
     * Creates a not-found exception.
     *
     * @param message exception message
     * @author lvdaxianerplus
     * @date 2026-06-07
     */
    public ResourceNotFoundException(String message) {
        super(message);
    }
}
