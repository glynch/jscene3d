/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.loaders;

import java.nio.file.Path;

/** Reports an operational failure while reading or decoding a texture image. */
public final class TextureLoadException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    /** Serializable representation of the failing image path. */
    private final String source;

    /** Retains the failing source and diagnostic message. */
    TextureLoadException(Path source, String message) {
        super(message);
        this.source = source.toString();
    }

    /** Retains the failing source, diagnostic message, and underlying I/O cause. */
    TextureLoadException(Path source, String message, Throwable cause) {
        super(message, cause);
        this.source = source.toString();
    }

    /**
     * Returns the image path that could not be loaded.
     *
     * @return failing source path
     */
    public Path source() {
        return Path.of(source);
    }
}
