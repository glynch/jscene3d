/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.workbench.view;

import io.github.glynch.jscene3d.editor.builtin.diagnostics.DiagnosticsView;
import io.github.glynch.jscene3d.editor.builtin.diagnostics.EditorDiagnosticsModel;
import io.github.glynch.jscene3d.editor.diagnostic.EditorDiagnosticSeverity;
import io.github.glynch.jscene3d.editor.lifecycle.EditorRegistration;
import io.github.glynch.jscene3d.editor.view.EditorIcon;
import io.github.glynch.jscene3d.editor.view.EditorIconId;
import io.github.glynch.jscene3d.editor.view.EditorIcons;
import io.github.glynch.jscene3d.editor.workbench.icon.JavaFxIconRenderer;
import io.github.glynch.jscene3d.editor.workbench.style.EditorStyleClasses;
import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;
import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

/** Renders the built-in Diagnostics view without leaking JavaFX into its extension contract. */
final class JavaFxDiagnosticsViewAdapter implements AutoCloseable {
    private final EditorDiagnosticsModel model;
    private final JavaFxIconRenderer icons;
    private final VBox node;
    private final HBox titleGraphic;
    private final HBox titleActions = new HBox(5.0);
    private final Label badge = new Label();
    private final Label empty = new Label("No diagnostics");
    private final TextField filter = new TextField();
    private final JavaFxDiagnosticTree tree;
    private final Map<EditorDiagnosticSeverity, ToggleButton> severityButtons =
            new EnumMap<>(EditorDiagnosticSeverity.class);
    private final EditorRegistration modelRegistration;
    private boolean closed;

    JavaFxDiagnosticsViewAdapter(DiagnosticsView view, JavaFxIconRenderer icons) {
        DiagnosticsView diagnostics = Objects.requireNonNull(view, "view");
        this.icons = Objects.requireNonNull(icons, "icons");
        model = diagnostics.model();
        tree = new JavaFxDiagnosticTree(icons);
        titleGraphic = createTitleGraphic(diagnostics.title());
        configureActions();
        node = createContent();
        modelRegistration = model.observe(this::refreshLater);
        refresh();
    }

    VBox node() {
        return node;
    }

    HBox titleGraphic() {
        return titleGraphic;
    }

    HBox titleActions() {
        return titleActions;
    }

    void requestFocus() {
        tree.requestFocus();
    }

    @Override
    public void close() {
        closed = true;
        modelRegistration.close();
    }

    private HBox createTitleGraphic(String title) {
        Label text = new Label(title);
        text.getStyleClass().add(EditorStyleClasses.EDITOR_PANEL_TAB_LABEL);
        badge.getStyleClass().add(EditorStyleClasses.EDITOR_DIAGNOSTIC_BADGE);
        HBox graphic = new HBox(6.0, text, badge);
        graphic.setAlignment(Pos.CENTER_LEFT);
        return graphic;
    }

    private void configureActions() {
        titleActions
                .getChildren()
                .addAll(
                        severityButton(EditorDiagnosticSeverity.ERROR, EditorIcons.ERROR, "Errors"),
                        severityButton(EditorDiagnosticSeverity.WARNING, EditorIcons.WARNING, "Warnings"),
                        severityButton(EditorDiagnosticSeverity.INFORMATION, EditorIcons.INFORMATION, "Information"),
                        severityButton(EditorDiagnosticSeverity.HINT, EditorIcons.INFORMATION, "Hints"));
        filter.setPromptText("Filter diagnostics…");
        filter.setPrefWidth(210.0);
        filter.getStyleClass().add(EditorStyleClasses.EDITOR_DIAGNOSTIC_FILTER);
        filter.textProperty().addListener((ignored, previous, current) -> model.filter(current));
        titleActions.getChildren().add(filter);
        titleActions.setAlignment(Pos.CENTER_RIGHT);
        titleActions.getStyleClass().add(EditorStyleClasses.EDITOR_DIAGNOSTIC_TOOLS);
    }

    private ToggleButton severityButton(EditorDiagnosticSeverity severity, EditorIconId icon, String tooltip) {
        ToggleButton button = new ToggleButton();
        button.setSelected(true);
        button.setGraphic(icons.create(new EditorIcon(icon, tooltip)));
        button.setTooltip(new Tooltip());
        button.getStyleClass().add(EditorStyleClasses.EDITOR_DIAGNOSTIC_SEVERITY_FILTER);
        button.setOnAction(ignored -> model.showSeverity(severity, button.isSelected()));
        severityButtons.put(severity, button);
        return button;
    }

    private VBox createContent() {
        empty.getStyleClass()
                .addAll(EditorStyleClasses.EDITOR_EMPTY_DETAIL, EditorStyleClasses.EDITOR_DIAGNOSTIC_EMPTY);
        StackPane body = new StackPane(tree, empty);
        VBox.setVgrow(body, Priority.ALWAYS);
        VBox panel = new VBox(body);
        panel.getStyleClass().addAll(EditorStyleClasses.EDITOR_PANEL, EditorStyleClasses.EDITOR_DIAGNOSTICS_PANEL);
        return panel;
    }

    private void refreshLater() {
        if (Platform.isFxApplicationThread()) {
            refresh();
        } else {
            Platform.runLater(this::refresh);
        }
    }

    private void refresh() {
        if (closed) {
            return;
        }
        EditorDiagnosticsModel.View view = model.view();
        badge.setText(Long.toString(view.total()));
        badge.setManaged(view.total() > 0L);
        badge.setVisible(view.total() > 0L);
        updateButton(EditorDiagnosticSeverity.ERROR, view.errors());
        updateButton(EditorDiagnosticSeverity.WARNING, view.warnings());
        updateButton(EditorDiagnosticSeverity.INFORMATION, view.information());
        updateButton(EditorDiagnosticSeverity.HINT, view.hints());
        empty.setText(view.total() == 0L ? "No diagnostics" : hiddenDiagnosticsMessage(view.total()));
        boolean noVisibleDiagnostics = view.visible() == 0L;
        empty.setManaged(noVisibleDiagnostics);
        empty.setVisible(noVisibleDiagnostics);
        tree.setManaged(!noVisibleDiagnostics);
        tree.setVisible(!noVisibleDiagnostics);
        tree.show(view.groups());
    }

    private void updateButton(EditorDiagnosticSeverity severity, long count) {
        ToggleButton button = severityButtons.get(severity);
        String label = severityLabel(severity);
        button.setText(Long.toString(count));
        button.getTooltip().setText((button.isSelected() ? "Hide " : "Show ") + label);
        button.setAccessibleText((button.isSelected() ? "Showing " : "Hiding ") + count + " " + label);
    }

    private static String hiddenDiagnosticsMessage(long total) {
        return total + (total == 1L ? " diagnostic is" : " diagnostics are") + " hidden by the current filters.";
    }

    private static String severityLabel(EditorDiagnosticSeverity severity) {
        return switch (severity) {
            case ERROR -> "errors";
            case WARNING -> "warnings";
            case INFORMATION -> "information diagnostics";
            case HINT -> "hints";
        };
    }
}
