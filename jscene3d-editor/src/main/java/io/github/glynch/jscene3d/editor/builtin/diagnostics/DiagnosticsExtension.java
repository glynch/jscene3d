/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.builtin.diagnostics;

import io.github.glynch.jscene3d.editor.command.EditorCommandContribution;
import io.github.glynch.jscene3d.editor.command.EditorCommands;
import io.github.glynch.jscene3d.editor.extension.EditorExtension;
import io.github.glynch.jscene3d.editor.extension.EditorExtensionContext;
import io.github.glynch.jscene3d.editor.status.EditorStatusItem;
import io.github.glynch.jscene3d.editor.status.EditorStatusItemContribution;
import io.github.glynch.jscene3d.editor.status.EditorStatusItemState;
import io.github.glynch.jscene3d.editor.status.StatusBarAlignment;
import io.github.glynch.jscene3d.editor.status.StatusItemId;
import io.github.glynch.jscene3d.editor.view.EditorIcon;
import io.github.glynch.jscene3d.editor.view.EditorIconId;
import io.github.glynch.jscene3d.editor.view.EditorIcons;
import io.github.glynch.jscene3d.editor.view.EditorViewContainers;
import io.github.glynch.jscene3d.editor.view.EditorViewContribution;
import io.github.glynch.jscene3d.editor.workbench.extension.EditorDiagnosticSnapshot;
import io.github.glynch.jscene3d.editor.workbench.extension.EditorExtensionHost;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/** Built-in extension contributing Diagnostics, its reveal command, and status counts. */
public final class DiagnosticsExtension implements EditorExtension {
    private static final StatusItemId ERROR_STATUS =
            new StatusItemId("io.github.glynch.jscene3d.editor.diagnostics.errors");
    private static final StatusItemId WARNING_STATUS =
            new StatusItemId("io.github.glynch.jscene3d.editor.diagnostics.warnings");

    private final EditorExtensionHost host;
    private final EditorDiagnosticsModel model = new EditorDiagnosticsModel();

    /**
     * Creates the extension over the host's aggregated diagnostic stream.
     *
     * @param host editor extension host
     */
    public DiagnosticsExtension(EditorExtensionHost host) {
        this.host = Objects.requireNonNull(host, "host");
    }

    @Override
    public String id() {
        return "io.github.glynch.jscene3d.editor.builtin.diagnostics";
    }

    @Override
    public void activate(EditorExtensionContext context) {
        EditorExtensionContext editor = Objects.requireNonNull(context, "context");
        editor.subscriptions()
                .add(editor.views()
                        .register(new EditorViewContribution(
                                new DiagnosticsView(model), EditorViewContainers.BOTTOM_PANEL, 20)));
        editor.subscriptions()
                .add(editor.commands()
                        .register(
                                new EditorCommandContribution(EditorCommands.OPEN_DIAGNOSTICS, "Open Diagnostics"),
                                invocation -> invocation.window().showView(DiagnosticsView.VIEW_ID)));
        EditorStatusItem errors = editor.subscriptions()
                .add(editor.statusBar()
                        .create(new EditorStatusItemContribution(ERROR_STATUS, StatusBarAlignment.RIGHT, 100)));
        EditorStatusItem warnings = editor.subscriptions()
                .add(editor.statusBar()
                        .create(new EditorStatusItemContribution(WARNING_STATUS, StatusBarAlignment.RIGHT, 90)));
        editor.subscriptions().add(host.observeDiagnostics(snapshot -> update(snapshot, errors, warnings)));
    }

    private void update(List<EditorDiagnosticSnapshot> snapshot, EditorStatusItem errors, EditorStatusItem warnings) {
        model.showDiagnostics(snapshot);
        EditorDiagnosticsModel.View view = model.view();
        errors.update(status(view.errors(), "errors", EditorIcons.ERROR));
        warnings.update(status(view.warnings(), "warnings", EditorIcons.WARNING));
    }

    private static EditorStatusItemState status(long count, String label, EditorIconId icon) {
        String text = Long.toString(count);
        String tooltip = count + " " + label + "; open Diagnostics";
        return new EditorStatusItemState(
                text,
                Optional.of(new EditorIcon(icon, label)),
                Optional.of(tooltip),
                Optional.of(EditorCommands.OPEN_DIAGNOSTICS),
                true);
    }
}
