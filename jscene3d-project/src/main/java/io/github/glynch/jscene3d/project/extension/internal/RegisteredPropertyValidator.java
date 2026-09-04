/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.extension.internal;

import io.github.glynch.jscene3d.project.diagnostic.ProjectDiagnostic;
import io.github.glynch.jscene3d.project.extension.PropertyDescriptor;
import io.github.glynch.jscene3d.project.extension.RegisteredTypeDescriptor;
import io.github.glynch.jscene3d.project.internal.DiagnosticCollector;
import io.github.glynch.jscene3d.project.internal.JsonPointers;
import io.github.glynch.jscene3d.project.value.ProjectValue;
import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** Shared descriptor-aware validation for one authored property set. */
public final class RegisteredPropertyValidator {
    /** Prevents instantiation of this shared validation policy. */
    private RegisteredPropertyValidator() {
        throw new AssertionError("RegisteredPropertyValidator cannot be instantiated");
    }

    /**
     * Validates authored properties against one registered type descriptor.
     *
     * @param authored properties to validate
     * @param type registered type descriptor
     * @param source source document URI
     * @param location JSON Pointer of the property object
     * @param diagnosticPrefix document-family diagnostic prefix
     * @return immutable ordered property diagnostics
     */
    public static List<ProjectDiagnostic> validate(
            Map<String, ProjectValue> authored,
            RegisteredTypeDescriptor type,
            URI source,
            String location,
            String diagnosticPrefix) {
        Objects.requireNonNull(authored, "authored");
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(source, "source");
        Objects.requireNonNull(location, "location");
        Objects.requireNonNull(diagnosticPrefix, "diagnosticPrefix");
        DiagnosticCollector diagnostics = new DiagnosticCollector(source);
        for (Map.Entry<String, ProjectValue> entry : authored.entrySet()) {
            validateProperty(entry, type, location, diagnostics, diagnosticPrefix);
        }
        for (PropertyDescriptor property : type.properties().values()) {
            if (property.isRequired() && !authored.containsKey(property.id())) {
                diagnostics.error(
                        diagnosticPrefix + ".property.required",
                        "required property is missing: " + property.id(),
                        location);
            }
        }
        return diagnostics.diagnostics();
    }

    /** Validates one authored property entry. */
    private static void validateProperty(
            Map.Entry<String, ProjectValue> entry,
            RegisteredTypeDescriptor type,
            String location,
            DiagnosticCollector diagnostics,
            String diagnosticPrefix) {
        PropertyDescriptor property = type.properties().get(entry.getKey());
        String propertyLocation = location + "/" + JsonPointers.escapeSegment(entry.getKey());
        if (property == null) {
            diagnostics.error(
                    diagnosticPrefix + ".property.unknown",
                    "property is not declared by " + type.type() + ": " + entry.getKey(),
                    propertyLocation);
        } else if (!property.accepts(entry.getValue())) {
            diagnostics.error(
                    diagnosticPrefix + ".property.value",
                    "property does not satisfy descriptor " + property.id(),
                    propertyLocation);
        }
    }
}
