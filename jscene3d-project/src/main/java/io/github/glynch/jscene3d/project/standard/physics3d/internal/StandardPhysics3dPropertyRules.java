/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.standard.physics3d.internal;

import static io.github.glynch.jscene3d.project.validation.internal.DomainRuleDiagnostics.error;

import io.github.glynch.jscene3d.project.component.ComponentType;
import io.github.glynch.jscene3d.project.diagnostic.ProjectDiagnostic;
import io.github.glynch.jscene3d.project.standard.physics3d.StandardPhysics3dDescriptors;
import io.github.glynch.jscene3d.project.value.ProjectValue;
import java.math.BigDecimal;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.jspecify.annotations.Nullable;

/** Authored-domain rules owned by built-in 3D physics types. */
public final class StandardPhysics3dPropertyRules {
    /** Prevents construction of this type-owned validation policy. */
    private StandardPhysics3dPropertyRules() {
        throw new AssertionError("StandardPhysics3dPropertyRules cannot be instantiated");
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
        if (type.equals(StandardPhysics3dDescriptors.collisionShapeType())) {
            exactInteger(
                            values,
                            StandardPhysics3dDescriptors.categoryBitsProperty().value(),
                            source,
                            location)
                    .ifPresent(diagnostics::add);
            exactInteger(values, StandardPhysics3dDescriptors.maskBitsProperty().value(), source, location)
                    .ifPresent(diagnostics::add);
        }
        if (type.equals(StandardPhysics3dDescriptors.characterBodyType())) {
            List.of(
                            StandardPhysics3dDescriptors.gravityProperty().value(),
                            StandardPhysics3dDescriptors.jumpSpeedProperty().value(),
                            StandardPhysics3dDescriptors.maximumStepHeightProperty()
                                    .value(),
                            StandardPhysics3dDescriptors.groundSnapDistanceProperty()
                                    .value())
                    .forEach(propertyId ->
                            finiteFloat(values, propertyId, source, location).ifPresent(diagnostics::add));
        }
        return List.copyOf(diagnostics);
    }

    /** Requires one owner-specific integer property to fit the runtime's signed 32-bit contract. */
    private static Optional<ProjectDiagnostic> exactInteger(
            Map<String, ProjectValue> values, String propertyId, URI source, String location) {
        BigDecimal value = number(values, propertyId);
        if (value == null) {
            return Optional.empty();
        }
        try {
            value.intValueExact();
            return Optional.empty();
        } catch (ArithmeticException exception) {
            return Optional.of(error(
                    source,
                    location + "/" + propertyId,
                    propertyId,
                    "value must fit a signed 32-bit collision bit field"));
        }
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

    /** Returns a number when the already core-validated property is available. */
    private static @Nullable BigDecimal number(Map<String, ProjectValue> values, String propertyId) {
        ProjectValue value = values.get(propertyId);
        return value instanceof ProjectValue.NumberValue number ? number.value() : null;
    }
}
