/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.spatial3d;

import io.github.glynch.jscene3d.project.component.CapabilityId;
import io.github.glynch.jscene3d.project.component.ComponentSpatialDomain;
import io.github.glynch.jscene3d.project.component.ComponentType;
import io.github.glynch.jscene3d.project.component.ComponentTypeDescriptor;
import io.github.glynch.jscene3d.project.component.PropertyId;
import io.github.glynch.jscene3d.project.extension.DescriptorPresentation;
import io.github.glynch.jscene3d.project.extension.ExtensionDescriptor;
import io.github.glynch.jscene3d.project.extension.ProjectValueKind;
import io.github.glynch.jscene3d.project.extension.PropertyDescriptor;
import io.github.glynch.jscene3d.project.value.ProjectValue;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Stable safe metadata for JScene3D's built-in three-dimensional components. */
public final class Spatial3dDescriptors {
    /** Stable extension identity shared by safe metadata and executable factories. */
    private static final String EXTENSION_ID = "io.github.glynch.jscene3d.spatial3d";

    /** Exact version-one Transform3d component identity. */
    private static final ComponentType TRANSFORM_TYPE = ComponentType.of(EXTENSION_ID + "/transform-3d", 1);

    /** Capability supplied by the unique primary Transform3d component. */
    private static final CapabilityId SPATIAL_CAPABILITY = new CapabilityId(EXTENSION_ID + "/spatial-3d");

    /** Authored local-position property identity. */
    private static final PropertyId POSITION = new PropertyId("position");

    /** Authored local-orientation property identity. */
    private static final PropertyId ORIENTATION = new PropertyId("orientation");

    /** Authored local-scale property identity. */
    private static final PropertyId SCALE = new PropertyId("scale");

    /** Complete immutable safe extension descriptor. */
    private static final ExtensionDescriptor DESCRIPTOR = createDescriptor();

    /** Prevents construction of this stable metadata container. */
    private Spatial3dDescriptors() {
        throw new AssertionError("Spatial3dDescriptors cannot be instantiated");
    }

    /**
     * Returns the stable extension identity.
     *
     * @return extension identifier
     */
    public static String extensionId() {
        return EXTENSION_ID;
    }

    /**
     * Returns the exact version-one Transform3d component type.
     *
     * @return transform component type
     */
    public static ComponentType transformType() {
        return TRANSFORM_TYPE;
    }

    /**
     * Returns the capability supplied by Transform3d.
     *
     * @return spatial capability identity
     */
    public static CapabilityId spatialCapability() {
        return SPATIAL_CAPABILITY;
    }

    /**
     * Returns the local-position property identity.
     *
     * @return position property
     */
    public static PropertyId positionProperty() {
        return POSITION;
    }

    /**
     * Returns the local-orientation property identity.
     *
     * @return orientation property
     */
    public static PropertyId orientationProperty() {
        return ORIENTATION;
    }

    /**
     * Returns the local-scale property identity.
     *
     * @return scale property
     */
    public static PropertyId scaleProperty() {
        return SCALE;
    }

    /**
     * Returns safe immutable metadata for all built-in three-dimensional components.
     *
     * @return extension descriptor
     */
    public static ExtensionDescriptor extensionDescriptor() {
        return DESCRIPTOR;
    }

    /** Builds the immutable version-one extension descriptor. */
    private static ExtensionDescriptor createDescriptor() {
        PropertyDescriptor position =
                vectorProperty(POSITION, "Position", "Local XYZ translation", numbers(0.0F, 0.0F, 0.0F), "vector3");
        PropertyDescriptor orientation = vectorProperty(
                ORIENTATION,
                "Orientation",
                "Normalized local XYZW quaternion",
                numbers(0.0F, 0.0F, 0.0F, 1.0F),
                "quaternion");
        PropertyDescriptor scale =
                vectorProperty(SCALE, "Scale", "Local XYZ scale", numbers(1.0F, 1.0F, 1.0F), "vector3");
        ComponentTypeDescriptor transform = ComponentTypeDescriptor.builder(
                        TRANSFORM_TYPE, DescriptorPresentation.described("Transform 3D", "Primary 3D spatial state"))
                .properties(List.of(position, orientation, scale))
                .providedCapabilities(Set.of(SPATIAL_CAPABILITY))
                .spatialDomain(ComponentSpatialDomain.THREE_DIMENSIONAL)
                .build();
        return new ExtensionDescriptor(
                EXTENSION_ID,
                "1.0.0",
                ">=0.1.0 <0.2.0",
                DescriptorPresentation.named("JScene3D spatial components"),
                List.of(),
                List.of(transform));
    }

    /** Creates one array-backed spatial property with an editor semantic hint. */
    private static PropertyDescriptor vectorProperty(
            PropertyId id, String name, String description, ProjectValue defaultValue, String editorSemantic) {
        return PropertyDescriptor.optionalWithDefault(
                id.value(),
                ProjectValueKind.ARRAY,
                defaultValue,
                DescriptorPresentation.described(name, description),
                Map.of("semantic", new ProjectValue.TextValue(editorSemantic)),
                Set.of());
    }

    /** Creates one portable numeric array. */
    private static ProjectValue.ArrayValue numbers(float... values) {
        List<ProjectValue> numbers = new ArrayList<>(values.length);
        for (float value : values) {
            numbers.add(new ProjectValue.NumberValue(BigDecimal.valueOf(value)));
        }
        return new ProjectValue.ArrayValue(numbers);
    }
}
