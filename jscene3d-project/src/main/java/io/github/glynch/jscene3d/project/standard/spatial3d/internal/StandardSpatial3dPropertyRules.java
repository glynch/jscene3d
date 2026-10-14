/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.standard.spatial3d.internal;

import static io.github.glynch.jscene3d.project.validation.internal.DomainRuleDiagnostics.error;

import io.github.glynch.jscene3d.project.component.ComponentType;
import io.github.glynch.jscene3d.project.diagnostic.ProjectDiagnostic;
import io.github.glynch.jscene3d.project.standard.spatial3d.StandardSpatial3dDescriptors;
import io.github.glynch.jscene3d.project.value.ProjectValue;
import java.math.BigDecimal;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.jspecify.annotations.Nullable;

/** Authored-domain rules owned by built-in 3D spatial types. */
public final class StandardSpatial3dPropertyRules {
    /** Prevents construction of this type-owned validation policy. */
    private StandardSpatial3dPropertyRules() {
        throw new AssertionError("StandardSpatial3dPropertyRules cannot be instantiated");
    }

    /**
     * Validates the exact built-in component type when supported.
     *
     * @param type exact component type
     * @param values effective property values
     * @param source source document containing the component
     * @param location JSON pointer for the component property set
     * @return ordered type-owned diagnostics
     */
    public static List<ProjectDiagnostic> validate(
            ComponentType type, Map<String, ProjectValue> values, URI source, String location) {
        if (type.equals(StandardSpatial3dDescriptors.perspectiveCameraType())) {
            return validatePerspectiveCamera(values, source, location);
        }
        if (type.equals(StandardSpatial3dDescriptors.orthographicCameraType())) {
            return validateOrthographicCamera(values, source, location);
        }
        return List.of();
    }

    /** Validates relationships and runtime representation for a perspective projection. */
    private static List<ProjectDiagnostic> validatePerspectiveCamera(
            Map<String, ProjectValue> values, URI source, String location) {
        List<ProjectDiagnostic> diagnostics = new ArrayList<>();
        String nearId = StandardSpatial3dDescriptors.nearProperty().value();
        String farId = StandardSpatial3dDescriptors.farProperty().value();
        addFiniteFloat(
                diagnostics,
                values,
                StandardSpatial3dDescriptors.fieldOfViewDegreesProperty().value(),
                source,
                location);
        addFiniteFloat(diagnostics, values, nearId, source, location);
        addFiniteFloat(diagnostics, values, farId, source, location);
        validateOrdered(diagnostics, values, nearId, farId, source, location, "clipping distance");
        return List.copyOf(diagnostics);
    }

    /** Validates relationships and runtime representation for an orthographic projection. */
    private static List<ProjectDiagnostic> validateOrthographicCamera(
            Map<String, ProjectValue> values, URI source, String location) {
        List<ProjectDiagnostic> diagnostics = new ArrayList<>();
        String leftId = StandardSpatial3dDescriptors.leftProperty().value();
        String rightId = StandardSpatial3dDescriptors.rightProperty().value();
        String bottomId = StandardSpatial3dDescriptors.bottomProperty().value();
        String topId = StandardSpatial3dDescriptors.topProperty().value();
        String nearId = StandardSpatial3dDescriptors.nearProperty().value();
        String farId = StandardSpatial3dDescriptors.farProperty().value();
        List.of(
                        leftId,
                        rightId,
                        topId,
                        bottomId,
                        nearId,
                        farId,
                        StandardSpatial3dDescriptors.zoomProperty().value())
                .forEach(property -> addFiniteFloat(diagnostics, values, property, source, location));
        validateOrdered(diagnostics, values, leftId, rightId, source, location, "projection boundary");
        validateOrdered(diagnostics, values, bottomId, topId, source, location, "projection boundary");
        validateOrdered(diagnostics, values, nearId, farId, source, location, "clipping distance");
        return List.copyOf(diagnostics);
    }

    /** Adds one finite-float diagnostic when required. */
    private static void addFiniteFloat(
            List<ProjectDiagnostic> diagnostics,
            Map<String, ProjectValue> values,
            String propertyId,
            URI source,
            String location) {
        finiteFloat(values, propertyId, source, location).ifPresent(diagnostics::add);
    }

    /** Validates one strictly ordered numeric-property pair. */
    private static void validateOrdered(
            List<ProjectDiagnostic> diagnostics,
            Map<String, ProjectValue> values,
            String lowerId,
            String upperId,
            URI source,
            String location,
            String description) {
        BigDecimal lower = number(values, lowerId);
        BigDecimal upper = number(values, upperId);
        if (lower != null && upper != null && lower.compareTo(upper) >= 0) {
            diagnostics.add(error(
                    source,
                    location + "/" + upperId,
                    upperId,
                    upperId + " " + description + " must be greater than " + lowerId + " " + description));
        }
    }

    /** Returns a number when the already core-validated property is available. */
    private static @Nullable BigDecimal number(Map<String, ProjectValue> values, String propertyId) {
        ProjectValue value = values.get(propertyId);
        return value instanceof ProjectValue.NumberValue number ? number.value() : null;
    }

    /** Reports values which cannot be represented as finite runtime floats. */
    private static Optional<ProjectDiagnostic> finiteFloat(
            Map<String, ProjectValue> values, String propertyId, URI source, String location) {
        BigDecimal value = number(values, propertyId);
        if (value != null && !Float.isFinite(value.floatValue())) {
            return Optional.of(error(
                    source,
                    location + "/" + propertyId,
                    propertyId,
                    "value must be representable as a finite 32-bit float"));
        }
        return Optional.empty();
    }
}
