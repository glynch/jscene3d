/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.spatial3d;

import io.github.glynch.jscene3d.project.component.CapabilityId;
import io.github.glynch.jscene3d.project.component.ComponentLifecycle;
import io.github.glynch.jscene3d.project.component.ComponentMultiplicity;
import io.github.glynch.jscene3d.project.component.ComponentSpatialDomain;
import io.github.glynch.jscene3d.project.component.ComponentType;
import io.github.glynch.jscene3d.project.component.ComponentTypeDescriptor;
import io.github.glynch.jscene3d.project.component.PropertyId;
import io.github.glynch.jscene3d.project.extension.DescriptorPresentation;
import io.github.glynch.jscene3d.project.extension.ExtensionDescriptor;
import io.github.glynch.jscene3d.project.extension.ProjectValueKind;
import io.github.glynch.jscene3d.project.extension.PropertyDescriptor;
import io.github.glynch.jscene3d.project.extension.RegisteredType;
import io.github.glynch.jscene3d.project.extension.RegisteredTypeDescriptor;
import io.github.glynch.jscene3d.project.extension.RegisteredTypeScope;
import io.github.glynch.jscene3d.project.value.ProjectValue;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Stable safe metadata for JScene3D's built-in three-dimensional components. */
public final class Spatial3dDescriptors {
    private static final String EXTENSION_ID = "io.github.glynch.jscene3d.spatial3d";

    private static final ComponentType TRANSFORM_TYPE = type("transform-3d");
    private static final ComponentType PERSPECTIVE_CAMERA_TYPE = type("perspective-camera-3d");
    private static final ComponentType DIRECTIONAL_LIGHT_TYPE = type("directional-light-3d");
    private static final ComponentType MESH_RENDERER_TYPE = type("mesh-renderer-3d");
    private static final RegisteredType MESH_RESOURCE_TYPE = resourceType("mesh-resource");
    private static final RegisteredType MATERIAL_RESOURCE_TYPE = resourceType("standard-material-resource");
    private static final RegisteredType TEXTURE_RESOURCE_TYPE = resourceType("rgba8-texture-resource");
    private static final RegisteredType BASIC_MATERIAL_RESOURCE_TYPE = resourceType("basic-material-resource");

    private static final CapabilityId SPATIAL_CAPABILITY = new CapabilityId(EXTENSION_ID + "/spatial-3d");

    private static final PropertyId POSITION = new PropertyId("position");
    private static final PropertyId ORIENTATION = new PropertyId("orientation");
    private static final PropertyId SCALE = new PropertyId("scale");
    private static final PropertyId FIELD_OF_VIEW_DEGREES = new PropertyId("field-of-view-degrees");
    private static final PropertyId NEAR = new PropertyId("near");
    private static final PropertyId FAR = new PropertyId("far");
    private static final PropertyId PRIMARY = new PropertyId("primary");
    private static final PropertyId COLOR = new PropertyId("color");
    private static final PropertyId INTENSITY = new PropertyId("intensity");
    private static final PropertyId TARGET = new PropertyId("target");
    private static final PropertyId MESH = new PropertyId("mesh");
    private static final PropertyId MATERIAL = new PropertyId("material");
    private static final PropertyId VISIBLE = new PropertyId("visible");
    private static final String PAYLOAD = "payload";

    private static final Set<ComponentLifecycle> PRESENTATION_LIFECYCLE =
            Set.of(ComponentLifecycle.ACTIVATED, ComponentLifecycle.DEACTIVATED);
    private static final ExtensionDescriptor DESCRIPTOR = createDescriptor();

    /** Prevents construction of this stable metadata container. */
    private Spatial3dDescriptors() {
        throw new AssertionError("Spatial3dDescriptors cannot be instantiated");
    }

