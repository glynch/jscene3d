/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.physics3d;

import io.github.glynch.jscene3d.project.component.PropertyId;
import io.github.glynch.jscene3d.project.value.ProjectValue;
import io.github.glynch.jscene3d.project.value.ResourceReference;
import java.util.Map;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/** Decodes validated effective project values into strongly typed collision configuration. */
final class AuthoredCollision3d {
    /** Prevents construction of this stateless decoder. */
    private AuthoredCollision3d() {
        throw new AssertionError("AuthoredCollision3d cannot be instantiated");
    }

    /** Decodes one collision-shape component. */
    static Shape shape(Map<PropertyId, ProjectValue> properties) {
        ProjectValue shapeValue = require(properties, Physics3dDescriptors.shapeProperty());
        if (!(shapeValue instanceof ProjectValue.ReferenceValue reference)) {
            throw new IllegalArgumentException("shape must be a resource reference");
        }
        Vector3f position =
                vector3(require(properties, Physics3dDescriptors.localPositionProperty()), "local-position");
        Quaternionf orientation =
                quaternion(require(properties, Physics3dDescriptors.localOrientationProperty()), "local-orientation");
        int category = integer(require(properties, Physics3dDescriptors.categoryBitsProperty()), "category-bits");
        int mask = integer(require(properties, Physics3dDescriptors.maskBitsProperty()), "mask-bits");
        return new Shape(reference.reference(), position, orientation, new CollisionFilter3d(category, mask));
    }

    /** Reads one required property. */
    private static ProjectValue require(Map<PropertyId, ProjectValue> properties, PropertyId id) {
        ProjectValue value = properties.get(id);
        if (value == null) {
            throw new IllegalArgumentException("collision property is missing: " + id);
        }
        return value;
    }

    /** Reads one exact integer. */
    private static int integer(ProjectValue value, String name) {
        if (!(value instanceof ProjectValue.NumberValue number)) {
            throw new IllegalArgumentException(name + " must be an integer");
        }
        try {
            return number.value().intValueExact();
        } catch (ArithmeticException failure) {
            throw new IllegalArgumentException(name + " must be a 32-bit integer", failure);
        }
    }

    /** Reads one finite three-number vector. */
    private static Vector3f vector3(ProjectValue value, String name) {
        ProjectValue.ArrayValue array = array(value, 3, name);
        return CollisionPreconditions.requireFinite(
                new Vector3f(
                        number(array.values().get(0), name),
                        number(array.values().get(1), name),
                        number(array.values().get(2), name)),
                name);
    }

    /** Reads one finite non-zero four-number orientation. */
    private static Quaternionf quaternion(ProjectValue value, String name) {
        ProjectValue.ArrayValue array = array(value, 4, name);
        return CollisionPreconditions.requireOrientation(
                new Quaternionf(
                        number(array.values().get(0), name),
                        number(array.values().get(1), name),
                        number(array.values().get(2), name),
                        number(array.values().get(3), name)),
                name);
    }

    /** Requires one array with an exact length. */
    private static ProjectValue.ArrayValue array(ProjectValue value, int size, String name) {
        if (!(value instanceof ProjectValue.ArrayValue array) || array.values().size() != size) {
            throw new IllegalArgumentException(name + " must contain exactly " + size + " numbers");
        }
        return array;
    }

    /** Reads one finite float. */
    private static float number(ProjectValue value, String name) {
        if (!(value instanceof ProjectValue.NumberValue number)) {
            throw new IllegalArgumentException(name + " must contain only numbers");
        }
        float result = number.value().floatValue();
        if (!Float.isFinite(result)) {
            throw new IllegalArgumentException(name + " must contain only finite numbers");
        }
        return result;
    }

    /** Fully decoded authored shape configuration. */
    record Shape(
            ResourceReference resource,
            Vector3f localPosition,
            Quaternionf localOrientation,
            CollisionFilter3d filter) {}
}
