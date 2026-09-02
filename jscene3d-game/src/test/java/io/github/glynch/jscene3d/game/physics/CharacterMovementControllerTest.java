/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.game.physics;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.glynch.jscene3d.game.input.ActionSnapshot;
import io.github.glynch.jscene3d.game.input.InputAction;
import io.github.glynch.jscene3d.physics.CharacterController;
import io.github.glynch.jscene3d.physics.KinematicBody;
import io.github.glynch.jscene3d.physics.PhysicsWorld;
import io.github.glynch.jscene3d.physics.movement.CharacterControllerSettings;
import io.github.glynch.jscene3d.physics.movement.CharacterMoveResult;
import io.github.glynch.jscene3d.physics.movement.KinematicMoveSettings;
import io.github.glynch.jscene3d.physics.shapes.BoxShape;
import io.github.glynch.jscene3d.physics.shapes.CapsuleShape;
import java.time.Duration;
import org.assertj.core.data.Offset;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.junit.jupiter.api.Test;

final class CharacterMovementControllerTest {
    private static final InputAction FORWARD = new InputAction("forward");
    private static final InputAction BACKWARD = new InputAction("backward");
    private static final InputAction LEFT = new InputAction("left");
    private static final InputAction RIGHT = new InputAction("right");
    private static final InputAction JUMP = new InputAction("jump");
    private static final CharacterMovementActions ACTIONS =
            new CharacterMovementActions(FORWARD, BACKWARD, LEFT, RIGHT, JUMP);
    private static final Offset<Float> TOLERANCE = Offset.offset(1.0E-5F);

    @Test
    void convertsCameraRelativeDiagonalInputIntoNormalizedPlanarMovement() {
        PhysicsWorld world = new PhysicsWorld();
        CharacterMovementController controller = controller(world, new Vector3f(0.0F, 1.0F, 0.0F), 6.0F);
        ActionSnapshot input =
                ActionSnapshot.builder().down(FORWARD).down(RIGHT).build();

        CharacterMoveResult result = controller.move(input, new Vector3f(0.0F, 2.0F, -2.0F), Duration.ofMillis(500L));

        float expectedComponent = (float) (3.0 / Math.sqrt(2.0));
        Vector3f translation = result.appliedTranslation(new Vector3f());
        assertThat(translation.x).isCloseTo(expectedComponent, TOLERANCE);
        assertThat(translation.y).isCloseTo(0.0F, TOLERANCE);
        assertThat(translation.z).isCloseTo(-expectedComponent, TOLERANCE);
        assertThat(controller.desiredVelocity(new Vector3f()).length()).isCloseTo(6.0F, TOLERANCE);
        assertThat(controller.facingDirection(new Vector3f()).length()).isCloseTo(1.0F, TOLERANCE);
        assertThat(controller.moveSpeed()).isEqualTo(6.0F);
        assertThat(controller.actions()).isEqualTo(ACTIONS);
    }

    @Test
    void retainsTheLastPlanarDirectionWhenViewForwardIsVertical() {
        PhysicsWorld world = new PhysicsWorld();
        CharacterMovementController controller = controller(world, new Vector3f(0.0F, 1.0F, 0.0F), 2.0F);
        ActionSnapshot moving = ActionSnapshot.builder().down(FORWARD).build();

        Duration fixedStep = Duration.ofMillis(250L);
        controller.move(moving, new Vector3f(1.0F, 0.0F, 0.0F), fixedStep);
        controller.move(ActionSnapshot.empty(), new Vector3f(0.0F, 1.0F, 0.0F), fixedStep);
        CharacterMoveResult result = controller.move(moving, new Vector3f(0.0F, 1.0F, 0.0F), fixedStep);

        Vector3f translation = result.appliedTranslation(new Vector3f());
        assertThat(translation.x).isCloseTo(0.5F, TOLERANCE);
        assertThat(translation.y).isCloseTo(0.0F, TOLERANCE);
        assertThat(translation.z).isCloseTo(0.0F, TOLERANCE);
    }

