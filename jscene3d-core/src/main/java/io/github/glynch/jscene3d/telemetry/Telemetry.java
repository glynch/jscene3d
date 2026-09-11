/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.telemetry;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import java.util.function.LongSupplier;
import org.jspecify.annotations.Nullable;

/** Creates hierarchical local measurements and publishes completed values to an application-owned observer. */
public final class Telemetry {
    private static final System.Logger LOGGER = System.getLogger(Telemetry.class.getName());
    private static final Telemetry DISABLED = new Telemetry(null, System::nanoTime, Telemetry::logObserverRejection);

    private final @Nullable Consumer<TelemetryMeasurement> observer;
    private final LongSupplier ticker;
    private final Consumer<? super RuntimeException> rejectionHandler;
    private final AtomicLong traceIds = new AtomicLong();
    private final AtomicLong operationIds = new AtomicLong();

    /** Creates a recorder around an optional observer and monotonic ticker. */
    private Telemetry(
            @Nullable Consumer<TelemetryMeasurement> observer,
            LongSupplier ticker,
            Consumer<? super RuntimeException> rejectionHandler) {
        this.observer = observer;
        this.ticker = Objects.requireNonNull(ticker, "ticker");
        this.rejectionHandler = Objects.requireNonNull(rejectionHandler, "rejectionHandler");
    }

    /**
     * Returns a recorder which performs no measurement or publication work.
     *
     * @return the shared disabled recorder
     */
    public static Telemetry disabled() {
        return DISABLED;
    }

    /**
     * Creates a local recorder which publishes completed measurements to the supplied observer.
     *
     * @param observer application-owned destination for completed measurements
     * @return a local recorder using monotonic system time
     */
    public static Telemetry recording(Consumer<TelemetryMeasurement> observer) {
        return new Telemetry(
                Objects.requireNonNull(observer, "observer"), System::nanoTime, Telemetry::logObserverRejection);
    }

    /**
     * Creates a root operation with immutable contextual attributes.
     *
     * @param name stable semantic operation name
     * @param attributes low-cardinality context copied when the operation begins
     * @return a root operation which publishes when closed
     */
    public TelemetryOperation begin(String name, Map<String, String> attributes) {
        if (observer == null) {
            return TelemetryOperation.disabled();
        }
        long operationId = operationIds.incrementAndGet();
        return TelemetryOperation.start(
                this,
                traceIds.incrementAndGet(),
                operationId,
                0L,
                requireName(name),
                copyAttributes(attributes),
                ticker.getAsLong());
    }

    /** Creates a recorder with a controllable ticker for deterministic package tests. */
    static Telemetry recording(Consumer<TelemetryMeasurement> observer, LongSupplier ticker) {
        return recording(observer, ticker, Telemetry::logObserverRejection);
    }

    /** Creates a recorder whose observer rejection handling is visible to package tests. */
    static Telemetry recording(
            Consumer<TelemetryMeasurement> observer,
            LongSupplier ticker,
            Consumer<? super RuntimeException> rejectionHandler) {
        return new Telemetry(Objects.requireNonNull(observer, "observer"), ticker, rejectionHandler);
    }

    /** Creates one child operation of an enabled parent. */
    TelemetryOperation beginChild(TelemetryOperation parent, String name, Map<String, String> attributes) {
        return TelemetryOperation.start(
                this,
                parent.traceId(),
                operationIds.incrementAndGet(),
                parent.operationId(),
                requireName(name),
                copyAttributes(attributes),
                ticker.getAsLong());
    }

    /** Returns one monotonic timestamp for an operation owned by this recorder. */
    long now() {
        return ticker.getAsLong();
    }

    /** Publishes one completed measurement without allowing observer failures into engine control flow. */
    void publish(TelemetryMeasurement measurement) {
        Consumer<TelemetryMeasurement> currentObserver = observer;
        if (currentObserver == null) {
            return;
        }
        try {
            currentObserver.accept(measurement);
        } catch (RuntimeException exception) {
            rejectionHandler.accept(exception);
        }
    }

    /** Reports one rejected measurement through the process logger. */
    private static void logObserverRejection(RuntimeException exception) {
        LOGGER.log(System.Logger.Level.WARNING, "Telemetry observer rejected a measurement", exception);
    }

    /** Validates one stable operation name. */
    private static String requireName(String name) {
        Objects.requireNonNull(name, "name");
        if (name.isBlank()) {
            throw new IllegalArgumentException("name must not be blank");
        }
        return name;
    }

    /** Copies and validates contextual attributes at the instrumentation boundary. */
    private static Map<String, String> copyAttributes(Map<String, String> attributes) {
        Objects.requireNonNull(attributes, "attributes");
        attributes.forEach((key, value) -> {
            if (key.isBlank()) {
                throw new IllegalArgumentException("attribute key must not be blank");
            }
            Objects.requireNonNull(value, "attribute value");
        });
        return Map.copyOf(attributes);
    }
}
