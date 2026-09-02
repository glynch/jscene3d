/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.physics;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.glynch.jscene3d.physics.queries.QueryFilter;
import io.github.glynch.jscene3d.physics.queries.TriggerMode;
import io.github.glynch.jscene3d.physics.shapes.BoxShape;
import io.github.glynch.jscene3d.physics.shapes.CapsuleShape;
import io.github.glynch.jscene3d.physics.shapes.CollisionShape;
import io.github.glynch.jscene3d.physics.shapes.SphereShape;
import org.assertj.core.data.Offset;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.junit.jupiter.api.Test;

final class PhysicsSweepTest {
    private static final Quaternionf IDENTITY = new Quaternionf();
    private static final Offset<Float> TOLERANCE = Offset.offset(3.0E-3F);

    @Test
    void sweepsSphereToSphereAndReturnsTravelFraction() {
        PhysicsWorld world = new PhysicsWorld();
        Collider target = world.addCollider(new SphereShape(1.0F), new Vector3f(5.0F, 0.0F, 0.0F), IDENTITY);

        assertThat(world.sweep(new SphereShape(1.0F), new Vector3f(), IDENTITY, new Vector3f(10.0F, 0.0F, 0.0F)))
                .hasValueSatisfying(hit -> {
                    assertThat(hit.collider()).isSameAs(target);
                    assertThat(hit.fraction()).isCloseTo(0.3F, TOLERANCE);
                    assertThat(hit.distance()).isCloseTo(3.0F, TOLERANCE);
                    assertThat(hit.normal(new Vector3f()).x).isCloseTo(-1.0F, TOLERANCE);
                });
    }

    @Test
    void supportsBoxAndCapsuleSweeps() {
        assertSweepHits(new BoxShape(1.0F, 1.0F, 1.0F), new BoxShape(2.0F, 2.0F, 2.0F));
        assertSweepHits(new CapsuleShape(0.5F, 2.0F), new BoxShape(2.0F, 2.0F, 2.0F));
        assertSweepHits(new BoxShape(1.0F, 1.0F, 1.0F), new CapsuleShape(0.5F, 2.0F));
        assertSweepHits(new CapsuleShape(0.5F, 2.0F), new CapsuleShape(0.5F, 2.0F));
    }

    @Test
    void startingOverlapReturnsZeroFractionEvenWithoutTranslation() {
        PhysicsWorld world = new PhysicsWorld();
        world.addCollider(new SphereShape(2.0F));

        assertThat(world.sweep(new SphereShape(1.0F), new Vector3f(), IDENTITY, new Vector3f()))
                .hasValueSatisfying(hit -> {
                    assertThat(hit.fraction()).isZero();
                    assertThat(hit.distance()).isZero();
                });
    }

    @Test
    void ignoresObjectsBehindOrBeyondTheTranslation() {
        PhysicsWorld world = new PhysicsWorld();
        world.addCollider(new SphereShape(1.0F), new Vector3f(-3.0F, 0.0F, 0.0F), IDENTITY);
        world.addCollider(new SphereShape(1.0F), new Vector3f(20.0F, 0.0F, 0.0F), IDENTITY);

        assertThat(world.sweep(new SphereShape(1.0F), new Vector3f(), IDENTITY, new Vector3f(10.0F, 0.0F, 0.0F)))
                .isEmpty();
    }

    @Test
    void returnsNearestAcceptedSweepHit() {
        PhysicsWorld world = new PhysicsWorld();
        Collider trigger = world.addCollider(new SphereShape(1.0F), new Vector3f(3.0F, 0.0F, 0.0F), IDENTITY);
        trigger.setTrigger(true);
        Collider solid = world.addCollider(new SphereShape(1.0F), new Vector3f(6.0F, 0.0F, 0.0F), IDENTITY);

        Vector3f translation = new Vector3f(10.0F, 0.0F, 0.0F);
        assertThat(world.sweep(new SphereShape(0.5F), new Vector3f(), IDENTITY, translation))
                .hasValueSatisfying(hit -> assertThat(hit.collider()).isSameAs(solid));
        assertThat(world.sweep(
                        new SphereShape(0.5F),
                        new Vector3f(),
                        IDENTITY,
                        translation,
                        QueryFilter.DEFAULT.withTriggerMode(TriggerMode.INCLUDE)))
                .hasValueSatisfying(hit -> assertThat(hit.collider()).isSameAs(trigger));
    }

    private static void assertSweepHits(CollisionShape query, CollisionShape target) {
        PhysicsWorld world = new PhysicsWorld();
        world.addCollider(target, new Vector3f(5.0F, 0.0F, 0.0F), IDENTITY);
        assertThat(world.sweep(query, new Vector3f(), IDENTITY, new Vector3f(10.0F, 0.0F, 0.0F)))
                .isPresent();
    }
}
