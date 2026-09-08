/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.physics3d;

import io.github.glynch.jscene3d.project.component.ComponentLifecycle;
import io.github.glynch.jscene3d.project.component.ComponentMultiplicity;
import io.github.glynch.jscene3d.project.component.ComponentType;
import io.github.glynch.jscene3d.project.component.ComponentTypeDescriptor;
import io.github.glynch.jscene3d.project.component.EndpointId;
import io.github.glynch.jscene3d.project.component.PropertyId;
import io.github.glynch.jscene3d.project.extension.DescriptorPresentation;
import io.github.glynch.jscene3d.project.extension.EndpointDescriptor;
import io.github.glynch.jscene3d.project.extension.ExtensionDescriptor;
import io.github.glynch.jscene3d.project.extension.ProjectValueKind;
import io.github.glynch.jscene3d.project.extension.PropertyDescriptor;
import io.github.glynch.jscene3d.project.extension.RegisteredType;
import io.github.glynch.jscene3d.project.extension.RegisteredTypeDescriptor;
import io.github.glynch.jscene3d.project.extension.RegisteredTypeScope;
import io.github.glynch.jscene3d.project.spatial3d.Spatial3dDescriptors;
import io.github.glynch.jscene3d.project.value.ProjectValue;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Stable safe metadata for JScene3D's built-in three-dimensional collision components and resources. */
public final class Physics3dDescriptors {
    private static final String EXTENSION_ID = "io.github.glynch.jscene3d.physics3d";
    private static final ComponentType SHAPE_TYPE = type("collision-shape-3d");
    private static final ComponentType STATIC_BODY_TYPE = type("static-body-3d");
    private static final ComponentType SENSOR_TYPE = type("collision-sensor-3d");
    private static final ComponentType CHARACTER_BODY_TYPE = type("character-body-3d");
    private static final RegisteredType BOX_RESOURCE_TYPE = resourceType("box-collision-shape-3d");
    private static final RegisteredType SPHERE_RESOURCE_TYPE = resourceType("sphere-collision-shape-3d");
    private static final RegisteredType CAPSULE_RESOURCE_TYPE = resourceType("capsule-collision-shape-3d");
    private static final RegisteredType TRIANGLE_MESH_RESOURCE_TYPE = resourceType("triangle-mesh-collision-shape-3d");
    private static final RegisteredType OVERLAP_PAYLOAD_TYPE = resourceType("collision-overlap-3d");

    private static final PropertyId SHAPE = new PropertyId("shape");
    private static final PropertyId LOCAL_POSITION = new PropertyId("local-position");
    private static final PropertyId LOCAL_ORIENTATION = new PropertyId("local-orientation");
    private static final PropertyId CATEGORY_BITS = new PropertyId("category-bits");
    private static final PropertyId MASK_BITS = new PropertyId("mask-bits");
    private static final PropertyId SHAPES = new PropertyId("shapes");
    private static final PropertyId GRAVITY = new PropertyId("gravity");
    private static final PropertyId JUMP_SPEED = new PropertyId("jump-speed");
    private static final PropertyId MAXIMUM_STEP_HEIGHT = new PropertyId("maximum-step-height");
    private static final PropertyId GROUND_SNAP_DISTANCE = new PropertyId("ground-snap-distance");
    private static final EndpointId OVERLAP_ENTERED = new EndpointId("overlap-entered");
    private static final EndpointId OVERLAP_EXITED = new EndpointId("overlap-exited");
    private static final Set<ComponentLifecycle> COLLISION_LIFECYCLE =
            Set.of(ComponentLifecycle.ACTIVATED, ComponentLifecycle.DEACTIVATED, ComponentLifecycle.DESTROYED);
    private static final ExtensionDescriptor DESCRIPTOR = createDescriptor();

    /** Prevents construction of this stable metadata container. */
    private Physics3dDescriptors() {
        throw new AssertionError("Physics3dDescriptors cannot be instantiated");
    }

    /**
     * Returns the extension identity.
     *
     * @return stable extension identity
     */
    public static String extensionId() {
        return EXTENSION_ID;
    }

    /**
     * Returns the collision-shape component type.
     *
     * @return exact version-one collision-shape component type
     */
    public static ComponentType collisionShapeType() {
        return SHAPE_TYPE;
    }

