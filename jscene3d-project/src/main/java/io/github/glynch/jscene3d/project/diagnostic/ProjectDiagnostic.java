/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.diagnostic;

import static io.github.glynch.jscene3d.project.internal.Preconditions.requireAbsoluteUri;

import io.github.glynch.jscene3d.diagnostic.DiagnosticCode;
import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** One structured project-loading message suitable for tools and graphical interfaces.
 *
 * @param severity diagnostic severity
 * @param code feature-owned stable code and English fallback
 * @param source absolute source URI, including {@code jar:} resources
 * @param location JSON Pointer location, or an empty string for the complete file
 * @param messageArguments immutable ordered values for localized message formatting
 * @param details immutable technical metadata associated with the diagnostic
 */
public record ProjectDiagnostic(
        Severity severity,
        DiagnosticCode code,
        URI source,
        String location,
        List<Object> messageArguments,
        Map<String, String> details) {
    /**
     * Creates a diagnostic with no display-message arguments.
     *
     * @param severity diagnostic severity
     * @param code feature-owned stable code and English fallback
     * @param source absolute source URI, including {@code jar:} resources
     * @param location JSON Pointer location, or an empty string for the complete file
     * @param details immutable technical metadata associated with the diagnostic
     */
    public ProjectDiagnostic(
            Severity severity, DiagnosticCode code, URI source, String location, Map<String, String> details) {
        this(severity, code, source, location, List.of(), details);
    }

    /** Validates diagnostic values. */
    public ProjectDiagnostic {
        Objects.requireNonNull(severity, "severity");
        Objects.requireNonNull(code, "code");
        source = requireAbsoluteUri(source, "source");
        Objects.requireNonNull(location, "location");
        messageArguments = List.copyOf(messageArguments);
        details = Map.copyOf(details);
    }

    /**
     * Returns the English fallback text supplied by the diagnostic code.
     *
     * @return non-blank fallback message
     */
    public String message() {
        return code.defaultMessage();
    }

    /** Project-loading diagnostic severity. */
    public enum Severity {
        /** Project loading cannot produce a valid descriptor. */
        ERROR,
        /** Loading can continue, but the project needs attention. */
        WARNING
    }
}
