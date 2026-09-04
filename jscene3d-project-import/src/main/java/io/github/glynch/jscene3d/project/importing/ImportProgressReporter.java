/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.importing;

import java.util.Objects;

/** Caller-supplied progress destination invoked synchronously on the calling thread. */
@FunctionalInterface
public interface ImportProgressReporter {
    /**
     * Receives one progress update.
     *
     * @param progress immutable update
     */
    void report(ImportProgress progress);

    /**
     * Returns a reporter that discards progress.
     *
     * @return shared no-op reporter
     */
    static ImportProgressReporter none() {
        return IgnoredProgress.INSTANCE;
    }

    /** Shared allocation-free no-op implementation. */
    enum IgnoredProgress implements ImportProgressReporter {
        /** The only no-op reporter. */
        INSTANCE;

        @Override
        public void report(ImportProgress progress) {
            Objects.requireNonNull(progress, "progress");
        }
    }
}
