/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.doom.runtime.internal;

import io.github.glynch.jscene3d.doom.runtime.DoomFloor;
import io.github.glynch.jscene3d.doom.runtime.DoomFloorDescriptors;
import io.github.glynch.jscene3d.project.physics3d.CollisionOverlap3d;
import io.github.glynch.jscene3d.project.runtime.FixedUpdateContext;
import io.github.glynch.jscene3d.project.runtime.RuntimePayload;
import io.github.glynch.jscene3d.project.runtime.extension.ComponentEndpointBinder;
import io.github.glynch.jscene3d.project.runtime.extension.ComponentEndpoints;
import io.github.glynch.jscene3d.project.runtime.extension.ComponentReferenceBinder;
import io.github.glynch.jscene3d.project.runtime.extension.ComponentReferenceResolver;
import io.github.glynch.jscene3d.project.runtime.extension.ComponentUpdateCallbacks;
import io.github.glynch.jscene3d.project.spatial3d.Transform3d;
import java.time.Duration;
import java.util.Objects;
import java.util.Optional;

/** Moves one explicitly targeted floor transform after its generated player-only trigger is entered. */
final class DoomFloorComponent
        implements DoomFloor, ComponentReferenceBinder, ComponentEndpointBinder, ComponentUpdateCallbacks {
    private final Profile profile;
    private final float raisedHeight;
    private final float loweredHeight;
    private final float speed;
    private Optional<Transform3d> transform = Optional.empty();
    private Phase phase = Phase.RAISED;
    private float currentHeight;

    /** Retains validated authored motion independently of generated presentation and collision. */
    DoomFloorComponent(Profile profile, float raisedHeight, float loweredHeight, float speed) {
        this.profile = Objects.requireNonNull(profile, "profile");
        this.raisedHeight = requireFinite(raisedHeight, "raisedHeight");
        this.loweredHeight = requireFinite(loweredHeight, "loweredHeight");
        this.speed = requirePositive(speed, "speed");
        if (loweredHeight >= raisedHeight) {
            throw new IllegalArgumentException("loweredHeight must be below raisedHeight");
        }
        currentHeight = raisedHeight;
    }

    @Override
    public void bindEndpoints(ComponentEndpoints endpoints) {
        Objects.requireNonNull(endpoints, "endpoints")
                .action(DoomFloorDescriptors.TRIGGER_ENTERED_ACTION, this::triggerEntered);
    }

    @Override
    public void bindReferences(ComponentReferenceResolver references) {
        transform = Optional.of(Objects.requireNonNull(references, "references")
                .component(DoomFloorDescriptors.TRANSFORM_PROPERTY, Transform3d.class));
        setHeight(raisedHeight);
    }

    @Override
    public boolean activate() {
        if (phase != Phase.RAISED) {
            return false;
        }
        phase = Phase.LOWERING;
        return true;
    }

    @Override
    public Profile profile() {
        return profile;
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
        if (phase != Phase.LOWERING) {
            return;
        }
        setHeight(Math.max(loweredHeight, currentHeight - distance(step)));
        if (currentHeight == loweredHeight) {
            phase = Phase.LOWERED;
        }
    }

    /** Validates the generated physics payload before beginning the one-shot motion. */
    private void triggerEntered(RuntimePayload payload) {
        Object value = Objects.requireNonNull(payload, "payload").value();
        if (!(value instanceof CollisionOverlap3d)) {
            throw new IllegalArgumentException("trigger-entered requires a CollisionOverlap3d payload");
        }
        activate();
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
