/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.game.physics;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.glynch.jscene3d.objects.Object3D;
import io.github.glynch.jscene3d.physics.KinematicBody;
import io.github.glynch.jscene3d.physics.PhysicsWorld;
import org.assertj.core.data.Offset;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.junit.jupiter.api.Test;

final class PhysicsBindingTest {
    private static final Offset<Float> TOLERANCE = Offset.offset(1.0E-5F);

    @Test
    void interpolatesBetweenCapturedPhysicsTransforms() {
        PhysicsWorld world = new PhysicsWorld();
        KinematicBody body = world.addKinematicBody(new Vector3f(), new Quaternionf());
        Object3D sceneObject = new Object3D();
        PhysicsBinding binding = new PhysicsBinding(body, sceneObject);
        body.setTransform(new Vector3f(10.0F, 4.0F, -2.0F), new Quaternionf().rotateY((float) Math.PI));

        binding.capture();
        binding.apply(0.5F);

        assertThat(sceneObject.position().x()).isCloseTo(5.0F, TOLERANCE);
        assertThat(sceneObject.position().y()).isCloseTo(2.0F, TOLERANCE);
        assertThat(Math.abs(sceneObject.quaternion().y())).isCloseTo((float) Math.sqrt(0.5), TOLERANCE);
        assertThat(binding.collisionObject()).isSameAs(body);
        assertThat(binding.sceneObject()).isSameAs(sceneObject);
    }

    @Test
    void convertsWorldTransformsThroughAParent() {
        PhysicsWorld world = new PhysicsWorld();
        KinematicBody body = world.addKinematicBody(new Vector3f(7.0F, 0.0F, 0.0F), new Quaternionf());
        Object3D parent = new Object3D();
        parent.setPosition(5.0F, 0.0F, 0.0F);
        Object3D sceneObject = new Object3D();
        parent.add(sceneObject);

        PhysicsBinding binding = new PhysicsBinding(body, sceneObject);

        assertThat(sceneObject.position().x()).isCloseTo(2.0F, TOLERANCE);
        assertThat(sceneObject.worldPosition(new Vector3f()).x).isCloseTo(7.0F, TOLERANCE);
    }

    @Test
    void snapsHistoryAndRejectsInvalidUse() {
        PhysicsWorld world = new PhysicsWorld();
        KinematicBody body = world.addKinematicBody();
        Object3D sceneObject = new Object3D();
        PhysicsBinding binding = new PhysicsBinding(body, sceneObject);
        body.setTransform(new Vector3f(3.0F, 2.0F, 1.0F), new Quaternionf());

        binding.snap();

        assertThat(sceneObject.position().x()).isEqualTo(3.0F);
        assertThatThrownBy(() -> binding.apply(Float.NaN)).isInstanceOf(IllegalArgumentException.class);
        world.remove(body);
        assertThatThrownBy(binding::capture).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> new PhysicsBinding(body, new Object3D())).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsNonInvertibleParentTransforms() {
        PhysicsWorld world = new PhysicsWorld();
        KinematicBody body = world.addKinematicBody();
        Object3D parent = new Object3D();
        Object3D sceneObject = new Object3D();
        parent.setScale(0.0F, 1.0F, 1.0F);
        parent.add(sceneObject);

        assertThatThrownBy(() -> new PhysicsBinding(body, sceneObject)).isInstanceOf(IllegalStateException.class);
    }
}