    /**
     * Returns the stable extension identity.
     *
     * @return extension identity
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
     * Returns the exact version-one perspective-camera component type.
     *
     * @return perspective-camera component type
     */
    public static ComponentType perspectiveCameraType() {
        return PERSPECTIVE_CAMERA_TYPE;
    }

    /**
     * Returns the exact version-one directional-light component type.
     *
     * @return directional-light component type
     */
    public static ComponentType directionalLightType() {
        return DIRECTIONAL_LIGHT_TYPE;
    }

    /**
     * Returns the exact version-one mesh-renderer component type.
     *
     * @return mesh-renderer component type
     */
    public static ComponentType meshRendererType() {
        return MESH_RENDERER_TYPE;
    }

    /**
     * Returns the exact version-one binary mesh resource type.
     *
     * @return mesh resource type
     */
    public static RegisteredType meshResourceType() {
        return MESH_RESOURCE_TYPE;
    }

    /**
     * Returns the exact version-one standard-material resource type.
     *
     * @return material resource type
     */
    public static RegisteredType materialResourceType() {
        return MATERIAL_RESOURCE_TYPE;
    }

    /**
     * Returns the exact version-one RGBA8 texture resource type.
     *
     * @return texture resource type
     */
    public static RegisteredType textureResourceType() {
        return TEXTURE_RESOURCE_TYPE;
    }

