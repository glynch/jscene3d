/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.physics;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.glynch.jscene3d.physics.queries.QueryFilter;
import io.github.glynch.jscene3d.physics.queries.TriggerMode;
import io.github.glynch.jscene3d.physics.shapes.SphereShape;
import org.assertj.core.data.Offset;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.junit.jupiter.api.Test;

final class PhysicsWorldTest {
    private static final float TOLERANCE = 1.0E-5F;

    @Test
    void copiesAndNormalizesColliderTransforms() {
        PhysicsWorld world = new PhysicsWorld();
        Vector3f sourcePosition = new Vector3f(1.0F, 2.0F, 3.0F);
        Quaternionf sourceOrientation = new Quaternionf(0.0F, 2.0F, 0.0F, 2.0F);

        Collider collider = world.addCollider(new SphereShape(1.0F), sourcePosition, sourceOrientation);
        sourcePosition.zero();
        sourceOrientation.identity();

        assertVector(collider.position(new Vector3f()), 1.0F, 2.0F, 3.0F);
        assertThat(collider.orientation(new Quaternionf()).lengthSquared()).isCloseTo(1.0F, within());
        Vector3f returned = collider.position(new Vector3f());
        returned.zero();
        assertVector(collider.position(new Vector3f()), 1.0F, 2.0F, 3.0F);
    }

    @Test
    void movesColliderAndUpdatesSpatialIndex() {
        PhysicsWorld world = new PhysicsWorld();
        Collider collider = world.addCollider(new SphereShape(1.0F));

        collider.setTransform(new Vector3f(5.0F, 0.0F, 0.0F), new Quaternionf());

        assertThat(world.raycast(new Vector3f(), new Vector3f(1.0F, 0.0F, 0.0F), 10.0F))
                .hasValueSatisfying(hit -> assertThat(hit.distance()).isCloseTo(4.0F, within()));
    }

    @Test
    void removalIsTerminalAndClearInvalidatesRemainingHandles() {
        PhysicsWorld world = new PhysicsWorld();
        world.clear();
        assertThat(world.raycast(new Vector3f(), new Vector3f(1.0F, 0.0F, 0.0F), 10.0F))
                .isEmpty();
        Collider first = world.addCollider(new SphereShape(1.0F));
        Collider second = world.addCollider(new SphereShape(1.0F));

        world.remove(first);
        assertThat(first.isRegistered()).isFalse();
        assertThat(first.shape()).isEqualTo(new SphereShape(1.0F));
        assertThatThrownBy(() -> first.setEnabled(false)).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> world.remove(first)).isInstanceOf(IllegalArgumentException.class);

        world.clear();
        assertThat(world.colliderCount()).isZero();
        assertThat(second.isRegistered()).isFalse();
    }

    @Test
    void rejectsForeignHandlesAndInvalidQueryInputs() {
        PhysicsWorld firstWorld = new PhysicsWorld();
        PhysicsWorld secondWorld = new PhysicsWorld();
        Collider foreign = secondWorld.addCollider(new SphereShape(1.0F));

        assertThatThrownBy(() -> firstWorld.remove(foreign)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> firstWorld.addCollider(
                        new SphereShape(1.0F), new Vector3f(Float.NaN, 0.0F, 0.0F), new Quaternionf()))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> firstWorld.addCollider(
                        new SphereShape(1.0F), new Vector3f(), new Quaternionf(0.0F, 0.0F, 0.0F, 0.0F)))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> firstWorld.raycast(new Vector3f(), new Vector3f(), 1.0F))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() ->
                        firstWorld.raycast(new Vector3f(Float.NaN, 0.0F, 0.0F), new Vector3f(1.0F, 0.0F, 0.0F), 1.0F))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() ->
                        firstWorld.raycast(new Vector3f(), new Vector3f(Float.POSITIVE_INFINITY, 0.0F, 0.0F), 1.0F))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() ->
                        firstWorld.raycast(new Vector3f(), new Vector3f(1.0F, 0.0F, 0.0F), Float.POSITIVE_INFINITY))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> firstWorld.sweep(
                        new SphereShape(1.0F), new Vector3f(), new Quaternionf(), new Vector3f(Float.NaN, 0.0F, 0.0F)))
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
        Collider excluded = world.addCollider(new SphereShape(1.0F));

        QueryFilter filter =
                QueryFilter.layers(0b0101).withTriggerMode(TriggerMode.INCLUDE).excluding(excluded);

        assertThat(filter.layerMask()).isEqualTo(0b0101);
        assertThat(filter.triggerMode()).isEqualTo(TriggerMode.INCLUDE);
        assertThat(filter.excludedCollider()).contains(excluded);
        assertThat(filter.withLayerMask(0b0010).layerMask()).isEqualTo(0b0010);
        assertThat(filter.withoutExclusion().excludedCollider()).isEmpty();
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
