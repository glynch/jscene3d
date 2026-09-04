/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.importing;

/** Caller-owned cooperative cancellation signal checked by import operations. */
@FunctionalInterface
public interface ImportCancellation {
    /**
     * Returns whether the caller has requested cancellation.
     *
     * @return {@code true} when the current operation should stop
     */
    boolean isCancellationRequested();

    /** Throws a distinct cancellation outcome when cancellation has been requested. */
    default void checkCancelled() {
        if (isCancellationRequested()) {
            throw new ImportCancelledException();
        }
    }

    /**
     * Returns a cancellation signal that never requests cancellation.
     *
     * @return shared non-cancelling signal
     */
    static ImportCancellation none() {
        return NoCancellation.INSTANCE;
    }

    /** Shared allocation-free non-cancelling implementation. */
    enum NoCancellation implements ImportCancellation {
        /** The only non-cancelling signal. */
        INSTANCE;

        @Override
        public boolean isCancellationRequested() {
            return false;
        }
    }
}
