/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.game.project3d;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.glynch.jscene3d.game.input.ActionSnapshot;
import io.github.glynch.jscene3d.game.input.InputAction;
import io.github.glynch.jscene3d.game.input.InputWorldModule;
import io.github.glynch.jscene3d.project.component.ComponentId;
import io.github.glynch.jscene3d.project.component.PropertyId;
import io.github.glynch.jscene3d.project.physics3d.CharacterBody3d;
import io.github.glynch.jscene3d.project.physics3d.CharacterMove3dResult;
import io.github.glynch.jscene3d.project.physics3d.CollisionShape3d;
import io.github.glynch.jscene3d.project.runtime.Entity;
import io.github.glynch.jscene3d.project.runtime.FixedUpdateContext;
import io.github.glynch.jscene3d.project.runtime.FrameUpdateContext;
import io.github.glynch.jscene3d.project.runtime.extension.ComponentReferenceResolver;
import io.github.glynch.jscene3d.project.spatial3d.Transform3d;
import java.time.Duration;
import java.util.List;
import org.assertj.core.data.Offset;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.joml.Quaternionf;
import org.joml.Quaternionfc;
import org.joml.Vector3f;
import org.joml.Vector3fc;
import org.junit.jupiter.api.Test;

final class FirstPersonCharacterController3dTest {
    private static final InputAction MOVE = new InputAction("move");
    private static final InputAction LOOK = new InputAction("look");
    private static final InputAction TURN_LEFT = new InputAction("turn-left");
    private static final InputAction TURN_RIGHT = new InputAction("turn-right");
    private static final Offset<Float> TOLERANCE = Offset.offset(1.0E-5F);

    @Test
    void movesRelativeToTheView() {
        MutableInput input = new MutableInput();
        RecordingBody body = new RecordingBody();
        RecordingTransform view = new RecordingTransform();
        FirstPersonCharacterController3d controller = controller(input);
        controller.bindReferences(references(body, view));
        input.snapshot = ActionSnapshot.builder().axis2d(MOVE, 1.0F, 1.0F).build();
        FixedUpdateContext fixed = new FixedUpdateContext(0, Duration.ofMillis(25), Duration.ZERO);

        controller.onBeforePhysics(fixed);

        assertThat(body.velocity.x).isCloseTo(5.656854F, TOLERANCE);
        assertThat(body.velocity.z).isCloseTo(-5.656854F, TOLERANCE);
        assertThat(body.step).isEqualTo(Duration.ofMillis(25));
    }

    @Test
    void consumesPointerMotionOncePerFrame() {
        MutableInput input = new MutableInput();
        RecordingTransform view = new RecordingTransform();
        FirstPersonCharacterController3d controller = controller(input);
        controller.bindReferences(references(new RecordingBody(), view));
        input.snapshot = ActionSnapshot.builder().pointerDelta(100.0, 50.0).build();
        FixedUpdateContext fixed = new FixedUpdateContext(0, Duration.ofMillis(25), Duration.ZERO);

        controller.onBeforePhysics(fixed);
        Quaternionf firstOrientation = new Quaternionf(view.orientation);
        controller.onBeforePhysics(fixed);

        assertThat(view.orientation).isEqualTo(firstOrientation);

        controller.onFrameUpdate(new FrameUpdateContext(Duration.ofMillis(16), Duration.ofMillis(25), 0.0F));
        controller.onBeforePhysics(fixed);

        assertThat(view.orientation).isNotEqualTo(firstOrientation);
    }

    @Test
    void appliesHeldLookAndKeyboardTurningUsingFixedElapsedTime() {
        MutableInput input = new MutableInput();
        RecordingTransform view = new RecordingTransform();
        FirstPersonCharacterController3d controller = controller(input);
        controller.bindReferences(references(new RecordingBody(), view));
        input.snapshot = ActionSnapshot.builder()
                .axis2d(LOOK, 0.2F, 0.1F)
                .down(TURN_RIGHT)
                .build();

        controller.onBeforePhysics(new FixedUpdateContext(0, Duration.ofMillis(100), Duration.ZERO));

        Vector3f angles = view.orientation.getEulerAnglesYXZ(new Vector3f());
        assertThat(angles.y).isCloseTo(-(float) Math.toRadians(21.6), TOLERANCE);
        assertThat(angles.x).isCloseTo((float) Math.toRadians(1.8), TOLERANCE);
    }

