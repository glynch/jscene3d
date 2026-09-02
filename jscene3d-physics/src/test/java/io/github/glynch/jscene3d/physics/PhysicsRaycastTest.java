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
import io.github.glynch.jscene3d.physics.shapes.SphereShape;
import org.assertj.core.data.Offset;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.junit.jupiter.api.Test;

final class PhysicsRaycastTest {
    private static final Offset<Float> TOLERANCE = Offset.offset(1.0E-4F);

    @Test
    void returnsNearestSphereHitAndNormalizesDirection() {
        PhysicsWorld world = new PhysicsWorld();
        Collider farther = addSphere(world, 7.0F, 1.0F);
        Collider nearer = addSphere(world, 4.0F, 1.0F);

        assertThat(world.raycast(new Vector3f(), new Vector3f(2.0F, 0.0F, 0.0F), 10.0F))
                .hasValueSatisfying(hit -> {
                    assertThat(hit.collider()).isSameAs(nearer);
                    assertThat(hit.collider()).isNotSameAs(farther);
                    assertThat(hit.distance()).isCloseTo(3.0F, TOLERANCE);
                    assertVector(hit.point(new Vector3f()), 3.0F, 0.0F, 0.0F);
                    assertVector(hit.normal(new Vector3f()), -1.0F, 0.0F, 0.0F);
                });
    }

    @Test
    void intersectsRotatedBox() {
        PhysicsWorld world = new PhysicsWorld();
        Quaternionf orientation = new Quaternionf().rotateZ((float) (Math.PI * 0.25));
        world.addStaticBody(new Vector3f(5.0F, 0.0F, 0.0F), orientation).addCollider(new BoxShape(2.0F, 2.0F, 2.0F));

        assertThat(world.raycast(new Vector3f(), new Vector3f(1.0F, 0.0F, 0.0F), 10.0F))
                .hasValueSatisfying(hit ->
                        assertThat(hit.distance()).isCloseTo(5.0F - (float) Math.sqrt(2.0), Offset.offset(2.0E-4F)));
    }

    @Test
    void intersectsCapsuleCylinderAndCap() {
        PhysicsWorld world = new PhysicsWorld();
        Collider capsule = world.addStaticBody().addCollider(new CapsuleShape(1.0F, 4.0F));

        assertThat(world.raycast(new Vector3f(-3.0F, 0.0F, 0.0F), new Vector3f(1.0F, 0.0F, 0.0F), 10.0F))
                .hasValueSatisfying(hit -> {
                    assertThat(hit.collider()).isSameAs(capsule);
                    assertThat(hit.distance()).isCloseTo(2.0F, TOLERANCE);
                });
        assertThat(world.raycast(new Vector3f(0.0F, 5.0F, 0.0F), new Vector3f(0.0F, -1.0F, 0.0F), 10.0F))
                .hasValueSatisfying(hit -> assertThat(hit.distance()).isCloseTo(2.0F, TOLERANCE));
    }

    @Test
    void returnsImmediateHitWhenRayStartsInside() {
        PhysicsWorld world = new PhysicsWorld();
        world.addStaticBody().addCollider(new BoxShape(2.0F, 2.0F, 2.0F));

        assertThat(world.raycast(new Vector3f(), new Vector3f(1.0F, 0.0F, 0.0F), 10.0F))
                .hasValueSatisfying(hit -> {
                    assertThat(hit.distance()).isZero();
                    assertVector(hit.normal(new Vector3f()), -1.0F, 0.0F, 0.0F);
                });
    }

    @Test
    void appliesEnabledLayerSensorAndExclusionFilters() {
        PhysicsWorld world = new PhysicsWorld();
        Collider disabled = addSphere(world, 2.0F);
        disabled.setEnabled(false);
        CollisionSensor sensor = world.addCollisionSensor(new Vector3f(4.0F, 0.0F, 0.0F), new Quaternionf());
        Collider sensorCollider = sensor.addCollider(new SphereShape(0.5F));
        Collider layerTwo = addSphere(world, 6.0F);
        layerTwo.setCollisionFilter(new CollisionFilter(0b0010, -1));
        Collider layerOne = addSphere(world, 8.0F);

        Vector3f origin = new Vector3f();
        Vector3f direction = new Vector3f(1.0F, 0.0F, 0.0F);
        assertThat(world.raycast(origin, direction, 20.0F))
                .hasValueSatisfying(hit -> assertThat(hit.collider()).isSameAs(layerTwo));
        assertThat(world.raycast(origin, direction, 20.0F, QueryFilter.layers(1)))
                .hasValueSatisfying(hit -> assertThat(hit.collider()).isSameAs(layerOne));
        assertThat(world.raycast(origin, direction, 20.0F, QueryFilter.DEFAULT.withSensorMode(SensorMode.ONLY)))
                .hasValueSatisfying(hit -> {
                    assertThat(hit.collider()).isSameAs(sensorCollider);
                    assertThat(hit.collisionObject()).isSameAs(sensor);
                });
        assertThat(world.raycast(
                        origin, direction, 20.0F, QueryFilter.layers(0b0010).excluding(layerTwo.collisionObject())))
                .isEmpty();
    }

    @Test
    void returnsEmptyWhenRayMissesOrIsTooShort() {
        PhysicsWorld world = new PhysicsWorld();
        addSphere(world, 5.0F);

        assertThat(world.raycast(new Vector3f(), new Vector3f(0.0F, 1.0F, 0.0F), 10.0F))
                .isEmpty();
        assertThat(world.raycast(new Vector3f(), new Vector3f(1.0F, 0.0F, 0.0F), 3.0F))
                .isEmpty();
    }

    @Test
    void rejectsRayParallelToAndOutsideABoxSlab() {
        PhysicsWorld world = new PhysicsWorld();
        world.addStaticBody().addCollider(new BoxShape(2.0F, 2.0F, 2.0F));

        assertThat(world.raycast(new Vector3f(2.0F, -3.0F, 0.0F), new Vector3f(0.0F, 1.0F, 0.0F), 10.0F))
                .isEmpty();
    }

    @Test
    void treatsZeroLengthCapsuleAsSphereForRaycasts() {
        PhysicsWorld world = new PhysicsWorld();
        world.addStaticBody().addCollider(new CapsuleShape(1.0F, 0.0F));

        assertThat(world.raycast(new Vector3f(-3.0F, 0.0F, 0.0F), new Vector3f(1.0F, 0.0F, 0.0F), 10.0F))
                .hasValueSatisfying(hit -> assertThat(hit.distance()).isCloseTo(2.0F, TOLERANCE));
    }

    private static Collider addSphere(PhysicsWorld world, float x) {
        return addSphere(world, x, 0.5F);
    }

    private static Collider addSphere(PhysicsWorld world, float x, float radius) {
        return world.addStaticBody(new Vector3f(x, 0.0F, 0.0F), new Quaternionf())
                .addCollider(new SphereShape(radius));
    }

    private static void assertVector(Vector3f value, float x, float y, float z) {
        assertThat(value.x).isCloseTo(x, TOLERANCE);
        assertThat(value.y).isCloseTo(y, TOLERANCE);
        assertThat(value.z).isCloseTo(z, TOLERANCE);
    }
}
