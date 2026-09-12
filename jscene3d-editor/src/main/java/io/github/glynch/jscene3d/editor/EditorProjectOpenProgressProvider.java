/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor;

import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;
import org.jspecify.annotations.Nullable;

/** Supplies startup progress once before using in-session progress for subsequent project opens. */
final class EditorProjectOpenProgressProvider implements Supplier<EditorProjectOpenProgress> {
    private final Supplier<EditorProjectOpenProgress> inSessionProgress;
    private @Nullable EditorProjectOpenProgress startupProgress;

    /** Creates a provider with optional startup progress and a reusable in-session factory. */
    EditorProjectOpenProgressProvider(
            Optional<EditorProjectOpenProgress> startupProgress,
            Supplier<EditorProjectOpenProgress> inSessionProgress) {
        this.startupProgress =
                Objects.requireNonNull(startupProgress, "startupProgress").orElse(null);
        this.inSessionProgress = Objects.requireNonNull(inSessionProgress, "inSessionProgress");
    }

    /** Returns startup progress once, then creates fresh in-session progress presentations. */
    @Override
    public EditorProjectOpenProgress get() {
        EditorProjectOpenProgress startup = startupProgress;
        startupProgress = null;
        return startup == null ? Objects.requireNonNull(inSessionProgress.get(), "inSessionProgress result") : startup;
    }
}
