/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor;

import java.time.Duration;
import java.util.Optional;

/** Completed durations for one editor project-open path. */
public record EditorProjectOpenDurations(
        Duration total, Duration projectLoad, Duration previewComposition, Optional<Duration> firstPresentation) {}
