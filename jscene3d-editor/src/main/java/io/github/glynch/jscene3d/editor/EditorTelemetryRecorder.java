/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor;

import io.github.glynch.jscene3d.telemetry.TelemetryMeasurement;
import java.util.Locale;
import java.util.function.Consumer;

/** Writes editor-owned telemetry measurements to the local process log. */
final class EditorTelemetryRecorder implements Consumer<TelemetryMeasurement> {
    private static final System.Logger LOGGER = System.getLogger(EditorTelemetryRecorder.class.getName());

    /** Records one completed operation without logging its potentially sensitive attributes. */
    @Override
    public void accept(TelemetryMeasurement measurement) {
        System.Logger.Level level =
                measurement.parentOperationId().isEmpty() ? System.Logger.Level.INFO : System.Logger.Level.DEBUG;
        LOGGER.log(
                level,
                "{0} completed in {1} with outcome {2}",
                measurement.name(),
                formatMilliseconds(measurement),
                measurement.outcome());
    }

    /** Formats a measurement duration with useful sub-millisecond precision. */
    private static String formatMilliseconds(TelemetryMeasurement measurement) {
        double milliseconds = measurement.duration().toNanos() / 1_000_000.0;
        return String.format(Locale.ROOT, "%.1f ms", milliseconds);
    }
}
