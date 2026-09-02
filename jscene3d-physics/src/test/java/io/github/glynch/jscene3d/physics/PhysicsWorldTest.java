/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.physics;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.glynch.jscene3d.physics.queries.QueryFilter;
import io.github.glynch.jscene3d.physics.queries.SensorMode;
import io.github.glynch.jscene3d.physics.shapes.SphereShape;
import org.assertj.core.data.Offset;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.junit.jupiter.api.Test;

final class PhysicsWorldTest {
    private static final float TOLERANCE = 1.0E-5F;

    @Test
    void copiesAndNormalizesObjectAndColliderTransforms() {
        PhysicsWorld world = new PhysicsWorld();
        Vector3f sourcePosition = new Vector3f(1.0F, 2.0F, 3.0F);
        Quaternionf sourceOrientation = new Quaternionf(0.0F, 2.0F, 0.0F, 2.0F);
        Vector3f localPosition = new Vector3f(4.0F, 5.0F, 6.0F);

        StaticBody body = world.addStaticBody(sourcePosition, sourceOrientation);
        Collider collider = body.addCollider(new SphereShape(1.0F), localPosition, new Quaternionf());
        sourcePosition.zero();
        sourceOrientation.identity();
        localPosition.zero();

        assertVector(body.position(new Vector3f()), 1.0F, 2.0F, 3.0F);
        assertThat(body.orientation(new Quaternionf()).lengthSquared()).isCloseTo(1.0F, within());
        assertVector(collider.localPosition(new Vector3f()), 4.0F, 5.0F, 6.0F);
        Vector3f returned = collider.position(new Vector3f());
        returned.zero();
        assertThat(collider.position(new Vector3f())).isNotEqualTo(returned);
        assertThat(collider.collisionObject()).isSameAs(body);
    }

    @Test
    void movesEveryColliderWithItsKinematicBodyAndUpdatesTheSpatialIndex() {
        PhysicsWorld world = new PhysicsWorld();
        KinematicBody body = world.addKinematicBody();
        Collider left = body.addCollider(new SphereShape(0.5F), new Vector3f(-1.0F, 0.0F, 0.0F), new Quaternionf());
        Collider right = body.addCollider(new SphereShape(0.5F), new Vector3f(1.0F, 0.0F, 0.0F), new Quaternionf());

        body.setTransform(new Vector3f(5.0F, 0.0F, 0.0F), new Quaternionf());

        assertVector(left.position(new Vector3f()), 4.0F, 0.0F, 0.0F);
        assertVector(right.position(new Vector3f()), 6.0F, 0.0F, 0.0F);
        assertThat(world.raycast(new Vector3f(), new Vector3f(1.0F, 0.0F, 0.0F), 10.0F))
                .hasValueSatisfying(hit -> {
                    assertThat(hit.collider()).isSameAs(left);
                    assertThat(hit.collisionObject()).isSameAs(body);
                    assertThat(hit.distance()).isCloseTo(3.5F, within());
                });
    }

    @Test
    void composesBodyAndColliderTransforms() {
        PhysicsWorld world = new PhysicsWorld();
        Quaternionf bodyOrientation = new Quaternionf().rotateZ((float) Math.toRadians(90.0));
        Quaternionf localOrientation = new Quaternionf().rotateX((float) Math.toRadians(90.0));
        StaticBody body = world.addStaticBody(new Vector3f(5.0F, 0.0F, 0.0F), bodyOrientation);
        Collider collider = body.addCollider(new SphereShape(0.5F), new Vector3f(2.0F, 0.0F, 0.0F), localOrientation);

        assertVector(collider.position(new Vector3f()), 5.0F, 2.0F, 0.0F);
        Quaternionf expected = new Quaternionf(bodyOrientation).mul(localOrientation);
        assertThat(collider.orientation(new Quaternionf()).dot(expected)).isCloseTo(1.0F, within());
    }

