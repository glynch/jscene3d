/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.physics3d;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.glynch.jscene3d.project.component.ComponentLifecycle;
import io.github.glynch.jscene3d.project.component.ComponentMultiplicity;
import io.github.glynch.jscene3d.project.component.ComponentTypeDescriptor;
import io.github.glynch.jscene3d.project.extension.ProjectValueKind;
import io.github.glynch.jscene3d.project.extension.PropertyDescriptor;
import io.github.glynch.jscene3d.project.spatial3d.Spatial3dDescriptors;
import java.util.List;
import java.util.Objects;
import org.junit.jupiter.api.Test;

/** Verifies the safe descriptor contract for first-profile three-dimensional collision. */
final class Physics3dDescriptorsTest {
    /** Publishes shape, body, and sensor identities with explicit transform and membership declarations. */
    @Test
    void describesCollisionComponents() {
        List<ComponentTypeDescriptor> components =
                Physics3dDescriptors.extensionDescriptor().components();
        ComponentTypeDescriptor shape = components.get(0);
        ComponentTypeDescriptor body = components.get(1);
        ComponentTypeDescriptor sensor = components.get(2);
        ComponentTypeDescriptor character = components.get(3);
        PropertyDescriptor membership =
                Objects.requireNonNull(body.properties().get(Physics3dDescriptors.shapesProperty()));

        assertThat(components)
                .extracting(ComponentTypeDescriptor::type)
                .containsExactly(
                        Physics3dDescriptors.collisionShapeType(),
                        Physics3dDescriptors.staticBodyType(),
                        Physics3dDescriptors.collisionSensorType(),
                        Physics3dDescriptors.characterBodyType());
        assertThat(shape.multiplicity()).isEqualTo(ComponentMultiplicity.MULTIPLE);
        assertThat(membership.valueKind()).isEqualTo(ProjectValueKind.ARRAY);
        assertThat(membership.elementKind()).contains(ProjectValueKind.COMPONENT_TARGET);
        assertThat(body.requiredCapabilities()).containsExactly(Spatial3dDescriptors.spatialCapability());
        assertThat(sensor.requiredCapabilities()).containsExactly(Spatial3dDescriptors.spatialCapability());
        assertThat(character.requiredCapabilities()).containsExactly(Spatial3dDescriptors.spatialCapability());
        assertThat(character.properties().keySet())
                .containsExactly(
                        Physics3dDescriptors.shapesProperty(),
                        Physics3dDescriptors.gravityProperty(),
                        Physics3dDescriptors.jumpSpeedProperty(),
                        Physics3dDescriptors.maximumStepHeightProperty(),
                        Physics3dDescriptors.groundSnapDistanceProperty());
        assertThat(sensor.lifecycle())
                .containsExactlyInAnyOrder(
                        ComponentLifecycle.ACTIVATED, ComponentLifecycle.DEACTIVATED, ComponentLifecycle.DESTROYED);
    }

    /** Declares typed enter and exit signals identifying the same precise-overlap payload contract. */
    @Test
    void describesSensorSignals() {
        ComponentTypeDescriptor sensor =
                Physics3dDescriptors.extensionDescriptor().components().get(2);

        assertThat(sensor.signals().keySet())
                .containsExactly(
                        Physics3dDescriptors.overlapEnteredSignal(), Physics3dDescriptors.overlapExitedSignal());
        assertThat(sensor.signals().values())
                .allSatisfy(signal -> assertThat(signal.payload()).contains(Physics3dDescriptors.overlapPayloadType()));
    }

    /** Publishes every immutable collision resource declaration and its matching loader. */
    @Test
    void describesCollisionResources() {
        assertThat(Physics3dDescriptors.extensionDescriptor().types())
                .extracting(descriptor -> descriptor.type())
                .containsExactly(
                        Physics3dDescriptors.boxResourceType(),
                        Physics3dDescriptors.sphereResourceType(),
                        Physics3dDescriptors.capsuleResourceType(),
                        Physics3dDescriptors.triangleMeshResourceType());
        assertThat(Physics3dResourceLoaders.all())
                .extracting(loader -> loader.type())
                .containsExactly(
                        Physics3dDescriptors.boxResourceType(),
                        Physics3dDescriptors.sphereResourceType(),
                        Physics3dDescriptors.capsuleResourceType(),
                        Physics3dDescriptors.triangleMeshResourceType());
    }
}
