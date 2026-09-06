/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.spatial3d;

import io.github.glynch.jscene3d.project.component.PropertyId;
import io.github.glynch.jscene3d.project.value.ProjectValue;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.joml.Quaternionf;
import org.joml.Quaternionfc;
import org.joml.Vector3f;
import org.joml.Vector3fc;

/** Validated numeric transform values decoded from structurally validated project properties. */
final class AuthoredTransform3d {
    /** Copied local position. */
    private final Vector3f position;

    /** Copied normalized local orientation. */
    private final Quaternionf orientation;

    /** Copied local scale. */
    private final Vector3f scale;

    /** Stores complete validated local transform values. */
    private AuthoredTransform3d(Vector3f position, Quaternionf orientation, Vector3f scale) {
        this.position = position;
        this.orientation = orientation;
        this.scale = scale;
    }

    /** Decodes exact array shapes and finite numeric values from effective component properties. */
    static AuthoredTransform3d from(Map<PropertyId, ProjectValue> properties) {
        Map<PropertyId, ProjectValue> values = Objects.requireNonNull(properties, "properties");
        Vector3f position = vector3(values, Spatial3dDescriptors.positionProperty());
        Quaternionf orientation = quaternion(values, Spatial3dDescriptors.orientationProperty());
        Vector3f scale = vector3(values, Spatial3dDescriptors.scaleProperty());
        return new AuthoredTransform3d(position, orientation, scale);
    }

    /** Returns the internal immutable-use position view. */
    Vector3fc position() {
        return position;
    }

    /** Returns the internal immutable-use orientation view. */
    Quaternionfc orientation() {
        return orientation;
    }

    /** Returns the internal immutable-use scale view. */
    Vector3fc scale() {
        return scale;
    }

    /** Decodes one exact three-number vector. */
    private static Vector3f vector3(Map<PropertyId, ProjectValue> properties, PropertyId property) {
        List<ProjectValue> values = array(properties, property, 3);
        return new Vector3f(number(values, 0, property), number(values, 1, property), number(values, 2, property));
    }

    /** Decodes and normalizes one exact four-number quaternion. */
    private static Quaternionf quaternion(Map<PropertyId, ProjectValue> properties, PropertyId property) {
        List<ProjectValue> values = array(properties, property, 4);
        Quaternionf result = new Quaternionf(
                number(values, 0, property),
                number(values, 1, property),
                number(values, 2, property),
                number(values, 3, property));
        if (result.lengthSquared() == 0.0F) {
            throw new IllegalArgumentException(property + " must not be a zero-length quaternion");
        }
        return result.normalize();
    }

    /** Requires one effective property to be an array with the exact expected size. */
    private static List<ProjectValue> array(
            Map<PropertyId, ProjectValue> properties, PropertyId property, int expectedSize) {
        ProjectValue value = Objects.requireNonNull(properties.get(property), property + " property");
        if (!(value instanceof ProjectValue.ArrayValue array) || array.values().size() != expectedSize) {
            throw new IllegalArgumentException(property + " must contain exactly " + expectedSize + " numbers");
        }
        return array.values();
    }

    /** Converts one authored number to a finite float. */
    private static float number(List<ProjectValue> values, int index, PropertyId property) {
        ProjectValue value = values.get(index);
        if (!(value instanceof ProjectValue.NumberValue number)) {
            throw new IllegalArgumentException(property + " entry " + index + " must be a number");
        }
        float result = number.value().floatValue();
        if (!Float.isFinite(result)) {
            throw new IllegalArgumentException(property + " entry " + index + " must be finite");
        }
        return result;
    }
}
