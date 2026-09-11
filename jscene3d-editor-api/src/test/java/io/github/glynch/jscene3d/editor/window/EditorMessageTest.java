/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.window;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.glynch.jscene3d.editor.command.EditorCommands;
import org.junit.jupiter.api.Test;

final class EditorMessageTest {
    @Test
    void createsOrdinaryAndCommandBackedMessages() {
        EditorMessage ordinary = new EditorMessage(EditorMessageSeverity.INFORMATION, "Ready");
        EditorMessage actionable =
                new EditorMessage(EditorMessageSeverity.ERROR, "See Diagnostics", EditorCommands.OPEN_DIAGNOSTICS);

        assertThat(ordinary.command()).isEmpty();
        assertThat(actionable.command()).contains(EditorCommands.OPEN_DIAGNOSTICS);
    }
}
