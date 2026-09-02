/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.examples.framework;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/** Verifies the reusable runtime diagnostics switch. */
final class ExampleDiagnosticsTest {
    /** Diagnostics remain off unless explicitly requested. */
    @Test
    void supportsAnExplicitDisabledDefault() {
        ExampleDiagnostics diagnostics = new ExampleDiagnostics(false);

        assertThat(diagnostics.isEnabled()).isFalse();
    }

    /** The control panel can change diagnostics without rebuilding the example. */
    @Test
    void changesAtRuntime() {
        ExampleDiagnostics diagnostics = new ExampleDiagnostics(false);

        diagnostics.setEnabled(true);

        assertThat(diagnostics.isEnabled()).isTrue();
    }
}
