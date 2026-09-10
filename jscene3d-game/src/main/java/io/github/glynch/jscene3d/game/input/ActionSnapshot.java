/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.game.input;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/** Immutable semantic action and relative-pointer state for one update. */
public final class ActionSnapshot {
    private static final ActionSnapshot EMPTY =
            new ActionSnapshot(Set.of(), Set.of(), Set.of(), Map.of(), Map.of(), PointerInput.EMPTY);

    private final Set<InputAction> down;
    private final Set<InputAction> pressed;
    private final Set<InputAction> released;
    private final Map<InputAction, Float> axes1d;
    private final Map<InputAction, InputVector2> axes2d;
    private final PointerInput pointerInput;

    /** Stores immutable action sets and finite relative-pointer movement. */
    ActionSnapshot(
            Set<InputAction> down,
            Set<InputAction> pressed,
            Set<InputAction> released,
            Map<InputAction, Float> axes1d,
            Map<InputAction, InputVector2> axes2d,
            PointerInput pointerInput) {
        this.down = Set.copyOf(down);
        this.pressed = Set.copyOf(pressed);
        this.released = Set.copyOf(released);
        this.axes1d = Map.copyOf(axes1d);
        this.axes2d = Map.copyOf(axes2d);
        this.pointerInput = Objects.requireNonNull(pointerInput, "pointerInput");
    }

    /**
     * Returns an empty snapshot.
     *
     * @return shared snapshot with no actions or movement
     */
    public static ActionSnapshot empty() {
        return EMPTY;
    }

    /**
     * Returns a builder suitable for tests, replay input, and non-window adapters.
     *
     * @return new snapshot builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Returns whether an action is held.
     *
     * @param action action to query
     * @return whether the action is held
     */
    public boolean isDown(InputAction action) {
        return down.contains(Objects.requireNonNull(action, "action"));
    }

    /**
     * Returns whether an action became active during this update.
     *
     * @param action action to query
     * @return whether the action was pressed
     */
    public boolean wasPressed(InputAction action) {
        return pressed.contains(Objects.requireNonNull(action, "action"));
    }

    /**
     * Returns whether an action became inactive during this update.
     *
     * @param action action to query
     * @return whether the action was released
     */
    public boolean wasReleased(InputAction action) {
        return released.contains(Objects.requireNonNull(action, "action"));
    }

    /**
     * Produces a digital axis from two actions.
     *
     * @param negative action contributing minus one while held
     * @param positive action contributing plus one while held
     * @return minus one, zero, or plus one
     */
    public float axis(InputAction negative, InputAction positive) {
        float negativeValue = isDown(negative) ? 1.0F : 0.0F;
        float positiveValue = isDown(positive) ? 1.0F : 0.0F;
        return positiveValue - negativeValue;
    }

    /** Returns one authored one-dimensional action value.
     *
     * @param action action to query
     * @return current scalar value
     */
    public float axis1d(InputAction action) {
        return axes1d.getOrDefault(Objects.requireNonNull(action, "action"), 0.0F);
    }

    /** Returns one authored two-dimensional action value.
     *
     * @param action action to query
     * @return current vector value
     */
    public InputVector2 axis2d(InputAction action) {
        return axes2d.getOrDefault(Objects.requireNonNull(action, "action"), InputVector2.ZERO);
    }

    /**
     * Returns horizontal relative-pointer movement.
     *
     * @return finite horizontal movement
     */
    public double pointerDeltaX() {
        return pointerInput.deltaX();
    }

    /**
     * Returns vertical relative-pointer movement.
     *
     * @return finite vertical movement
     */
    public double pointerDeltaY() {
        return pointerInput.deltaY();
    }

    /** Returns absolute primary-pointer state when supplied by the current host.
     *
     * @return pointer state in logical viewport coordinates, when available
     */
    public Optional<PointerSnapshot> pointer() {
        return pointerInput.absolute();
    }

