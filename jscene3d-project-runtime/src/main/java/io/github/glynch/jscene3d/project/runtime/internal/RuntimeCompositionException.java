/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.runtime.internal;

import java.util.Objects;

/** Structured composition failure converted to a project diagnostic by the loader. */
public final class RuntimeCompositionException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    /** Stable diagnostic code. */
    private final String code;

    /** JSON Pointer location. */
    private final String location;

    /**
     * Creates one composition failure.
     *
     * @param code stable diagnostic code
     * @param message human-readable failure detail
     * @param location JSON Pointer location
     */
    public RuntimeCompositionException(String code, String message, String location) {
        super(message);
        this.code = Preconditions.requireNonBlank(code, "code");
        this.location = Objects.requireNonNull(location, "location");
    }

    /**
     * Returns the stable diagnostic code.
     *
     * @return diagnostic code
     */
    public String code() {
        return code;
    }

    /**
     * Returns the JSON Pointer location.
     *
     * @return diagnostic location
     */
    public String location() {
        return location;
    }
}
