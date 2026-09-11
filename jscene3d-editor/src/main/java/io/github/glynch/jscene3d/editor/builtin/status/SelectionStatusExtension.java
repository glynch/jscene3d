/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.builtin.status;

import io.github.glynch.jscene3d.editor.extension.EditorExtension;
import io.github.glynch.jscene3d.editor.extension.EditorExtensionContext;
import io.github.glynch.jscene3d.editor.selection.EditorSelection;
import io.github.glynch.jscene3d.editor.status.EditorStatusItem;
import io.github.glynch.jscene3d.editor.status.EditorStatusItemContribution;
import io.github.glynch.jscene3d.editor.status.EditorStatusItemState;
import io.github.glynch.jscene3d.editor.status.StatusBarAlignment;
import io.github.glynch.jscene3d.editor.status.StatusItemId;
import io.github.glynch.jscene3d.editor.view.EditorDetails;
import io.github.glynch.jscene3d.editor.view.EditorIcon;
import io.github.glynch.jscene3d.editor.view.EditorIcons;
import java.util.Objects;
import java.util.Optional;

/** Built-in extension publishing semantic state for the current selection. */
public final class SelectionStatusExtension implements EditorExtension {
    /** Stable identity of the selection-context status item. */
    public static final StatusItemId STATUS_ID = new StatusItemId("io.github.glynch.jscene3d.editor.selection-status");

    @Override
    public String id() {
        return "io.github.glynch.jscene3d.editor.builtin.selection-status";
    }

    @Override
    public void activate(EditorExtensionContext context) {
        EditorExtensionContext editor = Objects.requireNonNull(context, "context");
        EditorStatusItem status = editor.subscriptions()
                .add(editor.statusBar()
                        .create(new EditorStatusItemContribution(STATUS_ID, StatusBarAlignment.RIGHT, 200)));
        editor.subscriptions().add(editor.selections().observe(selection -> status.update(state(selection))));
    }

    private static EditorStatusItemState state(Optional<EditorSelection> selection) {
        Optional<EditorDetails> details = selection.flatMap(EditorSelection::details);
        Optional<EditorIcon> readOnly = details.stream()
                .flatMap(value -> value.decorations().stream())
                .filter(icon -> icon.id().equals(EditorIcons.READ_ONLY))
                .findFirst();
        if (details.isEmpty() || readOnly.isEmpty()) {
            return new EditorStatusItemState(
                    "Selection status", Optional.empty(), Optional.empty(), Optional.empty(), false);
        }

        EditorDetails selected = details.orElseThrow();
        EditorIcon icon = readOnly.orElseThrow();
        String text = selected.kind() + " · Read-only";
        String tooltip = selected.title() + " · " + icon.tooltip();
        return new EditorStatusItemState(text, Optional.of(icon), Optional.of(tooltip), Optional.empty(), true);
    }
}
