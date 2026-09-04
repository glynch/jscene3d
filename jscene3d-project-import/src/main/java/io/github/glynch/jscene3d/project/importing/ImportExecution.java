/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.importing;

import java.util.Objects;

/** Immutable caller-owned cancellation and progress policy for one synchronous operation. */
public final class ImportExecution {
    private static final ImportExecution DEFAULT =
            new ImportExecution(ImportCancellation.none(), ImportProgressReporter.none());

    private final ImportCancellation cancellation;
    private final ImportProgressReporter progressReporter;

    /** Stores validated execution collaborators. */
    private ImportExecution(ImportCancellation cancellation, ImportProgressReporter progressReporter) {
        this.cancellation = Objects.requireNonNull(cancellation, "cancellation");
        this.progressReporter = Objects.requireNonNull(progressReporter, "progressReporter");
    }

    /**
     * Returns the shared non-cancelling, no-progress execution policy.
     *
     * @return default execution policy
     */
    public static ImportExecution defaults() {
        return DEFAULT;
    }

    /**
     * Creates an execution policy from caller-owned collaborators.
     *
     * @param cancellation cooperative cancellation signal
     * @param progressReporter synchronous progress destination
     * @return immutable execution policy
     */
    public static ImportExecution of(ImportCancellation cancellation, ImportProgressReporter progressReporter) {
        return new ImportExecution(cancellation, progressReporter);
    }

    /**
     * Returns the cooperative cancellation signal.
     *
     * @return cooperative cancellation signal
     */
    public ImportCancellation cancellation() {
        return cancellation;
    }

    /**
     * Returns the synchronous progress destination.
     *
     * @return synchronous progress destination
     */
    public ImportProgressReporter progressReporter() {
        return progressReporter;
    }
}
