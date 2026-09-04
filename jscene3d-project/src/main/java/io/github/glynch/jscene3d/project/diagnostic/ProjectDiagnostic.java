/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.diagnostic;

import static io.github.glynch.jscene3d.project.internal.Preconditions.requireAbsoluteUri;
import static io.github.glynch.jscene3d.project.internal.Preconditions.requireNonBlank;

import java.net.URI;
import java.util.Objects;

/** One structured project-loading message suitable for tools and graphical interfaces.
 *
 * @param severity diagnostic severity
 * @param code stable machine-readable diagnostic code
 * @param message human-readable explanation
 * @param source absolute source URI, including {@code jar:} resources
 * @param location JSON Pointer location, or an empty string for the complete file
 */
public record ProjectDiagnostic(Severity severity, String code, String message, URI source, String location) {
    /** Validates diagnostic values. */
    public ProjectDiagnostic {
        Objects.requireNonNull(severity, "severity");
        code = requireNonBlank(code, "code");
        message = requireNonBlank(message, "message");
        source = requireAbsoluteUri(source, "source");
        Objects.requireNonNull(location, "location");
    }

    /** Project-loading diagnostic severity. */
    public enum Severity {
        /** Project loading cannot produce a valid descriptor. */
        ERROR,
        /** Loading can continue, but the project needs attention. */
        WARNING
    }
}
