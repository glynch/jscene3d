/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.validation.internal;

import io.github.glynch.jscene3d.project.diagnostic.ProjectDiagnostic;
import io.github.glynch.jscene3d.project.extension.ProjectValueKind;
import io.github.glynch.jscene3d.project.extension.PropertyDescriptor;
import io.github.glynch.jscene3d.project.extension.PropertyNumericBound;
import io.github.glynch.jscene3d.project.validation.PropertyValidationDiagnosticCode;
import io.github.glynch.jscene3d.project.value.ProjectValue;
import java.math.BigDecimal;
import java.net.URI;
import java.util.List;
import java.util.Objects;

/** Shared implementation of structural, descriptor, and built-in semantic value rules. */
public final class PropertyValueValidation {
    private static final BigDecimal ZERO = BigDecimal.ZERO;
    private static final BigDecimal ONE = BigDecimal.ONE;

    /** Prevents construction of this stateless validation policy. */
    private PropertyValueValidation() {
        throw new AssertionError("PropertyValueValidation cannot be instantiated");
    }

    /**
     * Returns the compatibility predicate used by {@link PropertyDescriptor#accepts(ProjectValue)}.
     *
     * <p>This deliberately covers structural shape and accepted reference namespace only.
     *
     * @param descriptor property contract to apply
     * @param value candidate authored value
     * @return whether the value has an accepted structure and reference namespace
     */
    public static boolean structurallyAccepts(PropertyDescriptor descriptor, ProjectValue value) {
        Objects.requireNonNull(descriptor, "descriptor");
        ProjectValue validValue = Objects.requireNonNull(value, "value");
        if (ProjectValueKind.of(validValue) != descriptor.valueKind()) {
            return false;
        }
        if (validValue instanceof ProjectValue.ReferenceValue reference) {
            return descriptor.acceptedReferenceKinds().isEmpty()
                    || descriptor
                            .acceptedReferenceKinds()
                            .contains(reference.reference().kind());
        }
        if (!(validValue instanceof ProjectValue.ArrayValue array)) {
            return true;
        }
        if (descriptor.exactElementCount().isPresent()
                && array.values().size() != descriptor.exactElementCount().orElseThrow()) {
            return false;
        }
        return descriptor.elementKind().isEmpty()
                || array.values().stream()
                        .allMatch(element -> ProjectValueKind.of(element)
                                == descriptor.elementKind().orElseThrow());
    }

    /**
     * Validates one descriptor default through all context-free core phases.
     *
     * @param descriptor property contract which declares the default
     * @param value declared default value
     * @param source source descriptor containing the default
     * @param location JSON pointer for the default
     * @return ordered validation diagnostics
     */
    public static List<ProjectDiagnostic> validateDefault(
            PropertyDescriptor descriptor, ProjectValue value, URI source, String location) {
        Objects.requireNonNull(descriptor, "descriptor");
        ValidationDiagnostics diagnostics = new ValidationDiagnostics(source);
        validate(descriptor.id(), Objects.requireNonNull(value, "value"), descriptor, location, diagnostics);
        return diagnostics.values();
    }

    /** Validates one value and returns whether structural prerequisites passed. */
    static boolean validate(
            String propertyId,
            ProjectValue value,
            PropertyDescriptor descriptor,
            String location,
            ValidationDiagnostics diagnostics) {
        if (!validateStructure(propertyId, value, descriptor, location, diagnostics)) {
            return false;
        }
        validateReferenceKind(propertyId, value, descriptor, location, diagnostics);
        validateBounds(propertyId, value, descriptor, location, diagnostics);
        validateSemantic(propertyId, value, descriptor, location, diagnostics);
        return true;
    }

    /** Validates kind and array shape, stopping deeper rules after a failure. */
    private static boolean validateStructure(
            String propertyId,
            ProjectValue value,
            PropertyDescriptor descriptor,
            String location,
            ValidationDiagnostics diagnostics) {
        ProjectValueKind actual = ProjectValueKind.of(value);
        if (actual != descriptor.valueKind()) {
            diagnostics.error(
                    PropertyValidationDiagnosticCode.KIND,
                    location,
                    "wrong structural property kind",
                    propertyId,
                    descriptor.valueKind(),
                    actual);
            return false;
        }
        if (!(value instanceof ProjectValue.ArrayValue array)) {
            return true;
        }
        if (descriptor.exactElementCount().isPresent()
                && array.values().size() != descriptor.exactElementCount().orElseThrow()) {
            diagnostics.error(
                    PropertyValidationDiagnosticCode.ARRAY_LENGTH,
                    location,
                    "wrong array element count",
                    propertyId,
                    descriptor.exactElementCount().orElseThrow(),
                    array.values().size());
            return false;
        }
        return validateElementKinds(propertyId, array, descriptor, location, diagnostics);
    }

    /** Validates homogeneous elements in index order. */
    private static boolean validateElementKinds(
            String propertyId,
            ProjectValue.ArrayValue array,
            PropertyDescriptor descriptor,
            String location,
            ValidationDiagnostics diagnostics) {
        if (descriptor.elementKind().isEmpty()) {
            return true;
        }
        ProjectValueKind expected = descriptor.elementKind().orElseThrow();
        boolean valid = true;
        for (int index = 0; index < array.values().size(); index++) {
            ProjectValueKind actual = ProjectValueKind.of(array.values().get(index));
            if (actual != expected) {
                diagnostics.error(
                        PropertyValidationDiagnosticCode.ARRAY_ELEMENT_KIND,
                        location + "/" + index,
                        "wrong homogeneous array element kind",
                        propertyId,
                        index,
                        expected,
                        actual);
                valid = false;
            }
        }
        return valid;
    }

