/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.loaders;

import java.nio.file.Path;

/** Reports a failure while reading or decoding a high-dynamic-range environment image. */
public final class EnvironmentMapLoadException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    /** Serializable form of the source path. */
    private final String source;

    /** Retains the failing source and diagnostic message. */
    EnvironmentMapLoadException(Path source, String message) {
        super(message);
        this.source = source.toString();
    }

    /** Retains the failing source, diagnostic message, and underlying cause. */
    EnvironmentMapLoadException(Path source, String message, Throwable cause) {
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
