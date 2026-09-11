/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.workbench.view;

import io.github.glynch.jscene3d.editor.builtin.diagnostics.DiagnosticsView;
import io.github.glynch.jscene3d.editor.view.EditorCollectionView;
import io.github.glynch.jscene3d.editor.view.EditorDetailsView;
import io.github.glynch.jscene3d.editor.view.EditorTreeView;
import io.github.glynch.jscene3d.editor.view.EditorView;
import io.github.glynch.jscene3d.editor.workbench.extension.EditorExtensionHost;
import io.github.glynch.jscene3d.editor.workbench.icon.JavaFxIconRenderer;
import io.github.glynch.jscene3d.editor.workbench.style.EditorStyleClasses;
import java.util.Objects;
import java.util.Optional;
import javafx.scene.control.Label;

/** Selects the JavaFX adapter for a logical editor view. */
final class JavaFxViewRenderer {
    private final EditorExtensionHost extensions;
    private final JavaFxIconRenderer icons;

    JavaFxViewRenderer(EditorExtensionHost extensions, JavaFxIconRenderer icons) {
        this.extensions = Objects.requireNonNull(extensions, "extensions");
        this.icons = Objects.requireNonNull(icons, "icons");
    }

    JavaFxRenderedView render(EditorView view) {
        EditorView logicalView = Objects.requireNonNull(view, "view");
        if (logicalView instanceof DiagnosticsView diagnosticsView) {
            JavaFxDiagnosticsViewAdapter adapter = new JavaFxDiagnosticsViewAdapter(diagnosticsView, icons);
            return new JavaFxRenderedView(
                    adapter.node(),
                    adapter.titleGraphic(),
                    Optional.of(adapter.titleActions()),
                    adapter::close,
                    adapter::requestFocus);
        }
        if (logicalView instanceof EditorDetailsView detailsView) {
            JavaFxDetailsViewAdapter adapter = new JavaFxDetailsViewAdapter(detailsView, icons);
            return standard(logicalView, adapter.node(), adapter::close, adapter::requestFocus);
        }
        if (logicalView instanceof EditorCollectionView<?> collectionView) {
            return renderCollection(collectionView);
        }
        if (logicalView instanceof EditorTreeView<?> treeView) {
            return renderTree(treeView);
        }
        Label unsupported = new Label("Unsupported view kind: " + logicalView.kind());
        unsupported.getStyleClass().add(EditorStyleClasses.EDITOR_EMPTY_DETAIL);
        return standard(logicalView, unsupported, () -> {}, unsupported::requestFocus);
    }

    private <T> JavaFxRenderedView renderCollection(EditorCollectionView<T> view) {
        JavaFxCollectionViewAdapter<T> adapter = new JavaFxCollectionViewAdapter<>(view, extensions::execute, icons);
        return standard(view, adapter.node(), adapter::close, adapter::requestFocus);
    }

    private <T> JavaFxRenderedView renderTree(EditorTreeView<T> view) {
        JavaFxTreeViewAdapter<T> adapter = new JavaFxTreeViewAdapter<>(view, extensions::execute, icons);
        return standard(view, adapter.node(), adapter::close, adapter::requestFocus);
    }

    private static JavaFxRenderedView standard(
            EditorView view, javafx.scene.Node node, Runnable close, Runnable requestFocus) {
        Label title = new Label(view.title());
        title.getStyleClass().add(EditorStyleClasses.EDITOR_PANEL_TAB_LABEL);
        return new JavaFxRenderedView(node, title, Optional.empty(), close, requestFocus);
    }
}
