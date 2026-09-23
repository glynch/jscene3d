/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.authoring.protocol;

import java.util.Map;
import java.util.Objects;

/**
 * Stable wire projection of one JScene3D project diagnostic.
 *
 * @param severity diagnostic severity
 * @param code stable feature-owned diagnostic code
 * @param message locale-neutral English fallback
 * @param source absolute source URI
 * @param location JSON Pointer or empty document location
 * @param details language-neutral diagnostic detail values
 */
public record ProjectDiagnosticDto(
        String severity, String code, String message, String source, String location, Map<String, String> details) {
    /** Validates and copies one diagnostic projection. */
    public ProjectDiagnosticDto {
        Objects.requireNonNull(severity, "severity");
        Objects.requireNonNull(code, "code");
        Objects.requireNonNull(message, "message");
        Objects.requireNonNull(source, "source");
        Objects.requireNonNull(location, "location");
        details = Map.copyOf(details);
    }
}
