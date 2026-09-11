/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.builtin.diagnostics;

import io.github.glynch.jscene3d.editor.command.EditorCommandContribution;
import io.github.glynch.jscene3d.editor.command.EditorCommands;
import io.github.glynch.jscene3d.editor.extension.EditorExtension;
import io.github.glynch.jscene3d.editor.extension.EditorExtensionContext;
import java.util.Objects;

/** Built-in extension providing the command which reveals project diagnostics. */
public final class DiagnosticsCommandExtension implements EditorExtension {
    private final Runnable openDiagnostics;

    /** Creates the extension around the workbench's Diagnostics reveal action. */
    public DiagnosticsCommandExtension(Runnable openDiagnostics) {
        this.openDiagnostics = Objects.requireNonNull(openDiagnostics, "openDiagnostics");
    }

    @Override
    public String id() {
        return "io.github.glynch.jscene3d.editor.builtin.diagnostics";
    }

    @Override
    public void activate(EditorExtensionContext context) {
        EditorExtensionContext editor = Objects.requireNonNull(context, "context");
        editor.subscriptions()
                .add(editor.commands()
                        .register(
                                new EditorCommandContribution(EditorCommands.OPEN_DIAGNOSTICS, "Open Diagnostics"),
                                ignored -> openDiagnostics.run()));
    }
}
