/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.game.input;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/** Immutable semantic action and relative-pointer state for one update. */
public final class ActionSnapshot {
    private static final ActionSnapshot EMPTY = new ActionSnapshot(Set.of(), Set.of(), Set.of(), 0.0, 0.0);

    private final Set<InputAction> down;
    private final Set<InputAction> pressed;
    private final Set<InputAction> released;
    private final double pointerDeltaX;
    private final double pointerDeltaY;

    /** Stores immutable action sets and finite relative-pointer movement. */
    ActionSnapshot(
            Set<InputAction> down,
            Set<InputAction> pressed,
            Set<InputAction> released,
            double pointerDeltaX,
            double pointerDeltaY) {
        this.down = Set.copyOf(down);
        this.pressed = Set.copyOf(pressed);
        this.released = Set.copyOf(released);
        this.pointerDeltaX = requireFinite(pointerDeltaX, "pointerDeltaX");
        this.pointerDeltaY = requireFinite(pointerDeltaY, "pointerDeltaY");
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

    /**
     * Returns horizontal relative-pointer movement.
     *
     * @return finite horizontal movement
     */
    public double pointerDeltaX() {
        return pointerDeltaX;
    }

    /**
     * Returns vertical relative-pointer movement.
     *
     * @return finite vertical movement
     */
    public double pointerDeltaY() {
        return pointerDeltaY;
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
        return new ActionSnapshot(
                validNewer.down,
                mergedPressed,
                mergedReleased,
                pointerDeltaX + validNewer.pointerDeltaX,
                pointerDeltaY + validNewer.pointerDeltaY);
    }

    /**
     * Preserves held actions while consuming transitions and relative movement.
     *
     * @return held-only snapshot
     */
    public ActionSnapshot heldOnly() {
        return down.isEmpty() ? EMPTY : new ActionSnapshot(down, Set.of(), Set.of(), 0.0, 0.0);
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
                && Double.compare(pointerDeltaX, snapshot.pointerDeltaX) == 0
                && Double.compare(pointerDeltaY, snapshot.pointerDeltaY) == 0;
    }

    @Override
    public int hashCode() {
        return Objects.hash(down, pressed, released, pointerDeltaX, pointerDeltaY);
    }

    @Override
    public String toString() {
        return "ActionSnapshot{down=" + down + ", pressed=" + pressed + ", released=" + released + ", pointerDeltaX="
                + pointerDeltaX + ", pointerDeltaY=" + pointerDeltaY + '}';
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
        private double pointerDeltaX;
        private double pointerDeltaY;

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
         * Marks an action as newly released.
         *
         * @param action released action
         * @return this builder
         */
        public Builder released(InputAction action) {
            released.add(Objects.requireNonNull(action, "action"));
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

        /**
         * Builds the immutable snapshot.
         *
         * @return immutable action snapshot
         */
        public ActionSnapshot build() {
            return new ActionSnapshot(down, pressed, released, pointerDeltaX, pointerDeltaY);
        }
    }
}
