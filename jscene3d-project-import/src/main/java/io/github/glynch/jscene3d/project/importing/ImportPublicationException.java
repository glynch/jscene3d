/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.importing;

/** Operational failure preventing an already prepared generation from being published. */
public final class ImportPublicationException extends RuntimeException {
    /**
     * Creates one publication failure.
     *
     * @param message actionable failure description
     */
    public ImportPublicationException(String message) {
        super(message);
    }

    /**
     * Creates one publication failure retaining its filesystem cause.
     *
     * @param message actionable failure description
     * @param cause underlying failure
     */
    public ImportPublicationException(String message, Throwable cause) {
        super(message, cause);
    }
}
