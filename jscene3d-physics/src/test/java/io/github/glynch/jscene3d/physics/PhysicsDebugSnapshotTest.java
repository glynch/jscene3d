/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.physics;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.glynch.jscene3d.physics.debug.PhysicsDebugLine;
import io.github.glynch.jscene3d.physics.debug.PhysicsDebugSnapshot;
import io.github.glynch.jscene3d.physics.shapes.BoxShape;
import io.github.glynch.jscene3d.physics.shapes.CapsuleShape;
import io.github.glynch.jscene3d.physics.shapes.SphereShape;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.junit.jupiter.api.Test;

final class PhysicsDebugSnapshotTest {
    @Test
    void snapshotsEveryShapeInColliderOrder() {
        PhysicsWorld world = new PhysicsWorld();
        Collider box = world.addCollider(new BoxShape(2.0F, 4.0F, 6.0F));
        Collider sphere = world.addCollider(new SphereShape(1.0F));
        Collider capsule = world.addCollider(new CapsuleShape(0.5F, 2.0F));

        PhysicsDebugSnapshot snapshot = world.debugSnapshot();

        assertThat(snapshot.lines()).isNotEmpty();
        assertThat(snapshot.lines()).extracting(PhysicsDebugLine::collider).contains(box, sphere, capsule);
        assertThat(snapshot.lines()).extracting(line -> line.collider().id()).isSorted();
    }

    @Test
    void capturesWorldTransformAndDefensivelyCopiesEndpoints() {
        PhysicsWorld world = new PhysicsWorld();
        Collider box = world.addCollider(
                new BoxShape(2.0F, 2.0F, 2.0F),
                new Vector3f(5.0F, 6.0F, 7.0F),
                new Quaternionf().rotateY((float) Math.PI * 0.5F));

        PhysicsDebugLine line = world.debugSnapshot().lines().getFirst();
        Vector3f firstRead = line.start(new Vector3f());
        firstRead.set(Float.NaN);

        assertThat(line.collider()).isSameAs(box);
        assertThat(line.start(new Vector3f()).isFinite()).isTrue();
        assertThat(line.start(new Vector3f()).distance(new Vector3f(5.0F, 6.0F, 7.0F)))
                .isGreaterThan(0.0F);
    }
}
