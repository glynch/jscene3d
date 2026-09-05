/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.physics;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.glynch.jscene3d.physics.shapes.BoxShape;
import io.github.glynch.jscene3d.physics.shapes.CapsuleShape;
import io.github.glynch.jscene3d.physics.shapes.SphereShape;
import io.github.glynch.jscene3d.physics.shapes.TriangleMeshShape;
import org.assertj.core.data.Offset;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.junit.jupiter.api.Test;

final class TriangleMeshCollisionTest {
    private static final float TOLERANCE = 1.0E-4F;
    private static final Quaternionf IDENTITY = new Quaternionf();
    private static final float[] FLOOR_POSITIONS = {
        -10.0F, 0.0F, -10.0F, 10.0F, 0.0F, -10.0F, 10.0F, 0.0F, 10.0F, -10.0F, 0.0F, 10.0F
    };
    private static final int[] FLOOR_INDICES = {0, 2, 1, 0, 3, 2};

    @Test
    void raycastsAgainstTransformedDoubleSidedTriangles() {
        PhysicsWorld world = worldWithFloor(new Vector3f(0.0F, 2.0F, 0.0F), new Quaternionf().rotateZ(0.0F));

        assertThat(world.raycast(new Vector3f(0.0F, 5.0F, 0.0F), new Vector3f(0.0F, -1.0F, 0.0F), 10.0F))
                .hasValueSatisfying(hit -> {
                    assertThat(hit.distance()).isCloseTo(3.0F, within());
                    assertThat(hit.point(new Vector3f())).isEqualTo(new Vector3f(0.0F, 2.0F, 0.0F));
                    assertThat(hit.normal(new Vector3f())).isEqualTo(new Vector3f(0.0F, 1.0F, 0.0F));
                });
        assertThat(world.raycast(new Vector3f(0.0F, -1.0F, 0.0F), new Vector3f(0.0F, 1.0F, 0.0F), 10.0F))
                .hasValueSatisfying(
                        hit -> assertThat(hit.normal(new Vector3f()).y).isCloseTo(-1.0F, within()));
    }

    @Test
    void overlapsSupportedConvexShapesAgainstTriangles() {
        PhysicsWorld world = worldWithFloor(new Vector3f(), IDENTITY);

        assertThat(world.overlap(new SphereShape(1.0F), new Vector3f(0.0F, 0.5F, 0.0F), IDENTITY))
                .singleElement()
                .satisfies(hit -> {
                    assertThat(hit.penetrationDepth()).isCloseTo(0.5F, within());
                    assertThat(hit.normal(new Vector3f())).isEqualTo(new Vector3f(0.0F, 1.0F, 0.0F));
                });
        assertThat(world.overlap(new CapsuleShape(0.5F, 1.0F), new Vector3f(2.0F, 0.25F, 0.0F), IDENTITY))
                .hasSize(1);
        assertThat(world.overlap(new BoxShape(1.0F, 1.0F, 1.0F), new Vector3f(-2.0F, 0.25F, 0.0F), IDENTITY))
                .singleElement()
                .satisfies(hit -> assertThat(hit.penetrationDepth()).isCloseTo(0.25F, within()));
        assertThat(world.overlap(new SphereShape(0.25F), new Vector3f(0.0F, 2.0F, 0.0F), IDENTITY))
                .isEmpty();
    }

    @Test
    void sweepsSupportedConvexShapesAgainstTriangles() {
        PhysicsWorld world = worldWithFloor(new Vector3f(), IDENTITY);
        Vector3f translation = new Vector3f(0.0F, -5.0F, 0.0F);

        assertThat(world.sweep(new SphereShape(1.0F), new Vector3f(0.0F, 3.0F, 0.0F), IDENTITY, translation))
                .hasValueSatisfying(hit -> {
                    assertThat(hit.fraction()).isCloseTo(0.4F, within());
                    assertThat(hit.distance()).isCloseTo(2.0F, within());
                    assertThat(hit.normal(new Vector3f())).isEqualTo(new Vector3f(0.0F, 1.0F, 0.0F));
                });
        assertThat(world.sweep(new CapsuleShape(0.5F, 1.0F), new Vector3f(2.0F, 3.0F, 0.0F), IDENTITY, translation))
                .isPresent();
        assertThat(world.sweep(new BoxShape(1.0F, 1.0F, 1.0F), new Vector3f(-2.0F, 3.0F, 0.0F), IDENTITY, translation))
                .isPresent();
    }

    @Test
    void limitsTriangleMeshesToStaticColliderTargets() {
        PhysicsWorld world = new PhysicsWorld();
        TriangleMeshShape mesh = floorShape();
        KinematicBody kinematicBody = world.addKinematicBody();
        CollisionSensor sensor = world.addCollisionSensor();
        Vector3f origin = new Vector3f();

        assertThatThrownBy(() -> kinematicBody.addCollider(mesh)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> sensor.addCollider(mesh)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> world.overlap(mesh, origin, IDENTITY)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> world.sweep(mesh, origin, IDENTITY, origin))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void exposesTriangleEdgesAsDebugGeometry() {
        PhysicsWorld world = worldWithFloor(new Vector3f(0.0F, 2.0F, 0.0F), IDENTITY);

        assertThat(world.debugSnapshot().lines()).hasSize(6).allSatisfy(line -> {
            assertThat(line.start(new Vector3f()).y).isEqualTo(2.0F);
            assertThat(line.end(new Vector3f()).y).isEqualTo(2.0F);
        });
    }

    private static PhysicsWorld worldWithFloor(Vector3f position, Quaternionf orientation) {
        PhysicsWorld world = new PhysicsWorld();
        world.addStaticBody(position, orientation).addCollider(floorShape());
        return world;
    }

    private static TriangleMeshShape floorShape() {
        return new TriangleMeshShape(FLOOR_POSITIONS, FLOOR_INDICES);
    }

    private static Offset<Float> within() {
        return Offset.offset(TOLERANCE);
    }
}
