/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.physics;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.glynch.jscene3d.physics.queries.QueryFilter;
import io.github.glynch.jscene3d.physics.queries.SensorMode;
import io.github.glynch.jscene3d.physics.shapes.BoxShape;
import io.github.glynch.jscene3d.physics.shapes.CapsuleShape;
import io.github.glynch.jscene3d.physics.shapes.CollisionShape;
import io.github.glynch.jscene3d.physics.shapes.SphereShape;
import java.util.List;
import org.assertj.core.data.Offset;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.junit.jupiter.api.Test;

final class PhysicsOverlapTest {
    private static final Quaternionf IDENTITY = new Quaternionf();
    private static final Offset<Float> TOLERANCE = Offset.offset(2.0E-4F);

    @Test
    void reportsSphereSphereContactDepthAndNormal() {
        PhysicsWorld world = new PhysicsWorld();
        StaticBody body = world.addStaticBody(new Vector3f(1.5F, 0.0F, 0.0F), IDENTITY);
        Collider collider = body.addCollider(new SphereShape(1.0F));

        assertThat(world.overlap(new SphereShape(1.0F), new Vector3f(), IDENTITY))
                .singleElement()
                .satisfies(hit -> {
                    assertThat(hit.collider()).isSameAs(collider);
                    assertThat(hit.collisionObject()).isSameAs(body);
                    assertThat(hit.penetrationDepth()).isCloseTo(0.5F, TOLERANCE);
                    assertThat(hit.normal(new Vector3f()).x).isCloseTo(-1.0F, TOLERANCE);
                });
    }

    @Test
    void touchingShapesCountAsOverlapping() {
        PhysicsWorld world = new PhysicsWorld();
        world.addStaticBody(new Vector3f(2.0F, 0.0F, 0.0F), IDENTITY).addCollider(new BoxShape(2.0F, 2.0F, 2.0F));

        assertThat(world.overlap(new SphereShape(1.0F), new Vector3f(), IDENTITY))
                .singleElement()
                .satisfies(hit -> assertThat(hit.penetrationDepth()).isCloseTo(0.0F, TOLERANCE));
    }

    @Test
    void supportsEveryShapePairAndRotatedBoxes() {
        assertOverlap(new SphereShape(1.0F), new CapsuleShape(0.75F, 2.0F), new Vector3f(1.2F, 0.0F, 0.0F));
        assertOverlap(new CapsuleShape(0.75F, 2.0F), new SphereShape(1.0F), new Vector3f(1.2F, 0.0F, 0.0F));
        assertOverlap(new CapsuleShape(0.75F, 2.0F), new CapsuleShape(0.75F, 2.0F), new Vector3f(1.0F, 0.0F, 0.0F));
        assertOverlap(new CapsuleShape(0.75F, 3.0F), new BoxShape(2.0F, 2.0F, 2.0F), new Vector3f(1.4F, 0.0F, 0.0F));
        assertOverlap(new BoxShape(2.0F, 2.0F, 2.0F), new CapsuleShape(0.75F, 3.0F), new Vector3f(1.4F, 0.0F, 0.0F));

        PhysicsWorld world = new PhysicsWorld();
        Quaternionf rotation = new Quaternionf().rotateY((float) (Math.PI * 0.25));
        world.addStaticBody(new Vector3f(1.5F, 0.0F, 0.0F), rotation).addCollider(new BoxShape(2.0F, 2.0F, 2.0F));
        assertThat(world.overlap(new BoxShape(2.0F, 2.0F, 2.0F), new Vector3f(), IDENTITY))
                .hasSize(1);
    }

    @Test
    void doesNotReportSeparatedPairs() {
        PhysicsWorld world = new PhysicsWorld();
        world.addStaticBody(new Vector3f(10.0F, 0.0F, 0.0F), IDENTITY).addCollider(new CapsuleShape(0.5F, 2.0F));

        assertThat(world.overlap(new BoxShape(1.0F, 1.0F, 1.0F), new Vector3f(), IDENTITY))
                .isEmpty();
    }

    @Test
    void returnsHitsInStableColliderOrderAndHonorsSensorPolicy() {
        PhysicsWorld world = new PhysicsWorld();
        Collider first = world.addStaticBody().addCollider(new SphereShape(2.0F));
        CollisionSensor sensor = world.addCollisionSensor();
        Collider sensorCollider = sensor.addCollider(new SphereShape(2.0F));
        Collider third = world.addStaticBody().addCollider(new SphereShape(2.0F));

        List<Collider> defaults = world.overlap(new SphereShape(1.0F), new Vector3f(), IDENTITY).stream()
                .map(hit -> hit.collider())
                .toList();
        assertThat(defaults).containsExactly(first, third);

        List<Collider> sensors = world
                .overlap(
                        new SphereShape(1.0F),
                        new Vector3f(),
                        IDENTITY,
                        QueryFilter.DEFAULT.withSensorMode(SensorMode.ONLY))
                .stream()
                .map(hit -> hit.collider())
                .toList();
        assertThat(sensors).containsExactly(sensorCollider);
    }

    private static void assertOverlap(CollisionShape query, CollisionShape target, Vector3f targetPosition) {
        PhysicsWorld world = new PhysicsWorld();
        world.addStaticBody(targetPosition, IDENTITY).addCollider(target);
        assertThat(world.overlap(query, new Vector3f(), IDENTITY)).hasSize(1);
    }
}
