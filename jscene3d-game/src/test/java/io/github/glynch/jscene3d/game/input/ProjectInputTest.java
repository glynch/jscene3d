/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.game.input;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.glynch.jscene3d.project.input.InputActionDefinition;
import io.github.glynch.jscene3d.project.input.InputBinding;
import io.github.glynch.jscene3d.project.input.InputMapDefinition;
import io.github.glynch.jscene3d.project.input.InputValueType;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

final class ProjectInputTest {
    private static final InputAction FIRE = new InputAction("fire");

    @Test
    void publishesSemanticStateAndMakesClosureTerminal() {
        ProjectInput input = new ProjectInput(definition());
        ActionSnapshot pressed = ActionSnapshot.builder().pressed(FIRE).build();

        assertThat(input.snapshot()).isEqualTo(ActionSnapshot.empty());
        input.publish(pressed);
        assertThat(input.snapshot()).isSameAs(pressed);

        input.close();
        input.close();
        assertThatThrownBy(input::snapshot).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> input.publish(pressed)).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void providesAnEmptyModuleForProjectsWithoutAnInputMap() {
        try (ProjectInput input = ProjectInput.empty()) {
            assertThat(input.snapshot()).isEqualTo(ActionSnapshot.empty());
        }
    }

    @Test
    @SuppressWarnings("NullAway") // Deliberate nulls verify public boundary validation.
    void rejectsMissingConstructionAndPublicationState() {
        InputMapDefinition definition = definition();

        assertThatThrownBy(() -> new ProjectInput(null)).isInstanceOf(NullPointerException.class);
        try (ProjectInput input = new ProjectInput(definition)) {
            assertThatThrownBy(() -> input.publish(null)).isInstanceOf(NullPointerException.class);
        }
    }

    /** Creates one minimal validated authored definition. */
    private static InputMapDefinition definition() {
        return new InputMapDefinition(
                Path.of("/project/input-map.json"),
                Map.of(
                        "fire",
                        new InputActionDefinition(
                                InputValueType.BUTTON, List.of(new InputBinding.KeyboardKey("SPACE")))));
    }
}
