/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.physics.examples;

import io.github.glynch.jscene3d.examples.framework.ExampleDiagnostics;
import java.util.Locale;
import org.joml.Vector3f;
import org.joml.Vector3fc;

/** Captures the observable boundaries of one kinematic movement frame. */
final class MovementDiagnostics {
    private static final float MOVEMENT_EPSILON_SQUARED = 0.00000001F;

    private final ExampleDiagnostics runtime;
    private final Vector3f movementInput = new Vector3f();
    private final Vector3f requestedDisplacement = new Vector3f();
    private final Vector3f actualDisplacement = new Vector3f();

    private boolean windowFocused;
    private boolean keyboardCaptured;
    private MovementKeys keys = new MovementKeys(false, false, false, false);
    private int fixedSteps;

    MovementDiagnostics(ExampleDiagnostics runtime) {
        this.runtime = runtime;
    }

    boolean isEnabled() {
        return runtime.isEnabled();
    }

    void setEnabled(boolean enabled) {
        runtime.setEnabled(enabled);
    }

    void beginFrame(
            boolean windowFocused,
            boolean keyboardCaptured,
            MovementKeys keys,
            Vector3fc movementInput,
            int fixedSteps) {
        if (!isEnabled()) {
            return;
        }
        this.windowFocused = windowFocused;
        this.keyboardCaptured = keyboardCaptured;
        this.keys = keys;
        this.movementInput.set(movementInput);
        this.fixedSteps = fixedSteps;
        requestedDisplacement.zero();
        actualDisplacement.zero();
    }

    void recordStep(Vector3fc requested, Vector3fc actual) {
        if (!isEnabled()) {
            return;
        }
        requestedDisplacement.add(requested);
        actualDisplacement.add(actual);
    }

    String windowFocusStatus() {
        return value(windowFocused ? "focused" : "not focused");
    }

    String keyboardOwnershipStatus() {
        return value(keyboardCaptured ? "captured by browser" : "available to example");
    }

    String keyStatus() {
        return value(String.format(
                Locale.ROOT, "W:%s A:%s S:%s D:%s", keys.wDown(), keys.aDown(), keys.sDown(), keys.dDown()));
    }

    String movementInputStatus() {
        return vectorStatus(movementInput);
    }

    String requestedDisplacementStatus() {
        return vectorStatus(requestedDisplacement);
    }

    String actualDisplacementStatus() {
        return vectorStatus(actualDisplacement);
    }

    String fixedStepStatus() {
        return value(Integer.toString(fixedSteps));
    }

    String diagnosis() {
        if (!isEnabled()) {
            return "disabled";
        }
        if (!windowFocused) {
            return "window is not focused";
        }
        if (keyboardCaptured) {
            return "browser owns keyboard";
        }
        if (!keys.anyDown()) {
            return "waiting for W A S D";
        }
        if (movementInput.lengthSquared() <= MOVEMENT_EPSILON_SQUARED) {
            return "input mapping produced no movement";
        }
        if (fixedSteps == 0) {
            return "waiting for fixed step";
        }
        if (horizontalLengthSquared(requestedDisplacement) <= MOVEMENT_EPSILON_SQUARED) {
            return "controller requested no movement";
        }
        if (horizontalLengthSquared(actualDisplacement) <= MOVEMENT_EPSILON_SQUARED) {
            return "physics blocked movement";
        }
        return "moving";
    }

    private String vectorStatus(Vector3fc vector) {
        return value(String.format(Locale.ROOT, "%.4f, %.4f, %.4f", vector.x(), vector.y(), vector.z()));
    }

    private String value(String enabledValue) {
        return isEnabled() ? enabledValue : "disabled";
    }

    private static float horizontalLengthSquared(Vector3fc vector) {
        return vector.x() * vector.x() + vector.z() * vector.z();
    }

    /** Held state of the four movement keys for one input poll. */
    record MovementKeys(boolean wDown, boolean aDown, boolean sDown, boolean dDown) {
        boolean anyDown() {
            return wDown || aDown || sDown || dDown;
        }
    }
}