    @Test
    void acceleratesHeldKeyboardTurningToAnAuthoredLimitAndResetsAfterRelease() {
        MutableInput input = new MutableInput();
        RecordingTransform view = new RecordingTransform();
        FirstPersonCharacterController3d controller = new FirstPersonCharacterController3d(
                input,
                new FirstPersonCharacterController3d.Actions(MOVE, LOOK, TURN_LEFT, TURN_RIGHT),
                new FirstPersonCharacterController3d.Tuning(8.0F, 180.0F, 234.0F, 540.0F, 0.0015F, 85.0F));
        controller.bindReferences(references(new RecordingBody(), view));
        FixedUpdateContext fixed = new FixedUpdateContext(0, Duration.ofMillis(100), Duration.ZERO);
        input.snapshot = ActionSnapshot.builder().down(TURN_RIGHT).build();

        controller.onBeforePhysics(fixed);
        float firstYaw = view.orientation.getEulerAnglesYXZ(new Vector3f()).y;
        controller.onBeforePhysics(fixed);
        float secondYaw = view.orientation.getEulerAnglesYXZ(new Vector3f()).y;
        controller.onBeforePhysics(fixed);
        float thirdYaw = view.orientation.getEulerAnglesYXZ(new Vector3f()).y;
        controller.onBeforePhysics(fixed);
        float fourthYaw = view.orientation.getEulerAnglesYXZ(new Vector3f()).y;

        assertThat(firstYaw).isCloseTo(-(float) Math.toRadians(18.0), TOLERANCE);
        assertThat(secondYaw - firstYaw).isCloseTo(-(float) Math.toRadians(23.4), TOLERANCE);
        assertThat(thirdYaw - secondYaw).isCloseTo(-(float) Math.toRadians(23.4), TOLERANCE);
        assertThat(fourthYaw - thirdYaw).isCloseTo(-(float) Math.toRadians(23.4), TOLERANCE);

        input.snapshot = ActionSnapshot.empty();
        controller.onBeforePhysics(fixed);
        input.snapshot = ActionSnapshot.builder().down(TURN_RIGHT).build();
        controller.onBeforePhysics(fixed);
        float resetYaw = view.orientation.getEulerAnglesYXZ(new Vector3f()).y;

        assertThat(resetYaw - fourthYaw).isCloseTo(-(float) Math.toRadians(18.0), TOLERANCE);
    }

    @Test
    void validatesRuntimeTuningBeyondDescriptorKinds() {
        MutableInput input = new MutableInput();
        FirstPersonCharacterController3d.Actions actions =
                new FirstPersonCharacterController3d.Actions(MOVE, LOOK, TURN_LEFT, TURN_RIGHT);
        FirstPersonCharacterController3d.Tuning zeroMoveSpeed =
                new FirstPersonCharacterController3d.Tuning(0.0F, 180.0F, 360.0F, 540.0F, 0.001F, 85.0F);
        FirstPersonCharacterController3d.Tuning invalidPitch =
                new FirstPersonCharacterController3d.Tuning(8.0F, 180.0F, 360.0F, 540.0F, 0.001F, 90.0F);

        assertThatThrownBy(() -> new FirstPersonCharacterController3d(input, actions, zeroMoveSpeed))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("moveSpeed");
        assertThatThrownBy(() -> new FirstPersonCharacterController3d(input, actions, invalidPitch))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("maximumPitchDegrees");
    }

    private static FirstPersonCharacterController3d controller(InputWorldModule input) {
        return new FirstPersonCharacterController3d(
                input,
                new FirstPersonCharacterController3d.Actions(MOVE, LOOK, TURN_LEFT, TURN_RIGHT),
                new FirstPersonCharacterController3d.Tuning(8.0F, 180.0F, 360.0F, 540.0F, 0.0015F, 85.0F));
    }

    private static ComponentReferenceResolver references(CharacterBody3d body, Transform3d view) {
        return new ComponentReferenceResolver() {
            @Override
            public Entity entity(PropertyId property) {
                throw new UnsupportedOperationException();
            }

            @Override
            public <T> T component(PropertyId property, Class<T> valueType) {
                Object value = property.equals(Game3dDescriptors.bodyProperty()) ? body : view;
                return valueType.cast(value);
            }

            @Override
            public <T> List<T> components(PropertyId property, Class<T> valueType) {
                throw new UnsupportedOperationException();
            }
        };
    }

    private static final class MutableInput implements InputWorldModule {
        private ActionSnapshot snapshot = ActionSnapshot.empty();

        @Override
        public ActionSnapshot snapshot() {
            return snapshot;
        }

        @Override
        public void close() {
            // The fixture owns no resources.
        }
    }

    private static final class RecordingBody implements CharacterBody3d {
        private final Vector3f velocity = new Vector3f();
        private Duration step;

        @Override
        public CharacterMove3dResult move(Vector3fc planarVelocity, Duration fixedStep) {
            velocity.set(planarVelocity);
            step = fixedStep;
            return null;
        }

        @Override
        public boolean tryJump() {
            return false;
        }

        @Override
        public boolean isGrounded() {
            return false;
        }

        @Override
        public Vector3f groundNormal(Vector3f destination) {
            return destination.zero();
        }

        @Override
        public Entity owner() {
            throw new UnsupportedOperationException();
        }

        @Override
        public ComponentId componentId() {
            throw new UnsupportedOperationException();
        }

        @Override
        public List<CollisionShape3d> shapes() {
            return List.of();
        }

        @Override
        public boolean isActive() {
            return true;
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

    private static final class RecordingTransform implements Transform3d {
        private final Vector3f position = new Vector3f();
        private final Quaternionf orientation = new Quaternionf();
        private final Vector3f scale = new Vector3f(1.0F);

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
            orientation.set(x, y, z, w).normalize();
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
