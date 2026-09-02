/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.game.input;

import java.util.Objects;

/** Stable semantic name for one game-controlled action. */
public final class InputAction {
    private final String name;

    /**
     * Creates an action identifier.
     *
     * @param name non-blank semantic name
     * @throws NullPointerException if {@code name} is {@code null}
     * @throws IllegalArgumentException if {@code name} is blank
     */
    public InputAction(String name) {
        String validName = Objects.requireNonNull(name, "name");
        if (validName.isBlank()) {
            throw new IllegalArgumentException("name must not be blank");
        }
        this.name = validName;
    }

    /**
     * Returns the stable semantic name.
     *
     * @return non-blank name
     */
    public String name() {
        return name;
    }

    @Override
    public boolean equals(Object other) {
        return this == other || other instanceof InputAction action && name.equals(action.name);
    }

    @Override
    public int hashCode() {
        return name.hashCode();
    }

    @Override
    public String toString() {
        return name;
    }
}
