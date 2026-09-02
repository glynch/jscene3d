/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.game.input;

import io.github.glynch.jscene3d.platform.InputState;
import io.github.glynch.jscene3d.platform.Key;
import io.github.glynch.jscene3d.platform.MouseButton;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** Immutable mapping from keyboard and mouse controls to semantic game actions. */
public final class InputMap {
    private final Map<InputAction, List<Binding>> bindings;

    /** Copies completed builder state. */
    private InputMap(Map<InputAction, List<Binding>> bindings) {
        Map<InputAction, List<Binding>> copied = new LinkedHashMap<>();
        bindings.forEach((action, actionBindings) -> copied.put(action, List.copyOf(actionBindings)));
        this.bindings = Map.copyOf(copied);
    }

    /**
     * Returns a new mapping builder.
     *
     * @return empty builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Samples a native window input view into semantic action state.
     *
     * @param input current window input
     * @param capture host-interface ownership for this frame
     * @return immutable semantic snapshot
     */
    public ActionSnapshot sample(InputState input, InputCapture capture) {
        InputState validInput = Objects.requireNonNull(input, "input");
        return sample(new WindowInput(validInput), capture);
    }

    /** Resolves semantic state through the internal deterministic input seam. */
    ActionSnapshot sample(PhysicalInput input, InputCapture capture) {
        PhysicalInput validInput = Objects.requireNonNull(input, "input");
        InputCapture validCapture = Objects.requireNonNull(capture, "capture");
        ActionSnapshot.Builder snapshot = ActionSnapshot.builder();
        bindings.forEach(
                (action, actionBindings) -> sampleAction(action, actionBindings, validInput, validCapture, snapshot));
        if (!validCapture.pointer()) {
            snapshot.pointerDelta(validInput.pointerDeltaX(), validInput.pointerDeltaY());
        }
        return snapshot.build();
    }

    /** Aggregates every physical binding assigned to one semantic action. */
    private static void sampleAction(
            InputAction action,
            List<Binding> bindings,
            PhysicalInput input,
            InputCapture capture,
            ActionSnapshot.Builder snapshot) {
        boolean down = bindings.stream().anyMatch(binding -> binding.isDown(input, capture));
        boolean pressed = down && bindings.stream().anyMatch(binding -> binding.wasPressed(input, capture));
        boolean released = !down && bindings.stream().anyMatch(binding -> binding.wasReleased(input, capture));
        if (pressed) {
            snapshot.pressed(action);
        } else if (down) {
            snapshot.down(action);
        }
        if (released) {
            snapshot.released(action);
        }
    }

    /** One keyboard or mouse binding hidden behind a common sampling contract. */
    private sealed interface Binding permits KeyBinding, MouseBinding {
        boolean isDown(PhysicalInput input, InputCapture capture);

        boolean wasPressed(PhysicalInput input, InputCapture capture);

        boolean wasReleased(PhysicalInput input, InputCapture capture);
    }

    /** Keyboard implementation of one physical binding. */
    private record KeyBinding(Key key) implements Binding {
        @Override
        public boolean isDown(PhysicalInput input, InputCapture capture) {
            return !capture.keyboard() && input.isKeyDown(key);
        }

        @Override
        public boolean wasPressed(PhysicalInput input, InputCapture capture) {
            return !capture.keyboard() && input.wasKeyPressed(key);
        }

        @Override
        public boolean wasReleased(PhysicalInput input, InputCapture capture) {
            return !capture.keyboard() && input.wasKeyReleased(key);
        }
    }

    /** Mouse implementation of one physical binding. */
    private record MouseBinding(MouseButton button) implements Binding {
        @Override
        public boolean isDown(PhysicalInput input, InputCapture capture) {
            return !capture.pointer() && input.isMouseButtonDown(button);
        }

        @Override
        public boolean wasPressed(PhysicalInput input, InputCapture capture) {
            return !capture.pointer() && input.wasMouseButtonPressed(button);
        }

        @Override
        public boolean wasReleased(PhysicalInput input, InputCapture capture) {
            return !capture.pointer() && input.wasMouseButtonReleased(button);
        }
    }

    /** Native-window adapter for the internal physical-input seam. */
    private record WindowInput(InputState input) implements PhysicalInput {
        @Override
        public boolean isKeyDown(Key key) {
            return input.isKeyDown(key);
        }

        @Override
        public boolean wasKeyPressed(Key key) {
            return input.wasKeyPressed(key);
        }

        @Override
        public boolean wasKeyReleased(Key key) {
            return input.wasKeyReleased(key);
        }

        @Override
        public boolean isMouseButtonDown(MouseButton button) {
            return input.isMouseButtonDown(button);
        }

        @Override
        public boolean wasMouseButtonPressed(MouseButton button) {
            return input.wasMouseButtonPressed(button);
        }

        @Override
        public boolean wasMouseButtonReleased(MouseButton button) {
            return input.wasMouseButtonReleased(button);
        }

        @Override
        public double pointerDeltaX() {
            return input.pointerDeltaX();
        }

        @Override
        public double pointerDeltaY() {
            return input.pointerDeltaY();
        }
    }

    /** Builds an immutable action map while preserving declaration order. */
    public static final class Builder {
        private final Map<InputAction, List<Binding>> bindings = new LinkedHashMap<>();

        /** Creates an empty builder. */
        private Builder() {}

        /**
         * Binds a keyboard key to an action.
         *
         * @param action semantic action
         * @param key physical key
         * @return this builder
         */
        public Builder bind(InputAction action, Key key) {
            return add(action, new KeyBinding(Objects.requireNonNull(key, "key")));
        }

        /**
         * Binds a mouse button to an action.
         *
         * @param action semantic action
         * @param button physical mouse button
         * @return this builder
         */
        public Builder bind(InputAction action, MouseButton button) {
            return add(action, new MouseBinding(Objects.requireNonNull(button, "button")));
        }

        /**
         * Builds the immutable mapping.
         *
         * @return mapping containing at least one action binding
         * @throws IllegalStateException if no bindings were added
         */
        public InputMap build() {
            if (bindings.isEmpty()) {
                throw new IllegalStateException("An input map requires at least one binding");
            }
            return new InputMap(bindings);
        }

        /** Adds one unique physical binding to an action. */
        private Builder add(InputAction action, Binding binding) {
            InputAction validAction = Objects.requireNonNull(action, "action");
            List<Binding> actionBindings = bindings.computeIfAbsent(validAction, ignored -> new ArrayList<>());
            if (!actionBindings.contains(binding)) {
                actionBindings.add(binding);
            }
            return this;
        }
    }
}