    /**
     * Returns the static-body component type.
     *
     * @return exact version-one static-body component type
     */
    public static ComponentType staticBodyType() {
        return STATIC_BODY_TYPE;
    }

    /**
     * Returns the collision-sensor component type.
     *
     * @return exact version-one collision-sensor component type
     */
    public static ComponentType collisionSensorType() {
        return SENSOR_TYPE;
    }

    /**
     * Returns the explicitly moved character-body component type.
     *
     * @return exact version-one character-body component type
     */
    public static ComponentType characterBodyType() {
        return CHARACTER_BODY_TYPE;
    }

    /**
     * Returns the box collision resource type.
     *
     * @return exact version-one box collision resource type
     */
    public static RegisteredType boxResourceType() {
        return BOX_RESOURCE_TYPE;
    }

    /**
     * Returns the sphere collision resource type.
     *
     * @return exact version-one sphere collision resource type
     */
    public static RegisteredType sphereResourceType() {
        return SPHERE_RESOURCE_TYPE;
    }

    /**
     * Returns the capsule collision resource type.
     *
     * @return exact version-one capsule collision resource type
     */
    public static RegisteredType capsuleResourceType() {
        return CAPSULE_RESOURCE_TYPE;
    }

    /**
     * Returns the static triangle-mesh collision resource type.
     *
     * @return exact version-one triangle-mesh collision resource type
     */
    public static RegisteredType triangleMeshResourceType() {
        return TRIANGLE_MESH_RESOURCE_TYPE;
    }

    /**
     * Returns the overlap payload type.
     *
     * @return runtime payload identity shared by sensor signals and behavior actions
     */
    public static RegisteredType overlapPayloadType() {
        return OVERLAP_PAYLOAD_TYPE;
    }

    /**
     * Returns the shape-resource property identity.
     *
     * @return shape-resource property identity
     */
    public static PropertyId shapeProperty() {
        return SHAPE;
    }

    /**
     * Returns the shape-local-position property identity.
     *
     * @return shape-local-position property identity
     */
    public static PropertyId localPositionProperty() {
        return LOCAL_POSITION;
    }

    /**
     * Returns the shape-local-orientation property identity.
     *
     * @return shape-local-orientation property identity
     */
    public static PropertyId localOrientationProperty() {
        return LOCAL_ORIENTATION;
    }

    /**
     * Returns the shape-category property identity.
     *
     * @return shape-category property identity
     */
    public static PropertyId categoryBitsProperty() {
        return CATEGORY_BITS;
    }

    /**
     * Returns the shape-mask property identity.
     *
     * @return shape-mask property identity
     */
    public static PropertyId maskBitsProperty() {
        return MASK_BITS;
    }

    /**
     * Returns the collision-object membership property identity.
     *
     * @return collision-object member-shape property identity
     */
    public static PropertyId shapesProperty() {
        return SHAPES;
    }

    /**
     * Returns the character gravity property identity.
     *
     * @return character gravity property identity
     */
    public static PropertyId gravityProperty() {
        return GRAVITY;
    }

    /**
     * Returns the character jump-speed property identity.
     *
     * @return character jump-speed property identity
     */
    public static PropertyId jumpSpeedProperty() {
        return JUMP_SPEED;
    }

    /**
     * Returns the character maximum-step-height property identity.
     *
     * @return character maximum-step-height property identity
     */
    public static PropertyId maximumStepHeightProperty() {
        return MAXIMUM_STEP_HEIGHT;
    }

    /**
     * Returns the character ground-snap-distance property identity.
     *
     * @return character ground-snap-distance property identity
     */
    public static PropertyId groundSnapDistanceProperty() {
        return GROUND_SNAP_DISTANCE;
    }

    /**
     * Returns the overlap-entered signal identity.
     *
     * @return precise-overlap-entered signal identity
     */
    public static EndpointId overlapEnteredSignal() {
        return OVERLAP_ENTERED;
    }

    /**
     * Returns the overlap-exited signal identity.
     *
     * @return precise-overlap-exited signal identity
     */
    public static EndpointId overlapExitedSignal() {
        return OVERLAP_EXITED;
    }