    /**
     * Accumulates transitions and pointer movement while adopting the newer held state.
     *
     * @param newer snapshot sampled later
     * @return merged snapshot
     */
    public ActionSnapshot merge(ActionSnapshot newer) {
        ActionSnapshot validNewer = Objects.requireNonNull(newer, "newer");
        Set<InputAction> mergedPressed = union(pressed, validNewer.pressed);
        Set<InputAction> mergedReleased = union(released, validNewer.released);
        PointerInput mergedPointer = pointerInput.merge(validNewer.pointerInput);
        return new ActionSnapshot(
                validNewer.down, mergedPressed, mergedReleased, validNewer.axes1d, validNewer.axes2d, mergedPointer);
    }

    /**
     * Preserves held actions while consuming transitions and relative movement.
     *
     * @return held-only snapshot
     */
    public ActionSnapshot heldOnly() {
        return down.isEmpty()
                        && axes1d.isEmpty()
                        && axes2d.isEmpty()
                        && pointerInput.absolute().isEmpty()
                ? EMPTY
                : new ActionSnapshot(down, Set.of(), Set.of(), axes1d, axes2d, pointerInput.heldOnly());
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof ActionSnapshot snapshot)) {
            return false;
        }
        return down.equals(snapshot.down)
                && pressed.equals(snapshot.pressed)
                && released.equals(snapshot.released)
                && axes1d.equals(snapshot.axes1d)
                && axes2d.equals(snapshot.axes2d)
                && pointerInput.equals(snapshot.pointerInput);
    }

    @Override
    public int hashCode() {
        return Objects.hash(down, pressed, released, axes1d, axes2d, pointerInput);
    }

    @Override
    public String toString() {
        return "ActionSnapshot{down=" + down + ", pressed=" + pressed + ", released=" + released + ", axes1d="
                + axes1d + ", axes2d=" + axes2d + ", pointerDeltaX=" + pointerInput.deltaX()
                + ", pointerDeltaY=" + pointerInput.deltaY() + ", pointer=" + pointerInput.absolute() + '}';
    }

    /** Returns this action state with host-supplied absolute pointer state. */
    ActionSnapshot withPointer(PointerSnapshot value) {
        return new ActionSnapshot(
                down,
                pressed,
                released,
                axes1d,
                axes2d,
                pointerInput.withAbsolute(Objects.requireNonNull(value, "value")));
    }

    /** Returns the set union without exposing mutable storage. */
    private static Set<InputAction> union(Set<InputAction> first, Set<InputAction> second) {
        Set<InputAction> result = new HashSet<>(first);
        result.addAll(second);
        return result;
    }

    /** Rejects non-finite relative movement. */
    private static double requireFinite(double value, String name) {
        if (!Double.isFinite(value)) {
            throw new IllegalArgumentException(name + " must be finite: " + value);
        }
        return value;
    }

    /** Builds an immutable semantic snapshot without requiring a native input source. */
    public static final class Builder {
        private final Set<InputAction> down = new HashSet<>();
        private final Set<InputAction> pressed = new HashSet<>();
        private final Set<InputAction> released = new HashSet<>();
        private final Map<InputAction, Float> axes1d = new HashMap<>();
        private final Map<InputAction, InputVector2> axes2d = new HashMap<>();
        private double pointerDeltaX;
        private double pointerDeltaY;
        private Optional<PointerSnapshot> pointer = Optional.empty();

        /** Creates an empty builder. */
        private Builder() {}

        /**
         * Marks an action as held.
         *
         * @param action held action
         * @return this builder
         */
        public Builder down(InputAction action) {
            down.add(Objects.requireNonNull(action, "action"));
            return this;
        }

        /**
         * Marks an action as newly pressed and held.
         *
         * @param action pressed action
         * @return this builder
         */
        public Builder pressed(InputAction action) {
            InputAction validAction = Objects.requireNonNull(action, "action");
            pressed.add(validAction);
            down.add(validAction);
            return this;
        }

        /**
         * Marks an action as newly released and no longer held.
         *
         * @param action released action
         * @return this builder
         */
        public Builder released(InputAction action) {
            InputAction validAction = Objects.requireNonNull(action, "action");
            released.add(validAction);
            down.remove(validAction);
            return this;
        }

        /** Sets one authored one-dimensional action value.
         *
         * @param action action to set
         * @param value finite scalar value, clamped to the unit range
         * @return this builder
         */
        public Builder axis1d(InputAction action, float value) {
            InputAction validAction = Objects.requireNonNull(action, "action");
            float bounded = clamp(value);
            if (bounded == 0.0F) {
                axes1d.remove(validAction);
            } else {
                axes1d.put(validAction, bounded);
            }
            return this;
        }

        /** Sets one authored two-dimensional action value.
         *
         * @param action action to set
         * @param x finite horizontal value, clamped to the unit range
         * @param y finite vertical value, clamped to the unit range
         * @return this builder
         */
        public Builder axis2d(InputAction action, float x, float y) {
            InputAction validAction = Objects.requireNonNull(action, "action");
            InputVector2 value = new InputVector2(clamp(x), clamp(y));
            if (value.equals(InputVector2.ZERO)) {
                axes2d.remove(validAction);
            } else {
                axes2d.put(validAction, value);
            }
            return this;
        }

        /**
         * Sets relative-pointer movement.
         *
         * @param x finite horizontal movement
         * @param y finite vertical movement
         * @return this builder
         */
        public Builder pointerDelta(double x, double y) {
            pointerDeltaX = requireFinite(x, "x");
            pointerDeltaY = requireFinite(y, "y");
            return this;
        }

        /** Sets absolute primary-pointer state supplied by a host or replay.
         *
         * @param value immutable logical pointer sample
         * @return this builder
         */
        public Builder pointer(PointerSnapshot value) {
            pointer = Optional.of(Objects.requireNonNull(value, "value"));
            return this;
        }

        /**
         * Builds the immutable snapshot.
         *
         * @return immutable action snapshot
         */
        public ActionSnapshot build() {
            return new ActionSnapshot(
                    down, pressed, released, axes1d, axes2d, new PointerInput(pointerDeltaX, pointerDeltaY, pointer));
        }

        /** Clamps a finite aggregate to the authored axis range. */
        private static float clamp(float value) {
            if (!Float.isFinite(value)) {
                throw new IllegalArgumentException("axis value must be finite: " + value);
            }
            return Math.clamp(value, -1.0F, 1.0F);
        }
    }

    /** Relative and optional absolute pointer state grouped behind one immutable value. */
    private record PointerInput(double deltaX, double deltaY, Optional<PointerSnapshot> absolute) {
        private static final PointerInput EMPTY = new PointerInput(0.0, 0.0, Optional.empty());

        /** Validates finite movement and a non-null optional absolute sample. */
        private PointerInput {
            requireFinite(deltaX, "pointerDeltaX");
            requireFinite(deltaY, "pointerDeltaY");
            Objects.requireNonNull(absolute, "absolute");
        }

        /** Accumulates transitions and movement while adopting the latest available position. */
        private PointerInput merge(PointerInput newer) {
            Optional<PointerSnapshot> mergedAbsolute;
            if (newer.absolute.isEmpty()) {
                mergedAbsolute = absolute;
            } else {
                PointerSnapshot newest = newer.absolute.orElseThrow();
                mergedAbsolute = Optional.of(
                        absolute.map(previous -> previous.merge(newest)).orElse(newest));
            }
            return new PointerInput(deltaX + newer.deltaX, deltaY + newer.deltaY, mergedAbsolute);
        }

        /** Preserves position and held state while consuming transient movement and transitions. */
        private PointerInput heldOnly() {
            return new PointerInput(0.0, 0.0, absolute.map(PointerSnapshot::heldOnly));
        }

        /** Replaces only the absolute host pointer sample. */
        private PointerInput withAbsolute(PointerSnapshot value) {
            return new PointerInput(deltaX, deltaY, Optional.of(value));
        }
    }
}
