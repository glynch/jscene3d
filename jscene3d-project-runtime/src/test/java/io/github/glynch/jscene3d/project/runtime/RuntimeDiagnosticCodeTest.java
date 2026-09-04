/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.runtime;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/** Verifies the runtime diagnostic localization catalog. */
final class RuntimeDiagnosticCodeTest {
    /** Requires unique keys and non-blank English fallbacks. */
    @Test
    void validatesRuntimeDiagnosticCatalog() {
        assertThat(RuntimeDiagnosticCode.values())
                .extracting(RuntimeDiagnosticCode::code)
                .doesNotHaveDuplicates()
                .allSatisfy(code -> assertThat(code).isNotBlank());
        assertThat(RuntimeDiagnosticCode.values())
                .extracting(RuntimeDiagnosticCode::defaultMessage)
                .allSatisfy(message -> assertThat(message).isNotBlank());
    }
}
