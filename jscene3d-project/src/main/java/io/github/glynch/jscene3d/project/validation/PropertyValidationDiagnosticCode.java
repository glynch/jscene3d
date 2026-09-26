/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.validation;

import io.github.glynch.jscene3d.diagnostic.DiagnosticCode;

/** Stable core diagnostic identities for authored property validation. */
public enum PropertyValidationDiagnosticCode implements DiagnosticCode {
    /** A required property is absent. */
    REQUIRED("property.required", "Property {0} is required"),
    /** An authored property is not declared by its owner type. */
    UNKNOWN("property.unknown", "Property {0} is not declared by {1}"),
    /** A value has the wrong top-level structural kind. */
    KIND("property.kind", "Property {0} requires {1}, but received {2}"),
    /** An array has the wrong number of elements. */
    ARRAY_LENGTH("property.array.length", "Property {0} requires {1} elements, but received {2}"),
    /** An array element has the wrong structural kind. */
    ARRAY_ELEMENT_KIND("property.array.element-kind", "Property {0} element {1} requires {2}, but received {3}"),
    /** A reference uses a namespace excluded by its descriptor. */
    REFERENCE_KIND("property.reference.kind", "Property {0} does not accept reference namespace {1}"),
    /** An INTEGER semantic value is not mathematically integral. */
    INTEGER("property.integer", "Property {0} must be a mathematically integral number"),
    /** A number violates its declared lower bound. */
    MINIMUM("property.minimum", "Property {0} violates its {1} minimum of {2}"),
    /** A number violates its declared upper bound. */
    MAXIMUM("property.maximum", "Property {0} violates its {1} maximum of {2}"),
    /** An authored quaternion has zero magnitude. */
    QUATERNION_ZERO("property.quaternion.zero", "Property {0} must be a non-zero quaternion"),
    /** A semantic or owner-type domain invariant is violated. */
    DOMAIN("property.domain", "Property validation failed for {0}: {1}"),
    /** An authored entity or component target is unavailable in its definition scope. */
    TARGET("property.target", "Property {0} contains an invalid target: {1}");

    private final String value;
    private final String message;

    /** Stores one stable code and English fallback. */
    PropertyValidationDiagnosticCode(String value, String message) {
        this.value = value;
        this.message = message;
    }

    @Override
    public String code() {
        return value;
    }

    @Override
    public String defaultMessage() {
        return message;
    }
}
