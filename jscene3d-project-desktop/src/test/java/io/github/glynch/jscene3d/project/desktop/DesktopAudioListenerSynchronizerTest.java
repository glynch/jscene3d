/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.desktop;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.glynch.jscene3d.project.spatial3d.Transform3d;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.joml.Quaternionfc;
import org.joml.Vector3f;
import org.joml.Vector3fc;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;

final class DesktopAudioListenerSynchronizerTest {
    /** Copies the active camera's world position and orientation into the audio listener. */
    @Test
    void synchronizesListenerFromCameraWorldTransform() {
        Matrix4f world = new Matrix4f().translation(-6.0F, 1.28125F, 6.0F).rotateY((float) Math.toRadians(90.0));
        ListenerPose recorded = new ListenerPose();

        DesktopAudioListenerSynchronizer.synchronize(new FixedTransform(world), recorded::set);

        assertThat(recorded.position).isEqualTo(new Vector3f(-6.0F, 1.28125F, 6.0F));
        assertThat(recorded.forward).isEqualTo(new Vector3f(-1.0F, 0.0F, 0.0F));
        assertThat(recorded.up).isEqualTo(new Vector3f(0.0F, 1.0F, 0.0F));
    }

    private static final class ListenerPose {
        private @Nullable Vector3f position;
        private @Nullable Vector3f forward;
        private @Nullable Vector3f up;

        private void set(Vector3fc position, Vector3fc forward, Vector3fc up) {
            this.position = new Vector3f(position);
            this.forward = new Vector3f(forward);
            this.up = new Vector3f(up);
        }
    }

    private record FixedTransform(Matrix4fc worldMatrix) implements Transform3d {
        @Override
        public Vector3fc position() {
            throw new AssertionError("local position is not used");
        }

        @Override
        public Quaternionfc orientation() {
            throw new AssertionError("local orientation is not used");
        }

        @Override
        public Vector3fc scale() {
            throw new AssertionError("local scale is not used");
        }

        @Override
        public void setPosition(float x, float y, float z) {
            throw new AssertionError("mutation is not used");
        }

        @Override
        public void setOrientation(float x, float y, float z, float w) {
            throw new AssertionError("mutation is not used");
        }

        @Override
        public void setWorldPose(Vector3fc position, Quaternionfc orientation) {
            throw new AssertionError("mutation is not used");
        }

        @Override
        public void setScale(float x, float y, float z) {
            throw new AssertionError("mutation is not used");
        }

        @Override
        public Matrix4fc localMatrix() {
            throw new AssertionError("local matrix is not used");
        }

        @Override
        public boolean isClosed() {
            return false;
        }

        @Override
        public void close() {
            throw new AssertionError("closing is not used");
        }
    }
}
