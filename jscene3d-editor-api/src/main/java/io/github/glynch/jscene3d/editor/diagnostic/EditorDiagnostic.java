/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.diagnostic;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Toolkit-independent diagnostic occurrence relative to one collection-owned source.
 *
 * @param severity diagnostic severity
 * @param code stable producer-defined code
 * @param source concise producer name, or an empty string
 * @param message non-blank user-facing message
 * @param location producer-defined logical location or an empty string
 * @param range optional source-text range
 * @param details additional immutable diagnostic detail
 */
public record EditorDiagnostic(
        EditorDiagnosticSeverity severity,
        String code,
        String source,
        String message,
        String location,
        Optional<EditorTextRange> range,
        Map<String, String> details) {
    /** Copies and validates one diagnostic occurrence. */
    public EditorDiagnostic {
        Objects.requireNonNull(severity, "severity");
        if (Objects.requireNonNull(code, "code").isBlank()) {
            throw new IllegalArgumentException("code must not be blank");
        }
        Objects.requireNonNull(source, "source");
        if (Objects.requireNonNull(message, "message").isBlank()) {
            throw new IllegalArgumentException("message must not be blank");
        }
        Objects.requireNonNull(location, "location");
        Objects.requireNonNull(range, "range");
        details = Map.copyOf(Objects.requireNonNull(details, "details"));
    }

    /**
     * Creates a diagnostic without a separately identified producer.
     *
     * @param severity diagnostic severity
     * @param code stable producer-defined code
     * @param message non-blank user-facing message
     * @param location producer-defined logical location or an empty string
     * @param range optional source-text range
     * @param details additional immutable diagnostic detail
     */
    public EditorDiagnostic(
            EditorDiagnosticSeverity severity,
            String code,
            String message,
            String location,
            Optional<EditorTextRange> range,
            Map<String, String> details) {
        this(severity, code, "", message, location, range, details);
    }
}
