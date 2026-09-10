/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.game.project3d;

import io.github.glynch.jscene3d.game.input.ActionSnapshot;
import io.github.glynch.jscene3d.game.input.InputAction;
import io.github.glynch.jscene3d.game.input.InputVector2;
import io.github.glynch.jscene3d.game.input.InputWorldModule;
import io.github.glynch.jscene3d.project.physics3d.CharacterBody3d;
import io.github.glynch.jscene3d.project.runtime.FixedUpdateContext;
import io.github.glynch.jscene3d.project.runtime.FrameUpdateContext;
import io.github.glynch.jscene3d.project.runtime.extension.ComponentReferenceBinder;
import io.github.glynch.jscene3d.project.runtime.extension.ComponentReferenceResolver;
import io.github.glynch.jscene3d.project.runtime.extension.ComponentUpdateCallbacks;
import io.github.glynch.jscene3d.project.spatial3d.Transform3d;
import java.util.Objects;
import java.util.Optional;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/** Converts semantic movement and look actions into character-body and view-transform changes. */
final class FirstPersonCharacterController3d implements ComponentReferenceBinder, ComponentUpdateCallbacks {
    private final InputWorldModule input;
    private final InputAction moveAction;
    private final InputAction lookAction;
    private final InputAction turnLeftAction;
    private final InputAction turnRightAction;
    private final float moveSpeed;
    private final float turnSpeed;
    private final float maximumKeyboardTurnSpeed;
    private final float keyboardTurnAcceleration;
    private final float pointerSensitivity;
    private final float maximumPitch;
    private Optional<CharacterBody3d> characterBody = Optional.empty();
    private Optional<Transform3d> viewTransform = Optional.empty();
    private float yaw;
    private float pitch;
    private float currentKeyboardTurnSpeed;
    private int keyboardTurnDirection;
    private boolean pointerLookConsumed;

    FirstPersonCharacterController3d(InputWorldModule input, Actions actions, Tuning tuning) {
        this.input = Objects.requireNonNull(input, "input");
        Actions validActions = Objects.requireNonNull(actions, "actions");
        moveAction = validActions.move();
        lookAction = validActions.look();
        turnLeftAction = validActions.turnLeft();
        turnRightAction = validActions.turnRight();
        Tuning validTuning = Objects.requireNonNull(tuning, "tuning");
        moveSpeed = requirePositive(validTuning.moveSpeed(), "moveSpeed");
        turnSpeed = (float) Math.toRadians(requirePositive(validTuning.turnSpeedDegrees(), "turnSpeedDegrees"));
        maximumKeyboardTurnSpeed = (float) Math.toRadians(requireAtLeast(
                validTuning.maximumKeyboardTurnSpeedDegrees(),
                validTuning.turnSpeedDegrees(),
                "maximumKeyboardTurnSpeedDegrees"));
        keyboardTurnAcceleration = (float) Math.toRadians(
                requirePositive(validTuning.keyboardTurnAccelerationDegrees(), "keyboardTurnAccelerationDegrees"));
        currentKeyboardTurnSpeed = turnSpeed;
        pointerSensitivity = requirePositive(validTuning.pointerSensitivity(), "pointerSensitivity");
        maximumPitch = (float) Math.toRadians(requirePitch(validTuning.maximumPitchDegrees()));
    }

    @Override
    public void bindReferences(ComponentReferenceResolver references) {
        Objects.requireNonNull(references, "references");
        characterBody = Optional.of(references.component(Game3dDescriptors.bodyProperty(), CharacterBody3d.class));
        Transform3d resolvedView = references.component(Game3dDescriptors.viewTransformProperty(), Transform3d.class);
        viewTransform = Optional.of(resolvedView);
        Vector3f angles = resolvedView.orientation().getEulerAnglesYXZ(new Vector3f());
        pitch = Math.clamp(angles.x, -maximumPitch, maximumPitch);
        yaw = angles.y;
    }

    @Override
    public void onBeforePhysics(FixedUpdateContext update) {
        Objects.requireNonNull(update, "update");
        synchronizeViewOrientation();
        ActionSnapshot snapshot = input.snapshot();
        applyPointerLook(snapshot);
        applyContinuousLook(snapshot, update);
        requiredBody().move(planarVelocity(snapshot.axis2d(moveAction)), update.step());
    }

    @Override
    public void onFrameUpdate(FrameUpdateContext update) {
        Objects.requireNonNull(update, "update");
        pointerLookConsumed = false;
    }

