/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.glynch.jscene3d.telemetry.Telemetry;
import io.github.glynch.jscene3d.telemetry.TelemetryMeasurement;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Exercises the project-open trace across its loading, composition, and presentation phases. */
final class EditorProjectOpenTraceTest {
    /** Completes the root only after first presentation and relates every phase to that root. */
    @Test
    void measuresCompleteProjectOpenPath() {
        List<TelemetryMeasurement> measurements = new ArrayList<>();
        EditorProjectOpenTrace trace =
                new EditorProjectOpenTrace(Telemetry.recording(measurements::add), Path.of("example-project"));

        String loaded = trace.load(operation -> "loaded");
        String composed = trace.compose(() -> "composed");
        EditorProjectOpenTiming timing = trace.present(() -> {});

        assertThat(loaded).isEqualTo("loaded");
        assertThat(composed).isEqualTo("composed");
        assertThat(timing.firstPresentation()).isPresent();
        assertThat(timing.total()).isGreaterThanOrEqualTo(Duration.ZERO);
        assertThat(measurements)
                .extracting(TelemetryMeasurement::name)
                .containsExactly(
                        "editor.project.load",
                        "editor.preview.compose",
                        "editor.preview.first-presentation",
                        "editor.project.open");
        TelemetryMeasurement root = measurements.getLast();
        assertThat(measurements.subList(0, 3))
                .allSatisfy(measurement ->
                        assertThat(measurement.parentOperationId()).hasValue(root.operationId()));
    }

    /** Completes failed loading paths without claiming a first presentation. */
    @Test
    void completesFailedProjectOpenPath() {
        List<TelemetryMeasurement> measurements = new ArrayList<>();
        EditorProjectOpenTrace trace =
                new EditorProjectOpenTrace(Telemetry.recording(measurements::add), Path.of("missing-project"));

        trace.load(operation -> "unavailable");
        EditorProjectOpenTiming timing = trace.fail("no editor session");

        assertThat(timing.firstPresentation()).isEmpty();
        assertThat(measurements.getLast().outcome()).isEqualTo(TelemetryMeasurement.Outcome.FAILED);
        assertThat(measurements.getLast().failureMessage()).contains("no editor session");
    }
}