    /** Enforces the descriptor's accepted reference namespaces. */
    private static void validateReferenceKind(
            String propertyId,
            ProjectValue value,
            PropertyDescriptor descriptor,
            String location,
            ValidationDiagnostics diagnostics) {
        if (value instanceof ProjectValue.ReferenceValue reference
                && !descriptor.acceptedReferenceKinds().isEmpty()
                && !descriptor
                        .acceptedReferenceKinds()
                        .contains(reference.reference().kind())) {
            diagnostics.error(
                    PropertyValidationDiagnosticCode.REFERENCE_KIND,
                    location,
                    "reference namespace is not accepted",
                    propertyId,
                    reference.reference().kind());
        }
    }

    /** Enforces exact inclusive or exclusive decimal bounds. */
    private static void validateBounds(
            String propertyId,
            ProjectValue value,
            PropertyDescriptor descriptor,
            String location,
            ValidationDiagnostics diagnostics) {
        if (!(value instanceof ProjectValue.NumberValue number)) {
            return;
        }
        descriptor
                .editor()
                .minimum()
                .ifPresent(bound -> validateMinimum(propertyId, number.value(), bound, location, diagnostics));
        descriptor
                .editor()
                .maximum()
                .ifPresent(bound -> validateMaximum(propertyId, number.value(), bound, location, diagnostics));
    }

    /** Enforces one lower endpoint using exact decimal comparison. */
    private static void validateMinimum(
            String propertyId,
            BigDecimal value,
            PropertyNumericBound bound,
            String location,
            ValidationDiagnostics diagnostics) {
        int comparison = value.compareTo(bound.value());
        if (comparison < 0 || comparison == 0 && !bound.isInclusive()) {
            diagnostics.error(
                    PropertyValidationDiagnosticCode.MINIMUM,
                    location,
                    "numeric lower bound is violated",
                    propertyId,
                    bound.isInclusive() ? "inclusive" : "exclusive",
                    bound.value());
        }
    }

    /** Enforces one upper endpoint using exact decimal comparison. */
    private static void validateMaximum(
            String propertyId,
            BigDecimal value,
            PropertyNumericBound bound,
            String location,
            ValidationDiagnostics diagnostics) {
        int comparison = value.compareTo(bound.value());
        if (comparison > 0 || comparison == 0 && !bound.isInclusive()) {
            diagnostics.error(
                    PropertyValidationDiagnosticCode.MAXIMUM,
                    location,
                    "numeric upper bound is violated",
                    propertyId,
                    bound.isInclusive() ? "inclusive" : "exclusive",
                    bound.value());
        }
    }

    /** Applies the closed built-in semantic vocabulary without normalizing values. */
    private static void validateSemantic(
            String propertyId,
            ProjectValue value,
            PropertyDescriptor descriptor,
            String location,
            ValidationDiagnostics diagnostics) {
        switch (descriptor.editor().semantic()) {
            case DEFAULT, VECTOR2, VECTOR3, EULER_ROTATION -> {
                // Descriptor construction already guarantees the specialized structural shape.
            }
            case INTEGER -> validateInteger(propertyId, (ProjectValue.NumberValue) value, location, diagnostics);
            case QUATERNION -> validateQuaternion(propertyId, (ProjectValue.ArrayValue) value, location, diagnostics);
            case LINEAR_COLOR ->
                validateLinearColor(propertyId, (ProjectValue.ArrayValue) value, location, diagnostics);
        }
    }

    /** Requires exact mathematical integrality without imposing a primitive range. */
    private static void validateInteger(
            String propertyId, ProjectValue.NumberValue number, String location, ValidationDiagnostics diagnostics) {
        if (number.value().stripTrailingZeros().scale() > 0) {
            diagnostics.error(
                    PropertyValidationDiagnosticCode.INTEGER,
                    location,
                    "number has a non-zero fractional part",
                    propertyId);
        }
    }

    /** Rejects only the exact zero quaternion; non-unit values remain valid authored data. */
    private static void validateQuaternion(
            String propertyId, ProjectValue.ArrayValue array, String location, ValidationDiagnostics diagnostics) {
        boolean zero = array.values().stream()
                .map(ProjectValue.NumberValue.class::cast)
                .map(ProjectValue.NumberValue::value)
                .allMatch(component -> component.compareTo(ZERO) == 0);
        if (zero) {
            diagnostics.error(
                    PropertyValidationDiagnosticCode.QUATERNION_ZERO,
                    location,
                    "all quaternion components are exactly zero",
                    propertyId);
        }
    }

    /** Requires every linear-color channel to lie in the exact inclusive unit interval. */
    private static void validateLinearColor(
            String propertyId, ProjectValue.ArrayValue array, String location, ValidationDiagnostics diagnostics) {
        for (int index = 0; index < array.values().size(); index++) {
            BigDecimal channel = ((ProjectValue.NumberValue) array.values().get(index)).value();
            if (channel.compareTo(ZERO) < 0 || channel.compareTo(ONE) > 0) {
                diagnostics.error(
                        PropertyValidationDiagnosticCode.DOMAIN,
                        location + "/" + index,
                        "linear-color channel lies outside [0, 1]",
                        propertyId,
                        "channel " + index + " must be between 0 and 1 inclusive");
            }
        }
    }
}