    private void applyPointerLook(ActionSnapshot snapshot) {
        double horizontal = snapshot.pointerDeltaX();
        double vertical = snapshot.pointerDeltaY();
        if (pointerLookConsumed || horizontal == 0.0 && vertical == 0.0) {
            return;
        }
        yaw -= (float) horizontal * pointerSensitivity;
        pitch = Math.clamp(pitch - (float) vertical * pointerSensitivity, -maximumPitch, maximumPitch);
        pointerLookConsumed = true;
        updateViewOrientation();
    }

    private void applyContinuousLook(ActionSnapshot snapshot, FixedUpdateContext update) {
        InputVector2 look = pointerLookConsumed ? InputVector2.ZERO : snapshot.axis2d(lookAction);
        float keyboardTurn = snapshot.axis(turnLeftAction, turnRightAction);
        float elapsedSeconds = update.step().toNanos() / 1_000_000_000.0F;
        yaw -= look.x() * turnSpeed * elapsedSeconds;
        yaw -= keyboardTurnRate(keyboardTurn, elapsedSeconds) * elapsedSeconds;
        pitch = Math.clamp(pitch + look.y() * turnSpeed * elapsedSeconds, -maximumPitch, maximumPitch);
        if (!look.equals(InputVector2.ZERO) || keyboardTurn != 0.0F) {
            updateViewOrientation();
        }
    }

    /** Returns the signed keyboard turn rate for this step, advancing held-key acceleration for the next step. */
    private float keyboardTurnRate(float input, float elapsedSeconds) {
        int direction = Float.compare(input, 0.0F);
        if (direction == 0) {
            keyboardTurnDirection = 0;
            currentKeyboardTurnSpeed = turnSpeed;
            return 0.0F;
        }
        if (direction != keyboardTurnDirection) {
            keyboardTurnDirection = direction;
            currentKeyboardTurnSpeed = turnSpeed;
        }
        float rate = input * currentKeyboardTurnSpeed;
        currentKeyboardTurnSpeed = Math.min(
                maximumKeyboardTurnSpeed, currentKeyboardTurnSpeed + keyboardTurnAcceleration * elapsedSeconds);
        return rate;
    }

    private void updateViewOrientation() {
        Quaternionf orientation = new Quaternionf().rotationYXZ(yaw, pitch, 0.0F);
        requiredViewTransform().setOrientation(orientation.x, orientation.y, orientation.z, orientation.w);
    }

    /** Adopts an externally staged view pose before calculating look input or view-relative movement. */
    private void synchronizeViewOrientation() {
        Vector3f angles = requiredViewTransform().orientation().getEulerAnglesYXZ(new Vector3f());
        pitch = Math.clamp(angles.x, -maximumPitch, maximumPitch);
        yaw = angles.y;
    }

    private Vector3f planarVelocity(InputVector2 move) {
        Vector3f velocity = new Vector3f(move.x(), 0.0F, -move.y());
        if (velocity.lengthSquared() > 1.0F) {
            velocity.normalize();
        }
        return velocity.rotateY(yaw).mul(moveSpeed);
    }

    private CharacterBody3d requiredBody() {
        return characterBody.orElseThrow(() -> new IllegalStateException("character body has not been bound"));
    }

    private Transform3d requiredViewTransform() {
        return viewTransform.orElseThrow(() -> new IllegalStateException("view transform has not been bound"));
    }

    private static float requirePositive(float value, String name) {
        if (!Float.isFinite(value) || value <= 0.0F) {
            throw new IllegalArgumentException(name + " must be finite and positive: " + value);
        }
        return value;
    }

    private static float requirePitch(float value) {
        if (!Float.isFinite(value) || value <= 0.0F || value >= 90.0F) {
            throw new IllegalArgumentException("maximumPitchDegrees must be finite and in (0, 90): " + value);
        }
        return value;
    }

    private static float requireAtLeast(float value, float minimum, String name) {
        if (!Float.isFinite(value) || value < minimum) {
            throw new IllegalArgumentException(name + " must be finite and at least " + minimum + ": " + value);
        }
        return value;
    }

    record Actions(InputAction move, InputAction look, InputAction turnLeft, InputAction turnRight) {
        Actions {
            Objects.requireNonNull(move, "move");
            Objects.requireNonNull(look, "look");
            Objects.requireNonNull(turnLeft, "turnLeft");
            Objects.requireNonNull(turnRight, "turnRight");
        }
    }

    record Tuning(
            float moveSpeed,
            float turnSpeedDegrees,
            float maximumKeyboardTurnSpeedDegrees,
            float keyboardTurnAccelerationDegrees,
            float pointerSensitivity,
            float maximumPitchDegrees) {}
}
