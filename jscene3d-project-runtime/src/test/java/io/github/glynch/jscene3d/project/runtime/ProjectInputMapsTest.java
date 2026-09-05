/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.runtime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.glynch.jscene3d.game.input.InputMap;
import io.github.glynch.jscene3d.project.input.InputBinding;
import io.github.glynch.jscene3d.project.input.InputMapDefinition;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ProjectInputMapsTest {
    @TempDir
    private Path temporaryDirectory;

    @Test
    void convertsSupportedPortableBindings() {
        InputMapDefinition definition = definition(
                new InputBinding(InputBinding.Device.KEYBOARD, "W"),
                new InputBinding(InputBinding.Device.MOUSE_BUTTON, "LEFT"));

        InputMap inputMap = ProjectInputMaps.create(definition);

        assertThat(inputMap).isNotNull();
    }

    @Test
    void rejectsUnknownRuntimeControlNames() {
        InputMapDefinition definition = definition(new InputBinding(InputBinding.Device.KEYBOARD, "NOT_A_KEY"));

        assertThatThrownBy(() -> ProjectInputMaps.create(definition))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("unsupported KEYBOARD control")
                .hasMessageContaining("NOT_A_KEY");
    }

    /** Creates one portable definition at a normalized absolute test path. */
    private InputMapDefinition definition(InputBinding... bindings) {
        return new InputMapDefinition(
                temporaryDirectory.resolve("input-map.json"), Map.of("action", List.of(bindings)));
    }
}
