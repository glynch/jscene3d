/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.runtime;

import io.github.glynch.jscene3d.game.input.InputAction;
import io.github.glynch.jscene3d.game.input.InputMap;
import io.github.glynch.jscene3d.platform.Key;
import io.github.glynch.jscene3d.platform.MouseButton;
import io.github.glynch.jscene3d.project.input.InputBinding;
import io.github.glynch.jscene3d.project.input.InputMapDefinition;
import java.util.Objects;

/** Converts portable project input definitions into native semantic input maps. */
public final class ProjectInputMaps {
    /** Prevents construction of this stateless conversion namespace. */
    private ProjectInputMaps() {
        throw new AssertionError("ProjectInputMaps cannot be instantiated");
    }

    /**
     * Creates a native input map from one validated project definition.
     *
     * @param definition validated portable input-map definition
     * @return native semantic input map
     * @throws IllegalArgumentException if a named physical control is unsupported by this runtime
     */
    public static InputMap create(InputMapDefinition definition) {
        InputMapDefinition validDefinition = Objects.requireNonNull(definition, "definition");
        InputMap.Builder builder = InputMap.builder();
        validDefinition.actions().forEach((name, bindings) -> {
            InputAction action = new InputAction(name);
            bindings.forEach(binding -> add(builder, action, binding));
        });
        return builder.build();
    }

    /** Adds one portable binding after resolving its runtime control enum. */
    private static void add(InputMap.Builder builder, InputAction action, InputBinding binding) {
        try {
            if (binding.device() == InputBinding.Device.KEYBOARD) {
                builder.bind(action, Key.valueOf(binding.control()));
            } else {
                builder.bind(action, MouseButton.valueOf(binding.control()));
            }
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException(
                    "unsupported " + binding.device() + " control for action " + action.name() + ": "
                            + binding.control(),
                    exception);
        }
    }
}
