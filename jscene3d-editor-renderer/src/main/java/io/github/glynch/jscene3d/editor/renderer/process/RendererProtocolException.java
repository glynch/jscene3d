/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.renderer.process;

import org.jspecify.annotations.Nullable;

/** Invalid renderer-protocol input with a stable wire error category. */
final class RendererProtocolException extends Exception {
    private final String category;

    private RendererProtocolException(String category, String message, @Nullable Throwable cause) {
        super(message, cause);
        this.category = category;
    }

    static RendererProtocolException malformed(String message) {
        return new RendererProtocolException("MALFORMED_REQUEST", message, null);
    }

    static RendererProtocolException malformed(String message, Throwable cause) {
        return new RendererProtocolException("MALFORMED_REQUEST", message, cause);
    }

    static RendererProtocolException unknown(String message) {
        return new RendererProtocolException("UNKNOWN_COMMAND", message, null);
    }

    String category() {
        return category;
    }
}
