/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.importing;

/** Distinct terminal outcome for a cooperatively cancelled import operation. */
public final class ImportCancelledException extends RuntimeException {
    /** Creates a cancellation outcome with a stable message. */
    public ImportCancelledException() {
        super("Import operation was cancelled");
    }
}
