/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.runtime;

import java.util.Objects;

/**
 * One independently releasable retention of a shared immutable runtime resource.
 *
 * <p>A lease does not imply exclusive ownership of its value. Closing is idempotent and terminal, invokes the supplied
 * release operation at most once, and does not directly close the value. This type is not thread-safe.
 *
 * @param <T> retained runtime value type
 */
public final class RuntimeResourceLease<T> implements AutoCloseable {
    private final T value;
    private final Runnable release;
    private boolean closed;

    /** Stores one value and its independently owned release operation. */
    private RuntimeResourceLease(T value, Runnable release) {
        this.value = Objects.requireNonNull(value, "value");
        this.release = Objects.requireNonNull(release, "release");
    }

    /**
     * Creates one open resource lease.
     *
     * @param <T> retained runtime value type
     * @param value shared immutable runtime value
     * @param release operation which releases this retention
     * @return open lease
     * @throws NullPointerException if an argument is {@code null}
     */
    public static <T> RuntimeResourceLease<T> of(T value, Runnable release) {
        return new RuntimeResourceLease<>(value, release);
    }

    /**
     * Returns the retained value while this lease is open.
     *
     * @return retained runtime value
     * @throws IllegalStateException if this lease is closed
     */
    public T value() {
        if (closed) {
            throw new IllegalStateException("runtime resource lease is closed");
        }
        return value;
    }

    /**
     * Returns whether this lease has released its retention.
     *
     * @return {@code true} after closure begins
     */
    public boolean isClosed() {
        return closed;
    }

    /**
     * Releases this retention at most once and makes subsequent value access fail.
     *
     * @throws RuntimeException if the release operation fails; the lease remains closed
     */
    @Override
    public void close() {
        if (closed) {
            return;
        }
        closed = true;
        release.run();
    }
}
