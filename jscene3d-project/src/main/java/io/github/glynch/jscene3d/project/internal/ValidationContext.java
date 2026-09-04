/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.internal;

import static io.github.glynch.jscene3d.project.internal.ProjectIdentifiers.isLocalId;
import static io.github.glynch.jscene3d.project.internal.ProjectIdentifiers.isRegisteredTypeId;

import java.util.Objects;
import java.util.Optional;
import org.jspecify.annotations.Nullable;

/** Applies common diagnostic-producing field validation for one document format. */
public final class ValidationContext {
    private final DiagnosticCollector diagnostics;
    private final String diagnosticPrefix;

    /**
     * Creates a validation context with format-specific diagnostic codes.
     *
     * @param diagnostics destination for validation diagnostics
     * @param diagnosticPrefix stable diagnostic-code prefix
     */
    public ValidationContext(DiagnosticCollector diagnostics, String diagnosticPrefix) {
        this.diagnostics = Objects.requireNonNull(diagnostics, "diagnostics");
        this.diagnosticPrefix = Preconditions.requireNonBlank(diagnosticPrefix, "diagnosticPrefix");
    }

    /**
     * Returns normalized required text or an empty recovery value.
     *
     * @param value nullable authored value
     * @param location JSON Pointer location
     * @return normalized value, or an empty recovery value
     */
    public String requiredText(@Nullable String value, String location) {
        if (value == null || value.isBlank()) {
            diagnostics.error(diagnosticPrefix + ".field.required", "a non-blank value is required", location);
            return "";
        }
        return value.strip();
    }

    /**
     * Returns normalized optional text while rejecting authored blank values.
     *
     * @param value nullable authored value
     * @param location JSON Pointer location
     * @return normalized optional value
     */
    public Optional<String> optionalText(@Nullable String value, String location) {
        if (value == null) {
            return Optional.empty();
        }
        if (value.isBlank()) {
            diagnostics.error(diagnosticPrefix + ".field.blank", "optional values must not be blank", location);
            return Optional.empty();
        }
        return Optional.of(value.strip());
    }

    /**
     * Returns a validated local identifier or an empty recovery value.
     *
     * @param value nullable authored value
     * @param location JSON Pointer location
     * @return validated identifier, or an empty recovery value
     */
    public String requiredLocalId(@Nullable String value, String location) {
        String identifier = requiredText(value, location);
        if (!identifier.isEmpty() && !isLocalId(identifier)) {
            diagnostics.error(
                    diagnosticPrefix + ".field.identifier", "value must be a portable lowercase identifier", location);
            return "";
        }
        return identifier;
    }

    /**
     * Returns a validated registered-type identifier or an empty recovery value.
     *
     * @param value nullable authored value
     * @param location JSON Pointer location
     * @return validated identifier, or an empty recovery value
     */
    public String requiredRegisteredTypeId(@Nullable String value, String location) {
        String identifier = requiredText(value, location);
        if (!identifier.isEmpty() && !isRegisteredTypeId(identifier)) {
            diagnostics.error(
                    diagnosticPrefix + ".field.type",
                    "value must contain an extension id and local type separated by one slash",
                    location);
            return "";
        }
        return identifier;
    }
}
