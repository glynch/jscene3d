/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.workbench.view;

import io.github.glynch.jscene3d.editor.builtin.diagnostics.EditorDiagnosticsModel;
import io.github.glynch.jscene3d.editor.diagnostic.EditorDiagnosticSeverity;
import io.github.glynch.jscene3d.editor.view.EditorIcon;
import io.github.glynch.jscene3d.editor.view.EditorIconId;
import io.github.glynch.jscene3d.editor.view.EditorIcons;
import io.github.glynch.jscene3d.editor.workbench.icon.JavaFxIconRenderer;
import io.github.glynch.jscene3d.editor.workbench.style.EditorStyleClasses;
import java.util.List;
import java.util.Objects;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.Tooltip;
import javafx.scene.control.TreeCell;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import org.jspecify.annotations.Nullable;

/** JavaFX diagnostic source tree and occurrence-row presentation. */
final class JavaFxDiagnosticTree extends TreeView<JavaFxDiagnosticTree.DiagnosticNode> {
    JavaFxDiagnosticTree(JavaFxIconRenderer icons) {
        setShowRoot(false);
        setCellFactory(ignored -> new DiagnosticTreeCell(icons));
        getStyleClass().add(EditorStyleClasses.EDITOR_DIAGNOSTIC_TREE);
    }

    void show(List<EditorDiagnosticsModel.Group> groups) {
        TreeItem<DiagnosticNode> root = new TreeItem<>();
        groups.stream().map(JavaFxDiagnosticTree::groupItem).forEach(root.getChildren()::add);
        setRoot(root);
    }

    private static TreeItem<DiagnosticNode> groupItem(EditorDiagnosticsModel.Group group) {
        TreeItem<DiagnosticNode> result = new TreeItem<>(new DiagnosticGroupNode(group));
        for (EditorDiagnosticsModel.Item item : group.items()) {
            TreeItem<DiagnosticNode> itemNode = new TreeItem<>(new DiagnosticItemNode(item));
            item.details().stream()
                    .map(detail -> new TreeItem<DiagnosticNode>(new DiagnosticDetailNode(detail)))
                    .forEach(itemNode.getChildren()::add);
            result.getChildren().add(itemNode);
        }
        result.setExpanded(true);
        return result;
    }

    private static final class DiagnosticTreeCell extends TreeCell<DiagnosticNode> {
        private final JavaFxIconRenderer icons;

        private DiagnosticTreeCell(JavaFxIconRenderer icons) {
            this.icons = Objects.requireNonNull(icons, "icons");
            getStyleClass().add(EditorStyleClasses.EDITOR_DIAGNOSTIC_TREE_CELL);
        }

        @Override
        protected void updateItem(@Nullable DiagnosticNode value, boolean empty) {
            super.updateItem(value, empty);
            getStyleClass()
                    .removeAll(
                            EditorStyleClasses.DIAGNOSTIC_GROUP,
                            EditorStyleClasses.DIAGNOSTIC_ITEM,
                            EditorStyleClasses.DIAGNOSTIC_DETAIL);
            setText(null);
            setContextMenu(null);
            if (empty || value == null) {
                setGraphic(null);
            } else if (value instanceof DiagnosticGroupNode(EditorDiagnosticsModel.Group group)) {
                getStyleClass().add(EditorStyleClasses.DIAGNOSTIC_GROUP);
                setGraphic(groupGraphic(group));
            } else if (value instanceof DiagnosticItemNode(EditorDiagnosticsModel.Item item)) {
                getStyleClass().add(EditorStyleClasses.DIAGNOSTIC_ITEM);
                setGraphic(itemGraphic(item));
                setContextMenu(contextMenu(item));
            } else if (value instanceof DiagnosticDetailNode(EditorDiagnosticsModel.Detail detail)) {
                getStyleClass().add(EditorStyleClasses.DIAGNOSTIC_DETAIL);
                setGraphic(detailGraphic(detail));
            }
        }

