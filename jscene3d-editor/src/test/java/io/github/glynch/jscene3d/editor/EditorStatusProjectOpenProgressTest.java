/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

/** Verifies in-session project loading is presented through concise status text. */
final class EditorStatusProjectOpenProgressTest {
    /** Includes project identity, current phase, and cumulative progress. */
    @Test
    void presentsProjectOpeningProgress() {
        AtomicReference<String> status = new AtomicReference<>();
        EditorStatusProjectOpenProgress progress = new EditorStatusProjectOpenProgress(status::set);

        progress.opening(Path.of("/projects/doomed-corridors"));
        assertThat(status).hasValue("Opening doomed-corridors · Reading the project manifest · 16%");

        progress.projectIdentified("Doomed Corridors");
        progress.phaseStarted(EditorLoadingPhase.PREPARING_PREVIEW);
        assertThat(status)
                .hasValue("Opening Doomed Corridors · Composing and presenting the first preview frame · 96%");

        progress.finish();
        assertThat(status)
                .hasValue("Opening Doomed Corridors · Composing and presenting the first preview frame · 96%");
    }
}
