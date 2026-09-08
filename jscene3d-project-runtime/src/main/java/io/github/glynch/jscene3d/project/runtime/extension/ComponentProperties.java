/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.runtime.extension;

import io.github.glynch.jscene3d.project.component.PropertyId;
import io.github.glynch.jscene3d.project.value.ProjectValue;
import io.github.glynch.jscene3d.project.value.ResourceReference;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/** Typed read-only access to descriptor-validated effective component properties. */
public final class ComponentProperties {
    private final Map<PropertyId, ProjectValue> values;

    /** Copies validated effective values while preserving descriptor declaration order.
     *
     * @param values validated effective properties
     */
    public ComponentProperties(Map<PropertyId, ProjectValue> values) {
        Objects.requireNonNull(values, "values");
        Map<PropertyId, ProjectValue> copied = new LinkedHashMap<>();
        values.forEach((property, value) -> copied.put(
                Objects.requireNonNull(property, "property"), Objects.requireNonNull(value, "property value")));
        this.values = Collections.unmodifiableMap(copied);
    }

    /** Returns one required portable value.
     *
     * @param property property identity
     * @return effective value
     * @throws IllegalArgumentException if the property is absent
     */
    public ProjectValue value(PropertyId property) {
        PropertyId requiredProperty = Objects.requireNonNull(property, "property");
        ProjectValue value = values.get(requiredProperty);
        if (value == null) {
            throw new IllegalArgumentException("component property is missing: " + requiredProperty);
        }
        return value;
    }

    /** Returns one required text value.
     *
     * @param property property identity
     * @return stored text
     */
    public String text(PropertyId property) {
        ProjectValue value = value(property);
        if (!(value instanceof ProjectValue.TextValue(String text))) {
            throw typeMismatch(property, "text");
        }
        return text;
    }

    /** Converts one required number to a finite float.
     *
     * @param property property identity
     * @return finite float representation
     */
    public float finiteFloat(PropertyId property) {
        ProjectValue value = value(property);
        if (!(value instanceof ProjectValue.NumberValue(var number))) {
            throw typeMismatch(property, "a number");
        }
        float converted = number.floatValue();
        if (!Float.isFinite(converted)) {
            throw new IllegalArgumentException(property + " must be representable as a finite float");
        }
        return converted;
    }

    /** Returns one required boolean value.
     *
     * @param property property identity
     * @return stored boolean
     */
    public boolean booleanValue(PropertyId property) {
        ProjectValue value = value(property);
        if (!(value instanceof ProjectValue.BooleanValue(boolean result))) {
            throw typeMismatch(property, "a boolean");
        }
        return result;
    }

    /** Returns one required resource reference.
     *
     * @param property property identity
     * @return stored resource reference
     */
    public ResourceReference resourceReference(PropertyId property) {
        ProjectValue value = value(property);
        if (!(value instanceof ProjectValue.ReferenceValue(ResourceReference reference))) {
            throw typeMismatch(property, "a resource reference");
        }
        return reference;
    }

    /** Returns an immutable projection for decoders of compound portable values.
     *
     * @return descriptor-ordered effective values
     */
    public Map<PropertyId, ProjectValue> values() {
        return values;
    }

    private static IllegalArgumentException typeMismatch(PropertyId property, String expected) {
        return new IllegalArgumentException(property + " must be " + expected);
    }
}