    /**
     * Returns all first-profile 3D physics metadata.
     *
     * @return safe immutable metadata for all first-profile 3D physics types
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
                DescriptorPresentation.named("JScene3D 3D physics"),
                List.of(
                        boxResourceDescriptor(),
                        sphereResourceDescriptor(),
                        capsuleResourceDescriptor(),
                        triangleMeshResourceDescriptor()),
                List.of(shapeDescriptor(), staticBodyDescriptor(), sensorDescriptor(), characterBodyDescriptor()));
    }

    /** Describes one immutable box collision resource. */
    private static RegisteredTypeDescriptor boxResourceDescriptor() {
        return resourceDescriptor(
                BOX_RESOURCE_TYPE,
                "Box collision shape 3D",
                List.of(
                        positiveNumber("width", "Width"),
                        positiveNumber("height", "Height"),
                        positiveNumber("depth", "Depth")));
    }

    /** Describes one immutable sphere collision resource. */
    private static RegisteredTypeDescriptor sphereResourceDescriptor() {
        return resourceDescriptor(
                SPHERE_RESOURCE_TYPE, "Sphere collision shape 3D", List.of(positiveNumber("radius", "Radius")));
    }

    /** Describes one immutable Y-aligned capsule collision resource. */
    private static RegisteredTypeDescriptor capsuleResourceDescriptor() {
        return resourceDescriptor(
                CAPSULE_RESOURCE_TYPE,
                "Capsule collision shape 3D",
                List.of(positiveNumber("radius", "Radius"), nonNegativeNumber("segment-length", "Segment length")));
    }

    /** Describes one payload-backed immutable triangle collision mesh for static geometry. */
    private static RegisteredTypeDescriptor triangleMeshResourceDescriptor() {
        return resourceDescriptor(
                TRIANGLE_MESH_RESOURCE_TYPE,
                "Triangle mesh collision shape 3D",
                List.of(PropertyDescriptor.required(
                        "payload",
                        ProjectValueKind.REFERENCE,
                        DescriptorPresentation.named("Payload"),
                        Map.of(),
                        Set.of())));
    }

    /** Creates inert safe metadata for one collision resource. */
    private static RegisteredTypeDescriptor resourceDescriptor(
            RegisteredType type, String name, List<PropertyDescriptor> properties) {
        return new RegisteredTypeDescriptor(
                type,
                RegisteredTypeScope.RESOURCE,
                DescriptorPresentation.named(name),
                properties,
                List.of(),
                List.of(),
                List.of());
    }

    /** Describes one independently transformed member shape. */
    private static ComponentTypeDescriptor shapeDescriptor() {
        return ComponentTypeDescriptor.builder(SHAPE_TYPE, DescriptorPresentation.named("Collision Shape 3D"))
                .properties(List.of(
                        PropertyDescriptor.required(
                                SHAPE.value(),
                                ProjectValueKind.REFERENCE,
                                DescriptorPresentation.named("Shape"),
                                Map.of(),
                                Set.of()),
                        vectorProperty(LOCAL_POSITION, "Local position", numbers(0.0F, 0.0F, 0.0F), "vector3"),
                        vectorProperty(
                                LOCAL_ORIENTATION, "Local orientation", numbers(0.0F, 0.0F, 0.0F, 1.0F), "quaternion"),
                        integerProperty(CATEGORY_BITS, "Category bits", 1),
                        integerProperty(MASK_BITS, "Mask bits", -1)))
                .multiplicity(ComponentMultiplicity.MULTIPLE)
                .build();
    }

    /** Describes one static collision object. */
    private static ComponentTypeDescriptor staticBodyDescriptor() {
        return collisionObject(STATIC_BODY_TYPE, "Static Body 3D")
                .conflicts(Set.of(SENSOR_TYPE.id(), CHARACTER_BODY_TYPE.id()))
                .build();
    }

    /** Describes one non-blocking collision sensor and its typed transitions. */
    private static ComponentTypeDescriptor sensorDescriptor() {
        EndpointDescriptor entered = EndpointDescriptor.withPayload(
                OVERLAP_ENTERED.value(), OVERLAP_PAYLOAD_TYPE, DescriptorPresentation.named("Overlap entered"));
        EndpointDescriptor exited = EndpointDescriptor.withPayload(
                OVERLAP_EXITED.value(), OVERLAP_PAYLOAD_TYPE, DescriptorPresentation.named("Overlap exited"));
        return collisionObject(SENSOR_TYPE, "Collision Sensor 3D")
                .conflicts(Set.of(STATIC_BODY_TYPE.id(), CHARACTER_BODY_TYPE.id()))
                .signals(List.of(entered, exited))
                .build();
    }

