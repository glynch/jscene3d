/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.doom.runtime.internal;

import io.github.glynch.jscene3d.doom.runtime.DoomDoor;
import io.github.glynch.jscene3d.doom.runtime.DoomDoorDescriptors;
import io.github.glynch.jscene3d.project.runtime.FixedUpdateContext;
import io.github.glynch.jscene3d.project.runtime.extension.ComponentReferenceBinder;
import io.github.glynch.jscene3d.project.runtime.extension.ComponentReferenceResolver;
import io.github.glynch.jscene3d.project.runtime.extension.ComponentUpdateCallbacks;
import io.github.glynch.jscene3d.project.spatial3d.Transform3d;
import java.time.Duration;
import java.util.Objects;
import java.util.Optional;

/** Moves one explicitly targeted transform according to descriptor-authored Doom door behavior. */
final class DoomDoorComponent implements DoomDoor, ComponentReferenceBinder, ComponentUpdateCallbacks {
    private final float closedHeight;
    private final float openHeight;
    private final float speed;
    private final Duration holdOpen;
    private Optional<Transform3d> transform = Optional.empty();
    private Phase phase = Phase.CLOSED;
    private float currentHeight;
    private Duration remainingHold = Duration.ZERO;

    /** Retains validated authored door motion independently of its generated geometry. */
    DoomDoorComponent(float closedHeight, float openHeight, float speed, float holdOpenSeconds) {
        this.closedHeight = requireFinite(closedHeight, "closedHeight");
        this.openHeight = requireFinite(openHeight, "openHeight");
        this.speed = requirePositive(speed, "speed");
        if (openHeight < closedHeight) {
            throw new IllegalArgumentException("openHeight must not be below closedHeight");
        }
        float validHold = requireFinite(holdOpenSeconds, "holdOpenSeconds");
        if (validHold < 0.0F) {
            throw new IllegalArgumentException("holdOpenSeconds must not be negative");
        }
        holdOpen = Duration.ofNanos(Math.round(validHold * 1_000_000_000.0));
        currentHeight = closedHeight;
    }

    @Override
    public void bindReferences(ComponentReferenceResolver references) {
        transform = Optional.of(Objects.requireNonNull(references, "references")
                .component(DoomDoorDescriptors.TRANSFORM_PROPERTY, Transform3d.class));
        setHeight(closedHeight);
    }

    @Override
    public boolean activate() {
        if (phase == Phase.CLOSED || phase == Phase.CLOSING) {
            phase = Phase.OPENING;
            return true;
        }
        return false;
    }

    @Override
    public Phase phase() {
        return phase;
    }

    @Override
    public float currentHeight() {
        return currentHeight;
    }

    @Override
    public void onBeforePhysics(FixedUpdateContext update) {
        Duration step = Objects.requireNonNull(update, "update").step();
        switch (phase) {
            case OPENING -> openFurther(step);
            case WAITING -> waitBeforeClosing(step);
            case CLOSING -> close(step);
            case CLOSED, OPENED -> {
                // Stable phases have no work.
            }
        }
    }

    /** Advances toward the open position and selects the authored terminal behavior. */
    private void openFurther(Duration step) {
        setHeight(Math.min(openHeight, currentHeight + distance(step)));
        if (currentHeight != openHeight) {
            return;
        }
        if (holdOpen.isZero()) {
            phase = Phase.OPENED;
        } else {
            remainingHold = holdOpen;
            phase = Phase.WAITING;
        }
    }

    /** Counts down the authored open hold without coupling it to a particular fixed-step rate. */
    private void waitBeforeClosing(Duration step) {
        remainingHold = remainingHold.minus(step);
        if (remainingHold.isZero() || remainingHold.isNegative()) {
            remainingHold = Duration.ZERO;
            phase = Phase.CLOSING;
        }
    }

    /** Advances toward the original closed position. */
    private void close(Duration step) {
        setHeight(Math.max(closedHeight, currentHeight - distance(step)));
        if (currentHeight == closedHeight) {
            phase = Phase.CLOSED;
        }
    }

    /** Converts one fixed-step duration into a vertical distance. */
    private float distance(Duration step) {
        return speed * (float) (step.toNanos() / 1_000_000_000.0);
    }

    /** Updates the targeted transform while preserving its authored X and Z position. */
    private void setHeight(float height) {
        currentHeight = height;
        transform.ifPresent(value ->
                value.setPosition(value.position().x(), height, value.position().z()));
    }

    private static float requireFinite(float value, String name) {
        if (!Float.isFinite(value)) {
            throw new IllegalArgumentException(name + " must be finite");
        }
        return value;
    }

    private static float requirePositive(float value, String name) {
        float validValue = requireFinite(value, name);
        if (validValue <= 0.0F) {
            throw new IllegalArgumentException(name + " must be positive");
        }
        return validValue;
    }
}