    @Test
    void removesIndividualCollidersAndWholeObjectsTerminally() {
        PhysicsWorld world = new PhysicsWorld();
        StaticBody first = world.addStaticBody();
        Collider retained = first.addCollider(new SphereShape(1.0F));
        Collider removed = first.addCollider(new SphereShape(2.0F));
        StaticBody second = world.addStaticBody();
        Collider secondCollider = second.addCollider(new SphereShape(1.0F));

        first.removeCollider(removed);
        assertThat(first.colliders()).containsExactly(retained);
        assertThat(removed.isRegistered()).isFalse();
        assertThatThrownBy(() -> removed.setEnabled(false)).isInstanceOf(IllegalStateException.class);

        world.remove(first);
        assertThat(first.isRegistered()).isFalse();
        assertThat(retained.isRegistered()).isFalse();
        SphereShape newShape = new SphereShape(1.0F);
        assertThatThrownBy(() -> first.addCollider(newShape)).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> world.remove(first)).isInstanceOf(IllegalArgumentException.class);

        world.clear();
        assertThat(world.collisionObjectCount()).isZero();
        assertThat(world.colliderCount()).isZero();
        assertThat(second.isRegistered()).isFalse();
        assertThat(secondCollider.isRegistered()).isFalse();
    }

    @Test
    void rejectsForeignHandlesAndInvalidInputs() {
        PhysicsWorld firstWorld = new PhysicsWorld();
        PhysicsWorld secondWorld = new PhysicsWorld();
        StaticBody foreign = secondWorld.addStaticBody();
        Vector3f invalidPosition = new Vector3f(Float.NaN, 0.0F, 0.0F);
        Quaternionf identity = new Quaternionf();
        Quaternionf invalidOrientation = new Quaternionf(0.0F, 0.0F, 0.0F, 0.0F);
        StaticBody body = firstWorld.addStaticBody();
        SphereShape shape = new SphereShape(1.0F);
        Vector3f origin = new Vector3f();
        Vector3f direction = new Vector3f(1.0F, 0.0F, 0.0F);
        Vector3f invalidDirection = new Vector3f(Float.POSITIVE_INFINITY, 0.0F, 0.0F);
        Vector3f invalidTranslation = new Vector3f(Float.NaN, 0.0F, 0.0F);

        assertThatThrownBy(() -> firstWorld.remove(foreign)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> firstWorld.addStaticBody(invalidPosition, identity))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> firstWorld.addKinematicBody(origin, invalidOrientation))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> body.addCollider(shape, invalidPosition, identity))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> firstWorld.raycast(origin, origin, 1.0F)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> firstWorld.raycast(invalidPosition, direction, 1.0F))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> firstWorld.raycast(origin, invalidDirection, 1.0F))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> firstWorld.raycast(origin, direction, Float.POSITIVE_INFINITY))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> firstWorld.sweep(shape, origin, identity, invalidTranslation))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void collisionFiltersMatchMutually() {
        CollisionFilter first = new CollisionFilter(0b0010, 0b0100);
        CollisionFilter second = new CollisionFilter(0b0100, 0b0010);
        CollisionFilter oneWay = new CollisionFilter(0b0100, 0b1000);

        assertThat(first.matches(second)).isTrue();
        assertThat(first.matches(oneWay)).isFalse();
        assertThat(first.matches(new CollisionFilter(0b1000, -1))).isFalse();
    }

    @Test
    void queryFiltersAreImmutableAndComposable() {
        PhysicsWorld world = new PhysicsWorld();
        StaticBody excluded = world.addStaticBody();
        excluded.addCollider(new SphereShape(1.0F));

        QueryFilter filter =
                QueryFilter.layers(0b0101).withSensorMode(SensorMode.INCLUDE).excluding(excluded);

        assertThat(filter.layerMask()).isEqualTo(0b0101);
        assertThat(filter.sensorMode()).isEqualTo(SensorMode.INCLUDE);
        assertThat(filter.excludedObject()).contains(excluded);
        assertThat(filter.withLayerMask(0b0010).layerMask()).isEqualTo(0b0010);
        assertThat(filter.withoutExclusion().excludedObject()).isEmpty();
        assertThat(QueryFilter.DEFAULT.withoutExclusion()).isSameAs(QueryFilter.DEFAULT);
    }

    private static Offset<Float> within() {
        return Offset.offset(TOLERANCE);
    }

    private static void assertVector(Vector3f value, float x, float y, float z) {
        assertThat(value.x).isCloseTo(x, within());
        assertThat(value.y).isCloseTo(y, within());
        assertThat(value.z).isCloseTo(z, within());
    }
}
