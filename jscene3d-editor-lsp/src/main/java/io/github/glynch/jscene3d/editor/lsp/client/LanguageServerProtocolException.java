/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.lsp.client;

/** Reports a failed language-server protocol lifecycle operation. */
public final class LanguageServerProtocolException extends RuntimeException {
    /**
     * Creates a protocol failure with its underlying cause.
     *
     * @param message description of the failed protocol operation
     * @param cause underlying asynchronous or transport failure
     */
    public LanguageServerProtocolException(String message, Throwable cause) {
        super(message, cause);
    }
}
