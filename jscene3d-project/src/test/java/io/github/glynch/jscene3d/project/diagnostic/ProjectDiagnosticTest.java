/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.diagnostic;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.glynch.jscene3d.project.manifest.ProjectDiagnosticCode;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/** Verifies immutable localization-safe diagnostic message arguments. */
final class ProjectDiagnosticTest {
    /** Copies arguments in their declared order and rejects later mutation. */
    @Test
    void retainsImmutableOrderedMessageArguments() {
        List<Object> arguments = new ArrayList<>(List.of("expected", "actual"));
        ProjectDiagnostic diagnostic = new ProjectDiagnostic(
                ProjectDiagnostic.Severity.ERROR,
                ProjectDiagnosticCode.ENGINE_INCOMPATIBLE,
                URI.create("file:///project/game.j3d"),
                "/engine/requires",
                arguments,
                Map.of("technicalDetail", "debug only"));

        arguments.set(0, "changed");
        List<Object> immutableArguments = diagnostic.messageArguments();

        assertThat(immutableArguments).containsExactly("expected", "actual");
        assertThatThrownBy(() -> immutableArguments.add("later")).isInstanceOf(UnsupportedOperationException.class);
    }

    /** The compatibility constructor expresses a diagnostic with no formatting arguments. */
    @Test
    void defaultsMessageArgumentsToEmpty() {
        ProjectDiagnostic diagnostic = new ProjectDiagnostic(
                ProjectDiagnostic.Severity.WARNING,
                ProjectDiagnosticCode.DESCRIPTOR_LEGACY,
                URI.create("file:///project/jscene3d.json"),
                "",
                Map.of());

        assertThat(diagnostic.messageArguments()).isEmpty();
    }
}
