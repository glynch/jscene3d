/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.workbench.selection;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/** Exercises selection across context-driven availability changes. */
final class EditorAvailabilitySelectionTest {
    @Test
    void selectsANewlyAvailableLeadingItem() {
        assertThat(EditorAvailabilitySelection.reconcile(
                        List.of("Diagnostics"), Optional.of("Diagnostics"), List.of("Project", "Diagnostics")))
                .contains("Project");
    }

    @Test
    void preservesSelectionWhenTheLeadingItemWasAlreadyAvailable() {
        assertThat(EditorAvailabilitySelection.reconcile(
                        List.of("Project", "Diagnostics"),
                        Optional.of("Diagnostics"),
                        List.of("Project", "Diagnostics")))
                .contains("Diagnostics");
    }

    @Test
    void fallsBackToTheLeadingItemWhenSelectionDisappears() {
        assertThat(EditorAvailabilitySelection.reconcile(
                        List.of("Project", "Diagnostics"), Optional.of("Project"), List.of("Diagnostics")))
                .contains("Diagnostics");
    }
}
