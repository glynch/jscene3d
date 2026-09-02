/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.physics.examples;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.glynch.jscene3d.examples.framework.ExampleDiagnostics;
import io.github.glynch.jscene3d.physics.examples.MovementDiagnostics.MovementKeys;
import org.joml.Vector3f;
import org.junit.jupiter.api.Test;

/** Verifies that movement diagnostics identify the boundary that stopped movement. */
final class MovementDiagnosticsTest {
    /** Disabled diagnostics neither expose stale values nor report a misleading failure. */
    @Test
    void remainsQuietWhileDisabled() {
        MovementDiagnostics diagnostics = new MovementDiagnostics(new ExampleDiagnostics(false));

        diagnostics.beginFrame(true, false, keys(true, false, false, false), new Vector3f(0.0F, 0.0F, -1.0F), 1);

        assertThat(diagnostics.keyStatus()).isEqualTo("disabled");
        assertThat(diagnostics.diagnosis()).isEqualTo("disabled");
    }

    /** Host keyboard ownership is distinguished from missing platform key state. */
    @Test
    void identifiesBrowserKeyboardCapture() {
        MovementDiagnostics diagnostics = enabledDiagnostics();

        diagnostics.beginFrame(true, true, keys(true, false, false, false), new Vector3f(), 1);

        assertThat(diagnostics.diagnosis()).isEqualTo("browser owns keyboard");
    }

    /** A requested translation that physics completely rejects is reported explicitly. */
    @Test
    void identifiesMovementBlockedByPhysics() {
        MovementDiagnostics diagnostics = enabledDiagnostics();
        diagnostics.beginFrame(true, false, keys(true, false, false, false), new Vector3f(0.0F, 0.0F, -1.0F), 1);

        diagnostics.recordStep(new Vector3f(0.0F, -0.01F, -0.03F), new Vector3f(0.0F, 0.0F, 0.0F));

        assertThat(diagnostics.diagnosis()).isEqualTo("physics blocked movement");
    }

    /** Successful horizontal movement reaches the final diagnostic state. */
    @Test
    void identifiesSuccessfulMovement() {
        MovementDiagnostics diagnostics = enabledDiagnostics();
        diagnostics.beginFrame(true, false, keys(false, false, false, true), new Vector3f(1.0F, 0.0F, 0.0F), 1);

        diagnostics.recordStep(new Vector3f(0.03F, -0.01F, 0.0F), new Vector3f(0.03F, 0.0F, 0.0F));

        assertThat(diagnostics.diagnosis()).isEqualTo("moving");
    }

    private static MovementDiagnostics enabledDiagnostics() {
        return new MovementDiagnostics(new ExampleDiagnostics(true));
    }

    private static MovementKeys keys(boolean wDown, boolean aDown, boolean sDown, boolean dDown) {
        return new MovementKeys(wDown, aDown, sDown, dDown);
    }
}