    @Test
    void forwardsPressedJumpActionsToGroundedCharacterPhysics() {
        PhysicsWorld world = new PhysicsWorld();
        world.addStaticBody(new Vector3f(0.0F, -0.5F, 0.0F), new Quaternionf())
                .addCollider(new BoxShape(20.0F, 1.0F, 20.0F));
        KinematicBody body = world.addKinematicBody(new Vector3f(0.0F, 1.001F, 0.0F), new Quaternionf());
        body.addCollider(new CapsuleShape(0.5F, 1.0F));
        CharacterController physicsController = new CharacterController(world, body);
        CharacterMovementController controller = new CharacterMovementController(physicsController, ACTIONS, 4.0F);
        Duration fixedStep = Duration.ofNanos(1_000_000_000L / 120L);
        controller.move(ActionSnapshot.empty(), new Vector3f(0.0F, 0.0F, -1.0F), fixedStep);
        ActionSnapshot jump = ActionSnapshot.builder().pressed(JUMP).build();

        CharacterMoveResult result = controller.move(jump, new Vector3f(0.0F, 0.0F, -1.0F), fixedStep);

        assertThat(result.jumped()).isTrue();
        assertThat(result.appliedTranslation(new Vector3f()).y).isPositive();
    }

    @Test
    void supportsPhysicsWorldsWhoseUpAxisIsNotY() {
        PhysicsWorld world = new PhysicsWorld();
        CharacterMovementController controller = controller(world, new Vector3f(0.0F, 0.0F, 1.0F), 3.0F);
        Vector3f initialFacing = controller.facingDirection(new Vector3f());

        CharacterMoveResult result = controller.move(
                ActionSnapshot.builder().down(FORWARD).build(),
                new Vector3f(0.0F, 0.0F, 1.0F),
                Duration.ofMillis(500L));

        assertThat(initialFacing.y).isCloseTo(1.0F, TOLERANCE);
        assertThat(result.appliedTranslation(new Vector3f()).z).isCloseTo(0.0F, TOLERANCE);
    }

    @Test
    void rejectsInvalidConfigurationAndMovementArguments() {
        PhysicsWorld world = new PhysicsWorld();
        CharacterController physicsController = physicsController(world, new Vector3f(0.0F, 1.0F, 0.0F));
        Vector3f forward = new Vector3f(0.0F, 0.0F, -1.0F);
        Vector3f invalidForward = new Vector3f(Float.NaN, 0.0F, 0.0F);
        ActionSnapshot emptyInput = ActionSnapshot.empty();
        assertThatThrownBy(() -> new CharacterMovementController(physicsController, ACTIONS, 0.0F))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new CharacterMovementController(physicsController, ACTIONS, Float.NaN))
                .isInstanceOf(IllegalArgumentException.class);
        CharacterMovementController controller = new CharacterMovementController(physicsController, ACTIONS, 1.0F);
        Duration fixedStep = Duration.ofMillis(100L);
        assertThatThrownBy(() -> controller.move(emptyInput, invalidForward, fixedStep))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> controller.move(emptyInput, forward, Duration.ZERO))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private static CharacterMovementController controller(PhysicsWorld world, Vector3f up, float moveSpeed) {
        return new CharacterMovementController(physicsController(world, up), ACTIONS, moveSpeed);
    }

    private static CharacterController physicsController(PhysicsWorld world, Vector3f up) {
        KinematicBody body = world.addKinematicBody();
        body.addCollider(new CapsuleShape(0.5F, 1.0F));
        KinematicMoveSettings movementSettings = KinematicMoveSettings.DEFAULT.withUp(up);
        CharacterControllerSettings settings = CharacterControllerSettings.DEFAULT
                .withMovementSettings(movementSettings)
                .withGravity(0.0F)
                .withJumpSpeed(0.0F);
        return new CharacterController(world, body, settings);
    }
}
