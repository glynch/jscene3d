/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.standard.presentation.internal;

import static io.github.glynch.jscene3d.project.validation.internal.DomainRuleDiagnostics.error;

import io.github.glynch.jscene3d.project.component.ComponentType;
import io.github.glynch.jscene3d.project.diagnostic.ProjectDiagnostic;
import io.github.glynch.jscene3d.project.standard.presentation.StandardGamePresentationDescriptors;
import io.github.glynch.jscene3d.project.value.ProjectValue;
import java.math.BigDecimal;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.jspecify.annotations.Nullable;

/** Authored-domain rules owned by built-in screen-presentation types. */
public final class StandardPresentationPropertyRules {
    private static final Set<String> BITMAP_ALIGNMENTS = Set.of("left", "right");

    /** Prevents construction of this type-owned validation policy. */
    private StandardPresentationPropertyRules() {
        throw new AssertionError("StandardPresentationPropertyRules cannot be instantiated");
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
        List<ProjectDiagnostic> diagnostics = new ArrayList<>();
        if (type.equals(StandardGamePresentationDescriptors.screenCanvasType())) {
            finite(
                            values,
                            StandardGamePresentationDescriptors.referenceWidthProperty()
                                    .value(),
                            source,
                            location)
                    .ifPresent(diagnostics::add);
            finite(
                            values,
                            StandardGamePresentationDescriptors.referenceHeightProperty()
                                    .value(),
                            source,
                            location)
                    .ifPresent(diagnostics::add);
        } else if (type.equals(StandardGamePresentationDescriptors.screenRegionType())) {
            screenRegionNumbers()
                    .forEach(propertyId ->
                            finite(values, propertyId, source, location).ifPresent(diagnostics::add));
        } else if (type.equals(StandardGamePresentationDescriptors.bitmapNumberType())) {
            bitmapAlignment(values, source, location).ifPresent(diagnostics::add);
            exactNonNegativeInteger(values, source, location).ifPresent(diagnostics::add);
        }
        return List.copyOf(diagnostics);
    }

    /** Returns screen-region numeric properties in declaration order. */
    private static List<String> screenRegionNumbers() {
        return List.of(
                StandardGamePresentationDescriptors.anchorXProperty().value(),
                StandardGamePresentationDescriptors.anchorYProperty().value(),
                StandardGamePresentationDescriptors.pivotXProperty().value(),
                StandardGamePresentationDescriptors.pivotYProperty().value(),
                StandardGamePresentationDescriptors.offsetXProperty().value(),
                StandardGamePresentationDescriptors.offsetYProperty().value(),
                StandardGamePresentationDescriptors.widthProperty().value(),
                StandardGamePresentationDescriptors.heightProperty().value());
    }

    /** Restricts the bitmap-number alignment to the runtime-supported vocabulary. */
    private static Optional<ProjectDiagnostic> bitmapAlignment(
            Map<String, ProjectValue> values, URI source, String location) {
        String propertyId =
                StandardGamePresentationDescriptors.alignmentProperty().value();
        ProjectValue value = values.get(propertyId);
        if (value instanceof ProjectValue.TextValue text && !BITMAP_ALIGNMENTS.contains(text.value())) {
            return Optional.of(
                    error(source, location + "/" + propertyId, propertyId, "alignment must be left or right"));
        }
        return Optional.empty();
    }

    /** Enforces the bitmap-number runtime integer contract. */
    private static Optional<ProjectDiagnostic> exactNonNegativeInteger(
            Map<String, ProjectValue> values, URI source, String location) {
        String propertyId =
                StandardGamePresentationDescriptors.initialValueProperty().value();
        BigDecimal value = number(values, propertyId);
        if (value == null) {
            return Optional.empty();
        }
        try {
            if (value.intValueExact() >= 0) {
                return Optional.empty();
            }
        } catch (ArithmeticException ignored) {
            // Report the same authored-domain contract below.
        }
        return Optional.of(error(
                source,
                location + "/" + propertyId,
                propertyId,
                "initial value must be a non-negative signed 32-bit integer"));
    }

    /** Reports values which cannot be represented as finite runtime floats. */
    private static Optional<ProjectDiagnostic> finite(
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

    /** Returns a number when the already core-validated property is available. */
    private static @Nullable BigDecimal number(Map<String, ProjectValue> values, String propertyId) {
        ProjectValue value = values.get(propertyId);
        return value instanceof ProjectValue.NumberValue number ? number.value() : null;
    }
}
