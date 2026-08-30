/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.render;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.glynch.jscene3d.core.BoundingSphere;
import org.joml.Matrix4f;
import org.junit.jupiter.api.Test;

final class FrustumTest {
    @Test
    void acceptsSpheresInsideOrIntersectingTheFrustum() {
        Frustum frustum = createCanonicalClipFrustum();
        Matrix4f worldMatrix = new Matrix4f();

        assertThat(frustum.intersects(new BoundingSphere(0.0f, 0.0f, 0.0f, 0.5f), worldMatrix))
                .isTrue();
        assertThat(frustum.intersects(new BoundingSphere(0.9f, 0.0f, 0.0f, 0.2f), worldMatrix))
                .isTrue();
    }

    @Test
    void rejectsSpheresOutsideEveryFrustumPlane() {
        Frustum frustum = createCanonicalClipFrustum();
        Matrix4f worldMatrix = new Matrix4f();

        assertThat(frustum.intersects(new BoundingSphere(-2.0f, 0.0f, 0.0f, 0.1f), worldMatrix))
                .isFalse();
        assertThat(frustum.intersects(new BoundingSphere(2.0f, 0.0f, 0.0f, 0.1f), worldMatrix))
                .isFalse();
        assertThat(frustum.intersects(new BoundingSphere(0.0f, -2.0f, 0.0f, 0.1f), worldMatrix))
                .isFalse();
        assertThat(frustum.intersects(new BoundingSphere(0.0f, 2.0f, 0.0f, 0.1f), worldMatrix))
                .isFalse();
        assertThat(frustum.intersects(new BoundingSphere(0.0f, 0.0f, -2.0f, 0.1f), worldMatrix))
                .isFalse();
        assertThat(frustum.intersects(new BoundingSphere(0.0f, 0.0f, 2.0f, 0.1f), worldMatrix))
                .isFalse();
    }

    @Test
    void transformsSphereCentersIntoWorldSpace() {
        Frustum frustum = createCanonicalClipFrustum();
        BoundingSphere sphere = new BoundingSphere(0.0f, 0.0f, 0.0f, 0.25f);

        assertThat(frustum.intersects(sphere, new Matrix4f().translation(0.8f, 0.0f, 0.0f)))
                .isTrue();
        assertThat(frustum.intersects(sphere, new Matrix4f().translation(2.0f, 0.0f, 0.0f)))
                .isFalse();
    }

    @Test
    void expandsSphereRadiiForUniformAndNonUniformWorldScale() {
        Frustum frustum = createCanonicalClipFrustum();
        BoundingSphere sphere = new BoundingSphere(0.0f, 0.0f, 0.0f, 0.4f);
        Matrix4f uniformScale = new Matrix4f().translation(1.5f, 0.0f, 0.0f).scale(2.0f);
        Matrix4f nonUniformScale = new Matrix4f().translation(2.0f, 0.0f, 0.0f).scale(0.5f, 3.0f, 1.0f);

        assertThat(frustum.intersects(sphere, uniformScale)).isTrue();
        assertThat(frustum.intersects(sphere, nonUniformScale)).isTrue();
    }

    private static Frustum createCanonicalClipFrustum() {
        Frustum frustum = new Frustum();
        frustum.update(new Matrix4f(), new Matrix4f());
        return frustum;
    }
}
