/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.examples;

import io.github.glynch.jscene3d.telemetry.Telemetry;
import io.github.glynch.jscene3d.telemetry.TelemetryMeasurement;
import io.github.glynch.jscene3d.telemetry.TelemetryOperation;
import java.util.Map;
import java.util.logging.Logger;

/** Demonstrates local hierarchical measurements without coupling engine code to a telemetry backend. */
public final class TelemetryExample {
    private static final Logger LOGGER = Logger.getLogger(TelemetryExample.class.getName());

    /** Prevents instantiation of this application entry point. */
    private TelemetryExample() {
        throw new AssertionError("TelemetryExample cannot be instantiated");
    }

    /**
     * Records one root operation and a nested preparation phase to the application log.
     *
     * @param arguments unused command-line arguments
     */
    public static void main(String[] arguments) {
        Telemetry telemetry = Telemetry.recording(TelemetryExample::log);
        try (TelemetryOperation opening = telemetry.begin("example.project.open", Map.of("project", "example"))) {
            String result = opening.measure("example.project.prepare", Map.of(), () -> "ready");
            LOGGER.info(() -> "Project is " + result);
        }
    }

    /** Logs one completed measurement as an example application-owned observer. */
    private static void log(TelemetryMeasurement measurement) {
        LOGGER.info(() -> measurement.name() + " completed in " + measurement.duration());
    }
}
