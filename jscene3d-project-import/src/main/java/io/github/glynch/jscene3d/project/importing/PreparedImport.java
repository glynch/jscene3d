/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.importing;

/** Single-use owned import transaction containing one fully staged generation. */
public interface PreparedImport extends AutoCloseable {
    /**
     * Returns the immutable preparation preview.
     *
     * @return preparation preview
     * @throws IllegalStateException if this transaction is closed
     */
    ImportPreview preview();

    /**
     * Revalidates inputs and atomically publishes this generation.
     *
     * @throws IllegalStateException if the transaction is closed, already committed, or invalid
     * @throws ImportPublicationException if inputs changed or atomic publication fails
     * @throws ImportCancelledException if cancellation was requested
     */
    void commit();

    /**
     * Returns whether publication completed successfully.
     *
     * @return {@code true} after successful publication
     */
    boolean isCommitted();

    /**
     * Returns whether this transaction has been closed.
     *
     * @return {@code true} after closure
     */
    boolean isClosed();

    /**
     * Deletes uncommitted staging content and releases this transaction.
     *
     * <p>Closure is idempotent and terminal.
     */
    @Override
    void close();
}
