/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.authoring.protocol;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import org.junit.jupiter.api.Test;

/** Exercises invariants enforced at the versioned wire-DTO boundary. */
final class ProtocolDtoValidationTest {
    /** Rejects negative major or minor protocol components independently. */
    @Test
    void rejectsNegativeProtocolVersionComponents() {
        assertThatThrownBy(() -> new ProtocolVersion(-1, 0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("non-negative");
        assertThatThrownBy(() -> new ProtocolVersion(1, -1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("non-negative");
    }

    /** Rejects blank project selections before they reach path or domain loading. */
    @Test
    void rejectsBlankProjectPath() {
        assertThatThrownBy(() -> new ProjectOpenParams(" \t"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("blank");
    }

    /** Rejects inconsistent success and failure result shapes. */
    @Test
    void rejectsInconsistentProjectOpenResults() {
        ProjectSummary summary = summary();
        List<ProjectDiagnosticDto> diagnostics = List.of();

        assertThatThrownBy(() -> new ProjectOpenResult(true, null, summary, diagnostics, null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new ProjectOpenResult(true, 1L, null, diagnostics, null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new ProjectOpenResult(false, 1L, null, diagnostics, null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new ProjectOpenResult(false, null, summary, diagnostics, null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    /** Rejects either negative authored or projected asset counts. */
    @Test
    void rejectsNegativeAssetCounts() {
        assertThatThrownBy(() -> new ProjectSummary.AssetCounts(-1, 0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("non-negative");
        assertThatThrownBy(() -> new ProjectSummary.AssetCounts(0, -1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("non-negative");
    }

    /** Carries the explicit positive orderly-shutdown acknowledgement. */
    @Test
    void representsAcceptedShutdown() {
        assertThat(new ShutdownResult(true).shutdown()).isTrue();
    }

    /** Creates one valid project summary for result-shape validation. */
    private static ProjectSummary summary() {
        return new ProjectSummary(
                "project",
                "Project",
                "1.0.0",
                "/project",
                "/project/project.j3d",
                new ProjectSummary.WorldSummary("world", "World"),
                new ProjectSummary.AssetCounts(0, 0));
    }
}
