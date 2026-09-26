/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.standard.game3d.internal;

import static io.github.glynch.jscene3d.project.validation.internal.DomainRuleDiagnostics.error;

import io.github.glynch.jscene3d.project.component.ComponentType;
import io.github.glynch.jscene3d.project.diagnostic.ProjectDiagnostic;
import io.github.glynch.jscene3d.project.standard.game3d.StandardGame3dDescriptors;
import io.github.glynch.jscene3d.project.value.ProjectValue;
import java.math.BigDecimal;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.jspecify.annotations.Nullable;

/** Authored-domain rules owned by built-in 3D gameplay types. */
public final class StandardGame3dPropertyRules {
    /** Prevents construction of this type-owned validation policy. */
    private StandardGame3dPropertyRules() {
        throw new AssertionError("StandardGame3dPropertyRules cannot be instantiated");
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
        if (!type.equals(StandardGame3dDescriptors.firstPersonControllerType())) {
            return List.of();
        }
        List<ProjectDiagnostic> diagnostics = new ArrayList<>();
        List.of(
                        StandardGame3dDescriptors.moveSpeedProperty().value(),
                        StandardGame3dDescriptors.turnSpeedDegreesProperty().value(),
                        StandardGame3dDescriptors.maximumKeyboardTurnSpeedDegreesProperty()
                                .value(),
                        StandardGame3dDescriptors.keyboardTurnAccelerationDegreesProperty()
                                .value(),
                        StandardGame3dDescriptors.pointerSensitivityProperty().value(),
                        StandardGame3dDescriptors.maximumPitchDegreesProperty().value())
                .forEach(propertyId ->
                        finiteFloat(values, propertyId, source, location).ifPresent(diagnostics::add));
        validateTurnSpeeds(values, source, location).ifPresent(diagnostics::add);
        return List.copyOf(diagnostics);
    }

    /** Enforces the controller's whole-property-set keyboard speed relationship. */
    private static Optional<ProjectDiagnostic> validateTurnSpeeds(
            Map<String, ProjectValue> values, URI source, String location) {
        String turnId = StandardGame3dDescriptors.turnSpeedDegreesProperty().value();
        String maximumId = StandardGame3dDescriptors.maximumKeyboardTurnSpeedDegreesProperty()
                .value();
        BigDecimal turn = number(values, turnId);
        BigDecimal maximum = number(values, maximumId);
        if (turn != null && maximum != null && maximum.compareTo(turn) < 0) {
            return Optional.of(error(
                    source,
                    location + "/" + maximumId,
                    maximumId,
                    "maximum keyboard turn speed must be at least the initial turn speed"));
        }
        return Optional.empty();
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
