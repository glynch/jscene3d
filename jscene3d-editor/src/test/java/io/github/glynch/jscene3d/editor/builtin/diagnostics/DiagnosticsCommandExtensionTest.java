/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.builtin.diagnostics;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.glynch.jscene3d.editor.command.EditorCommands;
import io.github.glynch.jscene3d.editor.extension.project.EditorProjectContext;
import io.github.glynch.jscene3d.editor.workbench.extension.EditorExtensionHost;
import io.github.glynch.jscene3d.editor.workbench.selection.EditorSelectionContext;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.Test;

final class DiagnosticsCommandExtensionTest {
    @Test
    void opensDiagnosticsThroughTheRegisteredEditorCommand() {
        AtomicBoolean opened = new AtomicBoolean();
        EditorExtensionHost host = new EditorExtensionHost(new EditorProjectContext(), new EditorSelectionContext());
        host.activate(new DiagnosticsCommandExtension(() -> opened.set(true)));

        host.execute(EditorCommands.OPEN_DIAGNOSTICS);

        assertThat(opened).isTrue();
        host.close();
    }
}
