/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Path;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

/** Verifies startup progress cannot be reused for an in-session project open. */
final class EditorProjectOpenProgressProviderTest {
    /** Supplies startup progress once and creates in-session progress thereafter. */
    @Test
    void consumesStartupProgressOnce() {
        RecordingProgress startup = new RecordingProgress();
        AtomicInteger inSessionPresentations = new AtomicInteger();
        EditorProjectOpenProgressProvider progress = new EditorProjectOpenProgressProvider(Optional.of(startup), () -> {
            inSessionPresentations.incrementAndGet();
            return new RecordingProgress();
        });

        assertThat(progress.get()).isSameAs(startup);
        assertThat(progress.get()).isNotSameAs(startup);
        assertThat(progress.get()).isNotSameAs(startup);
        assertThat(inSessionPresentations).hasValue(2);
    }

    private static final class RecordingProgress implements EditorProjectOpenProgress {
        @Override
        public void opening(Path projectDirectory) {
            throw new UnsupportedOperationException("not used by this test");
        }

        @Override
        public void projectIdentified(String projectName) {
            throw new UnsupportedOperationException("not used by this test");
        }

        @Override
        public void phaseStarted(EditorLoadingPhase phase) {
            throw new UnsupportedOperationException("not used by this test");
        }

        @Override
        public void finish() {
            throw new UnsupportedOperationException("not used by this test");
        }
    }
}