        private HBox itemGraphic(EditorDiagnosticsModel.Item item) {
            EditorDiagnosticSeverity severity = item.diagnostic().severity();
            Node icon = icons.create(new EditorIcon(icon(severity), label(severity)));
            Label message = growableLabel(item.diagnostic().message(), EditorStyleClasses.EDITOR_DIAGNOSTIC_MESSAGE);
            Label code = new Label(item.diagnostic().code());
            code.getStyleClass().add(EditorStyleClasses.EDITOR_DIAGNOSTIC_CODE);
            Label location = new Label(item.diagnostic().location());
            location.setManaged(!item.diagnostic().location().isEmpty());
            location.setVisible(!item.diagnostic().location().isEmpty());
            location.setTooltip(new Tooltip("Location: " + item.diagnostic().location()));
            location.getStyleClass().add(EditorStyleClasses.EDITOR_DIAGNOSTIC_LOCATION);
            return row(icon, message, code, location);
        }

        private static HBox groupGraphic(EditorDiagnosticsModel.Group group) {
            Label source = new Label(group.label());
            source.setTooltip(new Tooltip(group.source().toString()));
            source.getStyleClass().add(EditorStyleClasses.EDITOR_DIAGNOSTIC_SOURCE_NAME);
            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);
            Label count = new Label(Long.toString(group.items().size()));
            count.getStyleClass().add(EditorStyleClasses.EDITOR_DIAGNOSTIC_GROUP_COUNTS);
            return row(source, spacer, count);
        }

        private static HBox detailGraphic(EditorDiagnosticsModel.Detail detail) {
            Label label = new Label(detail.label());
            label.getStyleClass().add(EditorStyleClasses.EDITOR_DIAGNOSTIC_DETAIL_LABEL);
            Label value = growableLabel(detail.value(), EditorStyleClasses.EDITOR_DIAGNOSTIC_DETAIL_VALUE);
            return row(label, value);
        }

        private static Label growableLabel(String text, String styleClass) {
            Label label = new Label(text);
            label.setMinWidth(0.0);
            label.setMaxWidth(Double.MAX_VALUE);
            label.setPrefWidth(1.0);
            label.setTooltip(new Tooltip(text));
            label.getStyleClass().add(styleClass);
            HBox.setHgrow(label, Priority.ALWAYS);
            return label;
        }

        private static HBox row(Node... nodes) {
            HBox row = new HBox(8.0, nodes);
            row.setAlignment(Pos.CENTER_LEFT);
            row.setMaxWidth(Double.MAX_VALUE);
            return row;
        }

        private static ContextMenu contextMenu(EditorDiagnosticsModel.Item item) {
            MenuItem copy = new MenuItem("Copy");
            copy.setOnAction(ignored -> copy(item.copyText()));
            MenuItem copyMessage = new MenuItem("Copy Message");
            copyMessage.setOnAction(ignored -> copy(item.diagnostic().message()));
            return new ContextMenu(copy, copyMessage);
        }

        private static void copy(String text) {
            ClipboardContent content = new ClipboardContent();
            content.putString(text);
            Clipboard.getSystemClipboard().setContent(content);
        }

        private static EditorIconId icon(EditorDiagnosticSeverity severity) {
            return switch (severity) {
                case ERROR -> EditorIcons.ERROR;
                case WARNING -> EditorIcons.WARNING;
                case INFORMATION, HINT -> EditorIcons.INFORMATION;
            };
        }

        private static String label(EditorDiagnosticSeverity severity) {
            return severity == EditorDiagnosticSeverity.INFORMATION
                    ? "Information"
                    : severity.name().toLowerCase();
        }
    }

    sealed interface DiagnosticNode permits DiagnosticGroupNode, DiagnosticItemNode, DiagnosticDetailNode {}

    private record DiagnosticGroupNode(EditorDiagnosticsModel.Group group) implements DiagnosticNode {}

    private record DiagnosticItemNode(EditorDiagnosticsModel.Item item) implements DiagnosticNode {}

    private record DiagnosticDetailNode(EditorDiagnosticsModel.Detail detail) implements DiagnosticNode {}
}
