/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.doom.runtime.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.glynch.jscene3d.doom.runtime.DoomFloor;
import io.github.glynch.jscene3d.doom.runtime.DoomFloorDescriptors;
import io.github.glynch.jscene3d.project.component.PropertyId;
import io.github.glynch.jscene3d.project.runtime.Entity;
import io.github.glynch.jscene3d.project.runtime.FixedUpdateContext;
import io.github.glynch.jscene3d.project.runtime.extension.ComponentReferenceResolver;
import io.github.glynch.jscene3d.project.spatial3d.Transform3d;
import java.time.Duration;
import java.util.List;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.joml.Quaternionf;
import org.joml.Quaternionfc;
import org.joml.Vector3f;
import org.joml.Vector3fc;
import org.junit.jupiter.api.Test;

/** Specifies descriptor-driven one-shot floor motion independently of a concrete physics adapter. */
final class DoomFloorComponentTest {
    /** Lowers at the classic authored rate, clamps exactly, and cannot be activated twice. */
    @Test
    void lowersOnceAndStaysLowered() {
        RecordingTransform transform = new RecordingTransform(2.0F, 99.0F, 3.0F);
        DoomFloorComponent floor =
                new DoomFloorComponent(DoomFloor.Profile.WALK_ONCE_LOWER_TO_HIGHEST_SURROUNDING, 2.0F, 1.0F, 2.0F);
        floor.bindReferences(references(transform));

        assertThat(transform.position()).isEqualTo(new Vector3f(2.0F, 2.0F, 3.0F));
        assertThat(floor.activate()).isTrue();
        floor.onBeforePhysics(update(Duration.ofMillis(250)));

        assertThat(floor.phase()).isEqualTo(DoomFloor.Phase.LOWERING);
        assertThat(floor.currentHeight()).isEqualTo(1.5F);
        floor.onBeforePhysics(update(Duration.ofSeconds(1)));

        assertThat(floor.phase()).isEqualTo(DoomFloor.Phase.LOWERED);
        assertThat(floor.currentHeight()).isEqualTo(1.0F);
        assertThat(floor.activate()).isFalse();
        assertThat(transform.position()).isEqualTo(new Vector3f(2.0F, 1.0F, 3.0F));
    }

    /** Rejects non-finite, non-lowering, and stationary authored configurations. */
    @Test
    void rejectsInvalidConfiguration() {
        DoomFloor.Profile profile = DoomFloor.Profile.WALK_ONCE_LOWER_TO_HIGHEST_SURROUNDING;
        assertThatThrownBy(() -> new DoomFloorComponent(profile, Float.NaN, 0.0F, 1.0F))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("raisedHeight");
        assertThatThrownBy(() -> new DoomFloorComponent(profile, 1.0F, 1.0F, 1.0F))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("loweredHeight");
        assertThatThrownBy(() -> new DoomFloorComponent(profile, 1.0F, 0.0F, 0.0F))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("speed");
    }

    private static FixedUpdateContext update(Duration step) {
        return new FixedUpdateContext(0L, step, Duration.ZERO);
    }

    private static ComponentReferenceResolver references(Transform3d transform) {
        return new ComponentReferenceResolver() {
            @Override
            public Entity entity(PropertyId property) {
                throw new UnsupportedOperationException();
            }

            @Override
            public <T> T component(PropertyId property, Class<T> valueType) {
                assertThat(property).isEqualTo(DoomFloorDescriptors.TRANSFORM_PROPERTY);
                return valueType.cast(transform);
            }

            @Override
            public <T> List<T> components(PropertyId property, Class<T> valueType) {
                throw new UnsupportedOperationException();
            }
        };
    }

    /** Minimal mutable transform fixture recording the behavior's output. */
    private static final class RecordingTransform implements Transform3d {
        private final Vector3f position;
        private final Quaternionf orientation = new Quaternionf();
        private final Vector3f scale = new Vector3f(1.0F);

        private RecordingTransform(float x, float y, float z) {
            position = new Vector3f(x, y, z);
        }

        @Override
        public Vector3fc position() {
            return position;
        }

        @Override
        public Quaternionfc orientation() {
            return orientation;
        }

        @Override
        public Vector3fc scale() {
            return scale;
        }

        @Override
        public void setPosition(float x, float y, float z) {
            position.set(x, y, z);
        }

        @Override
        public void setOrientation(float x, float y, float z, float w) {
            orientation.set(x, y, z, w);
        }

        @Override
        public void setWorldPose(Vector3fc worldPosition, Quaternionfc worldOrientation) {
            position.set(worldPosition);
            orientation.set(worldOrientation);
        }

        @Override
        public void setScale(float x, float y, float z) {
            scale.set(x, y, z);
        }

        @Override
        public Matrix4fc localMatrix() {
            return new Matrix4f().translationRotateScale(position, orientation, scale);
        }

        @Override
        public Matrix4fc worldMatrix() {
            return localMatrix();
        }

        @Override
        public boolean isClosed() {
            return false;
        }

        @Override
        public void close() {
            // The fixture owns no resources.
        }
    }
}
