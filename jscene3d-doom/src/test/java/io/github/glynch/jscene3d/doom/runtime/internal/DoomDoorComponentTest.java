/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.doom.runtime.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.glynch.jscene3d.doom.runtime.DoomDoor;
import io.github.glynch.jscene3d.doom.runtime.DoomDoorDescriptors;
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

/** Specifies descriptor-driven classic Doom door motion independently of a concrete physics adapter. */
final class DoomDoorComponentTest {
    /** Opens at an authored speed and remains open when no close delay is authored. */
    @Test
    void opensAndStaysOpen() {
        RecordingTransform transform = new RecordingTransform(2.0F, 99.0F, 3.0F);
        DoomDoorComponent door = new DoomDoorComponent(1.0F, 3.0F, 4.0F, 0.0F);
        door.bindReferences(references(transform));

        assertThat(transform.position()).isEqualTo(new Vector3f(2.0F, 1.0F, 3.0F));
        assertThat(door.activate()).isTrue();
        door.onBeforePhysics(update(Duration.ofMillis(250)));

        assertThat(door.phase()).isEqualTo(DoomDoor.Phase.OPENING);
        assertThat(door.currentHeight()).isEqualTo(2.0F);

        door.onBeforePhysics(update(Duration.ofMillis(250)));

        assertThat(door.phase()).isEqualTo(DoomDoor.Phase.OPENED);
        assertThat(door.currentHeight()).isEqualTo(3.0F);
        assertThat(door.activate()).isFalse();
        assertThat(transform.position()).isEqualTo(new Vector3f(2.0F, 3.0F, 3.0F));
    }

    /** Holds a repeatable door open for its authored duration before returning exactly to its closed pose. */
    @Test
    void waitsAndCloses() {
        RecordingTransform transform = new RecordingTransform(0.0F, 0.0F, 0.0F);
        DoomDoorComponent door = new DoomDoorComponent(-1.0F, 1.0F, 8.0F, 0.5F);
        door.bindReferences(references(transform));

        door.activate();
        door.onBeforePhysics(update(Duration.ofMillis(250)));
        assertThat(door.phase()).isEqualTo(DoomDoor.Phase.WAITING);

        door.onBeforePhysics(update(Duration.ofMillis(250)));
        assertThat(door.phase()).isEqualTo(DoomDoor.Phase.WAITING);
        door.onBeforePhysics(update(Duration.ofMillis(250)));
        assertThat(door.phase()).isEqualTo(DoomDoor.Phase.CLOSING);
        door.onBeforePhysics(update(Duration.ofMillis(250)));

        assertThat(door.phase()).isEqualTo(DoomDoor.Phase.CLOSED);
        assertThat(door.currentHeight()).isEqualTo(-1.0F);
        assertThat(transform.position().y()).isEqualTo(-1.0F);
    }

    /** Reverses a closing door without resetting its current height. */
    @Test
    void reopensWhileClosing() {
        RecordingTransform transform = new RecordingTransform(0.0F, 0.0F, 0.0F);
        DoomDoorComponent door = new DoomDoorComponent(0.0F, 2.0F, 4.0F, 0.25F);
        door.bindReferences(references(transform));
        door.activate();
        door.onBeforePhysics(update(Duration.ofMillis(500)));
        door.onBeforePhysics(update(Duration.ofMillis(250)));
        door.onBeforePhysics(update(Duration.ofMillis(125)));
        float closingHeight = door.currentHeight();

        assertThat(door.phase()).isEqualTo(DoomDoor.Phase.CLOSING);
        assertThat(door.activate()).isTrue();
        assertThat(door.currentHeight()).isEqualTo(closingHeight);
        door.onBeforePhysics(update(Duration.ofMillis(125)));

        assertThat(door.phase()).isEqualTo(DoomDoor.Phase.WAITING);
        assertThat(door.currentHeight()).isEqualTo(2.0F);
    }

    /** Rejects non-finite, inverted, stationary, and negative-duration authored configurations. */
    @Test
    void rejectsInvalidConfiguration() {
        assertThatThrownBy(() -> new DoomDoorComponent(Float.NaN, 1.0F, 1.0F, 0.0F))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("closedHeight");
        assertThatThrownBy(() -> new DoomDoorComponent(2.0F, 1.0F, 1.0F, 0.0F))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("openHeight");
        assertThatThrownBy(() -> new DoomDoorComponent(0.0F, 1.0F, 0.0F, 0.0F))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("speed");
        assertThatThrownBy(() -> new DoomDoorComponent(0.0F, 1.0F, 1.0F, -1.0F))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("holdOpenSeconds");
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
                assertThat(property).isEqualTo(DoomDoorDescriptors.TRANSFORM_PROPERTY);
                return valueType.cast(transform);
            }

            @Override
            public <T> List<T> components(PropertyId property, Class<T> valueType) {
                throw new UnsupportedOperationException();
            }
        };
    }

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
