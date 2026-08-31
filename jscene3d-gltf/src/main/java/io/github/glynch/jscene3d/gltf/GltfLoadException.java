/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.gltf;

import java.io.Serial;
import java.nio.file.Path;
import java.util.Objects;
import org.jspecify.annotations.Nullable;

/** Reports an I/O, format, or unsupported-capability failure while loading glTF. */
public final class GltfLoadException extends RuntimeException {
    @Serial
    private static final long serialVersionUID = 1L;

    /** Serializable source-path representation. */
    private final String source;

    /**
     * Creates a failure associated with one source path.
     *
     * @param source source path
     * @param message diagnostic message
     * @param cause underlying failure, or {@code null} when none exists
     * @throws NullPointerException if {@code source} or {@code message} is {@code null}
     */
    public GltfLoadException(Path source, String message, @Nullable Throwable cause) {
        super(Objects.requireNonNull(message, "message"), cause);
        this.source = Objects.requireNonNull(source, "source").toString();
    }

    /**
     * Returns the source path supplied to the loader.
     *
     * @return source path
     */
    public Path source() {
        return Path.of(source);
    }
}
