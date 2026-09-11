/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.status;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.glynch.jscene3d.editor.command.CommandId;
import java.util.Optional;
import org.junit.jupiter.api.Test;

final class EditorStatusBarContractTest {
    @Test
    void describesAtomicStatusPresentation() {
        StatusItemId id = new StatusItemId("io.github.glynch.jscene3d.editor.diagnostics-status");
        CommandId command = new CommandId("io.github.glynch.jscene3d.editor.diagnostics-show");
        EditorStatusItemContribution contribution = new EditorStatusItemContribution(id, StatusBarAlignment.RIGHT, 100);
        EditorStatusItemState state =
                new EditorStatusItemState("1 error", Optional.of("Open Diagnostics"), Optional.of(command), true);

        assertThat(id).hasToString(id.value());
        assertThat(contribution.alignment()).isEqualTo(StatusBarAlignment.RIGHT);
        assertThat(contribution.priority()).isEqualTo(100);
        assertThat(state.text()).isEqualTo("1 error");
        assertThat(state.tooltip()).contains("Open Diagnostics");
        assertThat(state.command()).contains(command);
        assertThat(state.visible()).isTrue();
    }

    @Test
    @SuppressWarnings("NullAway") // Deliberate nulls verify public boundary validation.
    void validatesStatusContributionsAndState() {
        StatusItemId id = new StatusItemId("io.github.glynch.test.status");
        Optional<String> emptyTooltip = Optional.empty();
        Optional<CommandId> emptyCommand = Optional.empty();

        assertThatNullPointerException()
                .isThrownBy(() -> new EditorStatusItemContribution(null, StatusBarAlignment.LEFT, 0))
                .withMessage("id");
        assertThatNullPointerException()
                .isThrownBy(() -> new EditorStatusItemContribution(id, null, 0))
                .withMessage("alignment");
        assertThatThrownBy(() -> new EditorStatusItemState(" ", emptyTooltip, emptyCommand, false))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("text must not be blank");
        assertThatNullPointerException()
                .isThrownBy(() -> new EditorStatusItemState("Ready", null, emptyCommand, true))
                .withMessage("tooltip");
        assertThatNullPointerException()
                .isThrownBy(() -> new EditorStatusItemState("Ready", emptyTooltip, null, true))
                .withMessage("command");
    }

    @Test
    @SuppressWarnings("NullAway") // Deliberate null verifies public boundary validation.
    void rejectsMalformedStatusIdentity() {
        assertThatNullPointerException()
                .isThrownBy(() -> new StatusItemId(null))
                .withMessage("value");
        assertThatThrownBy(() -> new StatusItemId("local"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("value must be a lowercase dotted namespaced identity");
    }
}