    /** Describes one explicitly moved character body with authored gameplay-scale movement settings. */
    private static ComponentTypeDescriptor characterBodyDescriptor() {
        CharacterBody3dSettings defaults = CharacterBody3dSettings.DEFAULT;
        List<PropertyDescriptor> properties = new ArrayList<>(collisionMembershipProperties());
        properties.add(nonNegativeNumber(GRAVITY, "Gravity", defaults.gravity()));
        properties.add(nonNegativeNumber(JUMP_SPEED, "Jump speed", defaults.jumpSpeed()));
        properties.add(nonNegativeNumber(MAXIMUM_STEP_HEIGHT, "Maximum step height", defaults.maximumStepHeight()));
        properties.add(nonNegativeNumber(GROUND_SNAP_DISTANCE, "Ground snap distance", defaults.groundSnapDistance()));
        return collisionObject(CHARACTER_BODY_TYPE, "Character Body 3D")
                .properties(properties)
                .conflicts(Set.of(STATIC_BODY_TYPE.id(), SENSOR_TYPE.id()))
                .build();
    }

    /** Starts one collision-object descriptor requiring a sibling Transform3d. */
    private static ComponentTypeDescriptor.Builder collisionObject(ComponentType type, String name) {
        return ComponentTypeDescriptor.builder(type, DescriptorPresentation.named(name))
                .properties(collisionMembershipProperties())
                .requiredCapabilities(Set.of(Spatial3dDescriptors.spatialCapability()))
                .lifecycle(COLLISION_LIFECYCLE);
    }

    /** Creates the stable explicit collision-shape membership property. */
    private static List<PropertyDescriptor> collisionMembershipProperties() {
        return List.of(PropertyDescriptor.requiredArray(
                SHAPES.value(),
                ProjectValueKind.COMPONENT_TARGET,
                DescriptorPresentation.described("Shapes", "Explicit sibling collision-shape membership"),
                Map.of()));
    }

    /** Creates one required positive numeric resource property. */
    private static PropertyDescriptor positiveNumber(String id, String name) {
        return PropertyDescriptor.required(
                id,
                ProjectValueKind.NUMBER,
                DescriptorPresentation.named(name),
                Map.of("minimum-exclusive", number(0.0F)),
                Set.of());
    }

    /** Creates one required non-negative numeric resource property. */
    private static PropertyDescriptor nonNegativeNumber(String id, String name) {
        return PropertyDescriptor.required(
                id,
                ProjectValueKind.NUMBER,
                DescriptorPresentation.named(name),
                Map.of("minimum", number(0.0F)),
                Set.of());
    }

    /** Creates one optional non-negative numeric component property. */
    private static PropertyDescriptor nonNegativeNumber(PropertyId id, String name, float defaultValue) {
        return PropertyDescriptor.optionalWithDefault(
                id.value(),
                ProjectValueKind.NUMBER,
                number(defaultValue),
                DescriptorPresentation.named(name),
                Map.of("minimum", number(0.0F)),
                Set.of());
    }

    /** Creates one array-backed authored spatial property. */
    private static PropertyDescriptor vectorProperty(
            PropertyId id, String name, ProjectValue defaultValue, String semantic) {
        return PropertyDescriptor.optionalWithDefault(
                id.value(),
                ProjectValueKind.ARRAY,
                defaultValue,
                DescriptorPresentation.named(name),
                Map.of("semantic", new ProjectValue.TextValue(semantic)),
                Set.of());
    }

    /** Creates one integer-valued authored property. */
    private static PropertyDescriptor integerProperty(PropertyId id, String name, int value) {
        return PropertyDescriptor.optionalWithDefault(
                id.value(),
                ProjectValueKind.NUMBER,
                new ProjectValue.NumberValue(BigDecimal.valueOf(value)),
                DescriptorPresentation.named(name),
                Map.of("semantic", new ProjectValue.TextValue("integer")),
                Set.of());
    }

    /** Creates one portable finite decimal. */
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

    /** Creates an exact version-one resource or payload type. */
    private static RegisteredType resourceType(String localName) {
        return new RegisteredType(EXTENSION_ID + '/' + localName, 1);
    }
}
