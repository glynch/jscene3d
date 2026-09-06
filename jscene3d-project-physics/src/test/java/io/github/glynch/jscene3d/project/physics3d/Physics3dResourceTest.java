/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.physics3d;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.glynch.jscene3d.project.resource.ResourceDefinition;
import io.github.glynch.jscene3d.project.runtime.RuntimeResourceLoader;
import io.github.glynch.jscene3d.project.value.ProjectValue;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.junit.jupiter.api.Test;

/** Exercises collision-shape resource publication, loading, and lease-owned terminal state. */
final class Physics3dResourceTest {
    /** Writes and reconstructs one box resource without backend-specific data in the document. */
    @Test
    void writesAndLoadsBox() throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        Physics3dResourceWriter.writeBox(output, 2.0F, 3.0F, 4.0F);
        ResourceDefinition definition = new ResourceDefinition(
                URI.create("project:/collision/box.resource.json"),
                Physics3dDescriptors.boxResourceType(),
                Map.of("width", number(2.0F), "height", number(3.0F), "depth", number(4.0F)));

        BoxCollisionShape3dResource resource = boxLoader().load(definition, reference -> {
            throw new AssertionError("box resource has no payload");
        });

        assertThat(new String(output.toByteArray(), StandardCharsets.UTF_8))
                .contains("box-collision-shape-3d", "\"width\" : 2.0");
        assertThat(resource.width()).isEqualTo(2.0F);
        assertThat(resource.height()).isEqualTo(3.0F);
        assertThat(resource.depth()).isEqualTo(4.0F);
        resource.close();
        resource.close();
        assertThat(resource.isClosed()).isTrue();
        assertThatThrownBy(resource::width).isInstanceOf(IllegalStateException.class);
    }

    /** Writes and reconstructs one sphere resource and rejects invalid geometry. */
    @Test
    void writesAndLoadsSphere() throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        Physics3dResourceWriter.writeSphere(output, 1.5F);
        ResourceDefinition definition = new ResourceDefinition(
                URI.create("project:/collision/sphere.resource.json"),
                Physics3dDescriptors.sphereResourceType(),
                Map.of("radius", number(1.5F)));

        SphereCollisionShape3dResource resource = sphereLoader().load(definition, reference -> {
            throw new AssertionError("sphere resource has no payload");
        });

        assertThat(new String(output.toByteArray(), StandardCharsets.UTF_8))
                .contains("sphere-collision-shape-3d", "\"radius\" : 1.5");
        assertThat(resource.radius()).isEqualTo(1.5F);
        assertThatThrownBy(() -> new SphereCollisionShape3dResource(0.0F)).isInstanceOf(IllegalArgumentException.class);
        resource.close();
    }

    /** Covers non-finite geometry rejection and both asymmetric filter mismatch paths. */
    @Test
    void rejectsNonFiniteGeometryAndMismatchedFilters() {
        Vector3f nonFinitePosition = new Vector3f(Float.NaN, 0.0F, 0.0F);
        Quaternionf nonFiniteOrientation = new Quaternionf(Float.NaN, 0.0F, 0.0F, 1.0F);
        Quaternionf overflowingOrientation = new Quaternionf(Float.MAX_VALUE, 0.0F, 0.0F, 1.0F);
        CollisionFilter3d sensorRejectsBody = new CollisionFilter3d(1, 2);
        CollisionFilter3d bodyOutsideSensorMask = new CollisionFilter3d(4, 1);
        CollisionFilter3d sensorAcceptedByBody = new CollisionFilter3d(1, 4);
        CollisionFilter3d bodyRejectsSensor = new CollisionFilter3d(4, 2);

        assertThatThrownBy(() -> new BoxCollisionShape3dResource(Float.NaN, 1.0F, 1.0F))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> CollisionPreconditions.requireFinite(nonFinitePosition, "position"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> CollisionPreconditions.requireOrientation(nonFiniteOrientation, "orientation"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> CollisionPreconditions.requireOrientation(overflowingOrientation, "orientation"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThat(sensorRejectsBody.matches(bodyOutsideSensorMask)).isFalse();
        assertThat(sensorAcceptedByBody.matches(bodyRejectsSensor)).isFalse();
    }

    /** Returns the typed box loader from the public heterogeneous collection. */
    @SuppressWarnings("unchecked")
    private static RuntimeResourceLoader<BoxCollisionShape3dResource> boxLoader() {
        return (RuntimeResourceLoader<BoxCollisionShape3dResource>)
                Physics3dResourceLoaders.all().getFirst();
    }

    /** Returns the typed sphere loader from the public heterogeneous collection. */
    @SuppressWarnings("unchecked")
    private static RuntimeResourceLoader<SphereCollisionShape3dResource> sphereLoader() {
        return (RuntimeResourceLoader<SphereCollisionShape3dResource>)
                Physics3dResourceLoaders.all().get(1);
    }

    /** Creates one portable exact decimal. */
    private static ProjectValue.NumberValue number(float value) {
        return new ProjectValue.NumberValue(new BigDecimal(Float.toString(value)));
    }
}
