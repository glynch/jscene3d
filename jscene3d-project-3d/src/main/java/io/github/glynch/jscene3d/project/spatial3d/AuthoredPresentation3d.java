/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.spatial3d;

import io.github.glynch.jscene3d.math.Color;
import io.github.glynch.jscene3d.project.component.PropertyId;
import io.github.glynch.jscene3d.project.spatial3d.descriptor.Spatial3dDescriptors;
import io.github.glynch.jscene3d.project.value.ProjectValue;
import io.github.glynch.jscene3d.project.value.ResourceReference;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.joml.Vector2f;
import org.joml.Vector3f;

/** Decodes structurally validated project values into exact 3D presentation configuration. */
final class AuthoredPresentation3d {
    /** Prevents construction of this stateless decoder. */
    private AuthoredPresentation3d() {
        throw new AssertionError("AuthoredPresentation3d cannot be instantiated");
    }

    /** Decodes complete perspective-camera configuration. */
    static Camera camera(Map<PropertyId, ProjectValue> properties) {
        return new Camera(
                number(properties, Spatial3dDescriptors.fieldOfViewDegreesProperty()),
                number(properties, Spatial3dDescriptors.nearProperty()),
                number(properties, Spatial3dDescriptors.farProperty()),
                bool(properties, Spatial3dDescriptors.primaryProperty()));
    }

    /** Decodes complete directional-light configuration. */
    static Light light(Map<PropertyId, ProjectValue> properties) {
        Vector3f channels = vector3(properties, Spatial3dDescriptors.colorProperty());
        Color color = Color.linear(channels.x, channels.y, channels.z);
        return new Light(
                color,
                number(properties, Spatial3dDescriptors.intensityProperty()),
                vector3(properties, Spatial3dDescriptors.targetProperty()));
    }

    /** Decodes complete mesh-renderer configuration before runtime resource acquisition. */
    static MeshRenderer meshRenderer(Map<PropertyId, ProjectValue> properties) {
        return new MeshRenderer(
                reference(properties, Spatial3dDescriptors.meshProperty()),
                reference(properties, Spatial3dDescriptors.materialProperty()),
                bool(properties, Spatial3dDescriptors.visibleProperty()));
    }

    /** Decodes complete billboard-renderer configuration before runtime resource acquisition. */
    static BillboardRenderer billboardRenderer(Map<PropertyId, ProjectValue> properties) {
        return new BillboardRenderer(
                reference(properties, Spatial3dDescriptors.materialProperty()),
                vector2(properties, Spatial3dDescriptors.sizeProperty()),
                vector2(properties, Spatial3dDescriptors.anchorProperty()),
                text(properties, Spatial3dDescriptors.alignmentProperty()),
                bool(properties, Spatial3dDescriptors.visibleProperty()));
    }

    /** Requires one finite numeric property. */
    private static float number(Map<PropertyId, ProjectValue> properties, PropertyId property) {
        ProjectValue value = required(properties, property);
        if (!(value instanceof ProjectValue.NumberValue number)) {
            throw new IllegalArgumentException(property + " must be a number");
        }
        float result = number.value().floatValue();
        if (!Float.isFinite(result)) {
            throw new IllegalArgumentException(property + " must be finite");
        }
        return result;
    }

    /** Requires one boolean property. */
    private static boolean bool(Map<PropertyId, ProjectValue> properties, PropertyId property) {
        ProjectValue value = required(properties, property);
        if (!(value instanceof ProjectValue.BooleanValue bool)) {
            throw new IllegalArgumentException(property + " must be a boolean");
        }
        return bool.value();
    }

    /** Requires one text property. */
    private static String text(Map<PropertyId, ProjectValue> properties, PropertyId property) {
        ProjectValue value = required(properties, property);
        if (!(value instanceof ProjectValue.TextValue(String text))) {
            throw new IllegalArgumentException(property + " must be text");
        }
        return text;
    }

    /** Requires one resource-reference property. */
    private static ResourceReference reference(Map<PropertyId, ProjectValue> properties, PropertyId property) {
        ProjectValue value = required(properties, property);
        if (!(value instanceof ProjectValue.ReferenceValue reference)) {
            throw new IllegalArgumentException(property + " must be a resource reference");
        }
        return reference.reference();
    }

    /** Requires one exact finite three-number vector. */
    private static Vector3f vector3(Map<PropertyId, ProjectValue> properties, PropertyId property) {
        ProjectValue value = required(properties, property);
        if (!(value instanceof ProjectValue.ArrayValue array) || array.values().size() != 3) {
            throw new IllegalArgumentException(property + " must contain exactly three numbers");
        }
        List<ProjectValue> values = array.values();
        return new Vector3f(entry(values, 0, property), entry(values, 1, property), entry(values, 2, property));
    }

    /** Requires one exact finite two-number vector. */
    private static Vector2f vector2(Map<PropertyId, ProjectValue> properties, PropertyId property) {
        ProjectValue value = required(properties, property);
        if (!(value instanceof ProjectValue.ArrayValue array) || array.values().size() != 2) {
            throw new IllegalArgumentException(property + " must contain exactly two numbers");
        }
        List<ProjectValue> values = array.values();
        return new Vector2f(entry(values, 0, property), entry(values, 1, property));
    }

    /** Requires one finite vector entry. */
    private static float entry(List<ProjectValue> values, int index, PropertyId property) {
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

    /** Returns one effective property while rejecting an incomplete construction context. */
    private static ProjectValue required(Map<PropertyId, ProjectValue> properties, PropertyId property) {
        return Objects.requireNonNull(
                Objects.requireNonNull(properties, "properties").get(property), property + " property");
    }

    /** Complete camera configuration. */
    record Camera(float fieldOfViewDegrees, float near, float far, boolean primary) {}

    /** Complete light configuration. */
    record Light(Color color, float intensity, Vector3f target) {}

    /** Complete mesh-renderer configuration. */
    record MeshRenderer(ResourceReference mesh, ResourceReference material, boolean visible) {}

    /** Complete billboard-renderer configuration. */
    record BillboardRenderer(
            ResourceReference material, Vector2f size, Vector2f anchor, String alignment, boolean visible) {}
}