    /**
     * Returns the exact version-one unlit material resource type.
     *
     * @return basic-material resource type
     */
    public static RegisteredType basicMaterialResourceType() {
        return BASIC_MATERIAL_RESOURCE_TYPE;
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
     * @return position property identity
     */
    public static PropertyId positionProperty() {
        return POSITION;
    }

    /**
     * Returns the local-orientation property identity.
     *
     * @return orientation property identity
     */
    public static PropertyId orientationProperty() {
        return ORIENTATION;
    }

    /**
     * Returns the local-scale property identity.
     *
     * @return scale property identity
     */
    public static PropertyId scaleProperty() {
        return SCALE;
    }

    /**
     * Returns the perspective vertical-field-of-view property identity.
     *
     * @return field-of-view property identity
     */
    public static PropertyId fieldOfViewDegreesProperty() {
        return FIELD_OF_VIEW_DEGREES;
    }

    /**
     * Returns the near clipping-plane property identity.
     *
     * @return near-plane property identity
     */
    public static PropertyId nearProperty() {
        return NEAR;
    }

    /**
     * Returns the far clipping-plane property identity.
     *
     * @return far-plane property identity
     */
    public static PropertyId farProperty() {
        return FAR;
    }

    /**
     * Returns the primary-camera-selection property identity.
     *
     * @return primary-camera property identity
     */
    public static PropertyId primaryProperty() {
        return PRIMARY;
    }

    /**
     * Returns the light-color property identity.
     *
     * @return light-color property identity
     */
    public static PropertyId colorProperty() {
        return COLOR;
    }

    /**
     * Returns the light-intensity property identity.
     *
     * @return light-intensity property identity
     */
    public static PropertyId intensityProperty() {
        return INTENSITY;
    }

    /**
     * Returns the directional-light target property identity.
     *
     * @return light-target property identity
     */
    public static PropertyId targetProperty() {
        return TARGET;
    }

    /**
     * Returns the mesh-resource property identity.
     *
     * @return mesh-resource property identity
     */
    public static PropertyId meshProperty() {
        return MESH;
    }

    /**
     * Returns the material-resource property identity.
     *
     * @return material-resource property identity
     */
    public static PropertyId materialProperty() {
        return MATERIAL;
    }

    /**
     * Returns the local mesh-visibility property identity.
     *
     * @return mesh-visibility property identity
     */
    public static PropertyId visibleProperty() {
        return VISIBLE;
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
        return new ExtensionDescriptor(
                EXTENSION_ID,
                "1.0.0",
                ">=0.1.0 <0.2.0",
                DescriptorPresentation.named("JScene3D 3D components"),
                List.of(
                        meshResourceDescriptor(),
                        materialResourceDescriptor(),
                        textureResourceDescriptor(),
                        basicMaterialResourceDescriptor()),
                List.of(transformDescriptor(), cameraDescriptor(), lightDescriptor(), meshRendererDescriptor()));
    }

    /** Describes one geometry document referencing an opaque binary payload. */
    private static RegisteredTypeDescriptor meshResourceDescriptor() {
        return resourceDescriptor(
                MESH_RESOURCE_TYPE,
                "Mesh resource",
                "Immutable renderer-independent vertex and index data",
                List.of(PropertyDescriptor.required(
                        PAYLOAD,
                        ProjectValueKind.REFERENCE,
                        DescriptorPresentation.described("Payload", "Binary mesh payload"),
                        Map.of(),
                        Set.of())));
    }

    /** Describes one texture-free metallic-roughness material. */
    private static RegisteredTypeDescriptor materialResourceDescriptor() {
        return resourceDescriptor(
                MATERIAL_RESOURCE_TYPE,
                "Standard material resource",
                "Immutable texture-free metallic-roughness material",
                List.of(
                        arrayProperty("color", "Color", "Linear-sRGB base color", numbers(1.0F, 1.0F, 1.0F)),
                        numberProperty("metalness", "Metalness", "Metallic contribution", 0.0F),
                        numberProperty("roughness", "Roughness", "Perceptual surface roughness", 1.0F),
                        arrayProperty("emissive", "Emissive", "Linear-sRGB emissive color", numbers(0.0F, 0.0F, 0.0F)),
                        numberProperty("emissive-intensity", "Emissive intensity", "Emissive multiplier", 1.0F),
                        numberProperty("opacity", "Opacity", "Surface opacity", 1.0F),
                        textProperty("alpha-mode", "Alpha mode", "Opaque, mask, or blend", "opaque"),
                        numberProperty("alpha-cutoff", "Alpha cutoff", "Masked-alpha cutoff", 0.5F),
                        textProperty("side", "Side", "Front, back, or double", "front"),
                        booleanProperty(
                                "vertex-colors", "Vertex colors", "Multiply by the geometry color attribute", false)));
    }

    /** Describes one raw RGBA8 image and its complete sampler state. */
    private static RegisteredTypeDescriptor textureResourceDescriptor() {
        return resourceDescriptor(
                TEXTURE_RESOURCE_TYPE,
                "RGBA8 texture resource",
                "Immutable renderer-independent RGBA8 image and sampler",
                List.of(
                        PropertyDescriptor.required(
                                PAYLOAD,
                                ProjectValueKind.REFERENCE,
                                DescriptorPresentation.described("Payload", "Raw RGBA8 pixel payload"),
                                Map.of(),
                                Set.of()),
                        PropertyDescriptor.required(
                                "width",
                                ProjectValueKind.NUMBER,
                                DescriptorPresentation.named("Width"),
                                Map.of(),
                                Set.of()),
                        PropertyDescriptor.required(
                                "height",
                                ProjectValueKind.NUMBER,
                                DescriptorPresentation.named("Height"),
                                Map.of(),
                                Set.of()),
                        textProperty("color-space", "Color space", "sRGB or linear channel interpretation", "srgb"),
                        textProperty(
                                "minification-filter",
                                "Minification filter",
                                "Texture minification and mipmap filter",
                                "linear-mipmap-linear"),
                        textProperty(
                                "magnification-filter",
                                "Magnification filter",
                                "Texture magnification filter",
                                "linear"),
                        textProperty("horizontal-wrap", "Horizontal wrap", "Horizontal address mode", "clamp-to-edge"),
                        textProperty("vertical-wrap", "Vertical wrap", "Vertical address mode", "clamp-to-edge"),
                        textProperty(
                                "coordinate-origin",
                                "Coordinate origin",
                                "Image corner represented by texture coordinate zero",
                                "bottom-left"),
                        textProperty("mipmap-mode", "Mipmap mode", "Renderer mipmap generation policy", "generate")));
    }

    /** Describes one unlit material with an optional shared base-color texture. */
    private static RegisteredTypeDescriptor basicMaterialResourceDescriptor() {
        return resourceDescriptor(
                BASIC_MATERIAL_RESOURCE_TYPE,
                "Basic material resource",
                "Immutable unlit material with an optional base-color texture",
                List.of(
                        arrayProperty("color", "Color", "Linear-sRGB base color", numbers(1.0F, 1.0F, 1.0F)),
                        numberProperty("opacity", "Opacity", "Surface opacity", 1.0F),
                        textProperty("alpha-mode", "Alpha mode", "Opaque, mask, or blend", "opaque"),
                        numberProperty("alpha-cutoff", "Alpha cutoff", "Masked-alpha cutoff", 0.5F),
                        textProperty("side", "Side", "Front, back, or double", "front"),
                        booleanProperty(
                                "vertex-colors", "Vertex colors", "Multiply by the geometry color attribute", false),
                        PropertyDescriptor.optional(
                                "color-map",
                                ProjectValueKind.REFERENCE,
                                DescriptorPresentation.described("Color map", "Shared RGBA8 texture resource"),
                                Map.of(),
                                Set.of())));
    }

    /** Creates one registered resource descriptor without executable endpoints. */
    private static RegisteredTypeDescriptor resourceDescriptor(
            RegisteredType type, String name, String description, List<PropertyDescriptor> properties) {
        return new RegisteredTypeDescriptor(
                type,
                RegisteredTypeScope.RESOURCE,
                DescriptorPresentation.described(name, description),
                properties,
                List.of(),
                List.of(),
                List.of());
    }

    /** Describes the unique primary spatial authority. */
    private static ComponentTypeDescriptor transformDescriptor() {
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
        return ComponentTypeDescriptor.builder(
                        TRANSFORM_TYPE, DescriptorPresentation.described("Transform 3D", "Primary 3D spatial state"))
                .properties(List.of(position, orientation, scale))
                .providedCapabilities(Set.of(SPATIAL_CAPABILITY))
                .spatialDomain(ComponentSpatialDomain.THREE_DIMENSIONAL)
                .build();
    }

    /** Describes an explicitly selectable perspective camera. */
    private static ComponentTypeDescriptor cameraDescriptor() {
        return presentation(PERSPECTIVE_CAMERA_TYPE, "Perspective Camera 3D", "Perspective projection")
                .properties(List.of(
                        numberProperty(
                                FIELD_OF_VIEW_DEGREES, "Field of view", "Vertical field of view in degrees", 60.0F),
                        numberProperty(NEAR, "Near", "Near clipping distance", 0.1F),
                        numberProperty(FAR, "Far", "Far clipping distance", 1000.0F),
                        booleanProperty(PRIMARY, "Primary", "Selects the authored primary camera", false)))
                .build();
    }

    /** Describes one transform-attached directional light. */
    private static ComponentTypeDescriptor lightDescriptor() {
        PropertyDescriptor color =
                vectorProperty(COLOR, "Color", "Linear-sRGB light color", numbers(1.0F, 1.0F, 1.0F), "color-linear");
        PropertyDescriptor target =
                vectorProperty(TARGET, "Target", "World-space target point", numbers(0.0F, 0.0F, 0.0F), "vector3");
        return presentation(DIRECTIONAL_LIGHT_TYPE, "Directional Light 3D", "Parallel illumination")
                .properties(List.of(
                        color, numberProperty(INTENSITY, "Intensity", "Linear intensity multiplier", 1.0F), target))
                .build();
    }

    /** Describes one shared-resource mesh presentation component. */
    private static ComponentTypeDescriptor meshRendererDescriptor() {
        return presentation(MESH_RENDERER_TYPE, "Mesh Renderer 3D", "Shared mesh and material presentation")
                .properties(List.of(
                        referenceProperty(MESH, "Mesh", "Immutable mesh resource"),
                        referenceProperty(MATERIAL, "Material", "Immutable material resource"),
                        booleanProperty(VISIBLE, "Visible", "Local instance visibility", true)))
                .multiplicity(ComponentMultiplicity.MULTIPLE)
                .build();
    }

    /** Starts one lifecycle-aware descriptor requiring a sibling Transform3d. */
    private static ComponentTypeDescriptor.Builder presentation(ComponentType type, String name, String description) {
        return ComponentTypeDescriptor.builder(type, DescriptorPresentation.described(name, description))
                .requiredCapabilities(Set.of(SPATIAL_CAPABILITY))
                .lifecycle(PRESENTATION_LIFECYCLE);
    }

    /** Creates one optional numeric property. */
    private static PropertyDescriptor numberProperty(PropertyId id, String name, String description, float value) {
        return numberProperty(id.value(), name, description, value);
    }

    /** Creates one optional numeric resource property. */
    private static PropertyDescriptor numberProperty(String id, String name, String description, float value) {
        return PropertyDescriptor.optionalWithDefault(
                id,
                ProjectValueKind.NUMBER,
                number(value),
                DescriptorPresentation.described(name, description),
                Map.of(),
                Set.of());
    }

    /** Creates one optional boolean property. */
    private static PropertyDescriptor booleanProperty(PropertyId id, String name, String description, boolean value) {
        return booleanProperty(id.value(), name, description, value);
    }

    /** Creates one optional boolean resource property. */
    private static PropertyDescriptor booleanProperty(String id, String name, String description, boolean value) {
        return PropertyDescriptor.optionalWithDefault(
                id,
                ProjectValueKind.BOOLEAN,
                new ProjectValue.BooleanValue(value),
                DescriptorPresentation.described(name, description),
                Map.of(),
                Set.of());
    }

    /** Creates one optional text resource property. */
    private static PropertyDescriptor textProperty(String id, String name, String description, String value) {
        return PropertyDescriptor.optionalWithDefault(
                id,
                ProjectValueKind.TEXT,
                new ProjectValue.TextValue(value),
                DescriptorPresentation.described(name, description),
                Map.of(),
                Set.of());
    }

    /** Creates one optional array resource property. */
    private static PropertyDescriptor arrayProperty(
            String id, String name, String description, ProjectValue.ArrayValue value) {
        return PropertyDescriptor.optionalWithDefault(
                id,
                ProjectValueKind.ARRAY,
                value,
                DescriptorPresentation.described(name, description),
                Map.of(),
                Set.of());
    }

    /** Creates one required resource-reference property accepting every portable namespace. */
    private static PropertyDescriptor referenceProperty(PropertyId id, String name, String description) {
        return PropertyDescriptor.required(
                id.value(),
                ProjectValueKind.REFERENCE,
                DescriptorPresentation.described(name, description),
                Map.of(),
                Set.of());
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

    /** Creates one portable number. */
    private static ProjectValue.NumberValue number(float value) {
        return new ProjectValue.NumberValue(new BigDecimal(Float.toString(value)));
    }

    /** Creates one portable numeric array. */
    private static ProjectValue.ArrayValue numbers(float... values) {
        List<ProjectValue> result = new ArrayList<>(values.length);
        for (float value : values) {
            result.add(number(value));
        }
        return new ProjectValue.ArrayValue(result);
    }

    /** Creates an exact version-one component type. */
    private static ComponentType type(String localName) {
        return ComponentType.of(EXTENSION_ID + '/' + localName, 1);
    }

    /** Creates an exact version-one non-component type. */
    private static RegisteredType resourceType(String localName) {
        return new RegisteredType(EXTENSION_ID + '/' + localName, 1);
    }
}
