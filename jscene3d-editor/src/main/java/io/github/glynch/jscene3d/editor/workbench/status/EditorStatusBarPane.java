/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.workbench.status;

import io.github.glynch.jscene3d.editor.command.CommandId;
import io.github.glynch.jscene3d.editor.view.EditorIcon;
import io.github.glynch.jscene3d.editor.view.EditorIconId;
import io.github.glynch.jscene3d.editor.view.EditorIcons;
import io.github.glynch.jscene3d.editor.window.EditorMessage;
import io.github.glynch.jscene3d.editor.window.EditorMessageSeverity;
import io.github.glynch.jscene3d.editor.workbench.extension.EditorExtensionHost;
import io.github.glynch.jscene3d.editor.workbench.icon.JavaFxIconRenderer;
import io.github.glynch.jscene3d.editor.workbench.style.EditorStyleClasses;
import java.util.Objects;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;

/** Owns the editor's built-in and extension-contributed status presentation. */
public final class EditorStatusBarPane implements AutoCloseable {
    private final EditorExtensionHost extensions;
    private final JavaFxIconRenderer icons;
    private final JavaFxStatusBarAdapter extensionItems;
    private final Label projectStatus = createStatus("No project opened");
    private final Button projectStatusAction = createStatusAction("No project opened");
    private final Label viewportStatus = createStatus("Preview: starting");
    private final StackPane projectStatusPresentation = new StackPane(projectStatus, projectStatusAction);
    private final HBox node;

    /** Creates a status bar backed by the editor's command and status-item registries. */
    public EditorStatusBarPane(EditorExtensionHost extensions, JavaFxIconRenderer icons) {
        this.extensions = Objects.requireNonNull(extensions, "extensions");
        this.icons = Objects.requireNonNull(icons, "icons");
        extensionItems = new JavaFxStatusBarAdapter(extensions, icons);
        showProjectText();
        node = createNode();
    }

    /** Returns the JavaFX node installed at the bottom of the workbench. */
    public HBox node() {
        return node;
    }

    /** Shows neutral project status without an associated action. */
    public void showProjectStatus(String text) {
        projectStatus.setText(Objects.requireNonNull(text, "text"));
        projectStatus.setGraphic(null);
        showProjectText();
    }

    /** Updates the concise viewport state. */
    public void showViewportStatus(String text) {
        viewportStatus.setText(Objects.requireNonNull(text, "text"));
    }

    /** Presents one message and exposes its optional command as an accessible status action. */
    public void showMessage(EditorMessage message) {
        EditorMessage shown = Objects.requireNonNull(message, "message");
        if (shown.command().isEmpty()) {
            projectStatus.setText(shown.text());
            projectStatus.setGraphic(createSeverityIcon(shown.severity()));
            showProjectText();
            return;
        }
        CommandId command = shown.command().orElseThrow();
        projectStatusAction.setText(shown.text());
        projectStatusAction.setTooltip(new Tooltip(shown.text()));
        projectStatusAction.setAccessibleText(shown.text());
        projectStatusAction.setOnAction(ignored -> extensions.execute(command));
        projectStatusAction.setGraphic(createSeverityIcon(shown.severity()));
        showProjectAction();
    }

    /** Stops observing extension-contributed status items. */
    @Override
    public void close() {
        extensionItems.close();
    }

    private HBox createNode() {
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        Separator separator = new Separator(Orientation.VERTICAL);
        HBox result = new HBox(
                10.0,
                projectStatusPresentation,
                extensionItems.leftNode(),
                spacer,
                viewportStatus,
                separator,
                extensionItems.rightNode());
        result.setAlignment(Pos.CENTER_LEFT);
        result.getStyleClass().add(EditorStyleClasses.EDITOR_STATUS_BAR);
        return result;
    }

    private void showProjectText() {
        projectStatus.setManaged(true);
        projectStatus.setVisible(true);
        projectStatusAction.setManaged(false);
        projectStatusAction.setVisible(false);
    }

    private void showProjectAction() {
        projectStatus.setManaged(false);
        projectStatus.setVisible(false);
        projectStatusAction.setManaged(true);
        projectStatusAction.setVisible(true);
    }

    private Node createSeverityIcon(EditorMessageSeverity severity) {
        return switch (severity) {
            case ERROR -> icon(EditorIcons.ERROR, "Error");
            case WARNING -> icon(EditorIcons.WARNING, "Warning");
            case INFORMATION -> icon(EditorIcons.INFORMATION, "Information");
        };
    }

    private Node icon(EditorIconId id, String tooltip) {
        return icons.create(new EditorIcon(id, tooltip));
    }

    private static Label createStatus(String initialText) {
        Label status = new Label(initialText);
        status.getStyleClass().add(EditorStyleClasses.EDITOR_STATUS);
        return status;
    }

    private static Button createStatusAction(String initialText) {
        Button status = new Button(initialText);
        status.getStyleClass().addAll(EditorStyleClasses.EDITOR_STATUS, EditorStyleClasses.EDITOR_STATUS_ACTION);
        return status;
    }
}
