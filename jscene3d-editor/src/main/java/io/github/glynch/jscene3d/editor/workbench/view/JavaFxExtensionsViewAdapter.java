/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.workbench.view;

import io.github.glynch.jscene3d.editor.builtin.extensions.ExtensionsView;
import io.github.glynch.jscene3d.editor.extension.EditorExtensionDescriptor;
import io.github.glynch.jscene3d.editor.lifecycle.EditorRegistration;
import io.github.glynch.jscene3d.editor.workbench.style.EditorStyleClasses;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.OverrunStyle;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

/** Workbench-owned JavaFX adapter for the installed-extension browser. */
final class JavaFxExtensionsViewAdapter implements AutoCloseable {
    private static final Comparator<EditorExtensionDescriptor> EXTENSION_ORDER =
            Comparator.comparing(EditorExtensionDescriptor::displayName, String.CASE_INSENSITIVE_ORDER);

    private final ExtensionsView view;
    private final VBox root = new VBox();
    private final TextField search = new TextField();
    private final Label count = new Label();
    private final ListView<EditorExtensionDescriptor> list = new ListView<>();
    private final EditorRegistration extensionsRegistration;
    private final EditorRegistration selectionRegistration;
    private List<EditorExtensionDescriptor> installed = List.of();
    private int selectionFeedbackSuppressionDepth;

    JavaFxExtensionsViewAdapter(ExtensionsView view) {
        this.view = Objects.requireNonNull(view, "view");
        configureView();
        extensionsRegistration = view.observe(this::showInstalled);
        selectionRegistration = view.observeSelection(this::showSelection);
    }

    Node node() {
        return root;
    }

    void requestFocus() {
        search.requestFocus();
    }

    @Override
    public void close() {
        selectionRegistration.close();
        extensionsRegistration.close();
        list.getItems().clear();
    }

    private void configureView() {
        search.setPromptText("Search installed extensions…");
        search.textProperty().addListener((ignored, previous, current) -> filter());
        search.getStyleClass().add(EditorStyleClasses.EDITOR_EXTENSIONS_SEARCH);
        Label heading = new Label("INSTALLED");
        heading.getStyleClass().add(EditorStyleClasses.EDITOR_EXTENSIONS_GROUP_TITLE);
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox group = new HBox(8.0, heading, spacer, count);
        group.setAlignment(Pos.CENTER_LEFT);
        group.getStyleClass().add(EditorStyleClasses.EDITOR_EXTENSIONS_GROUP);
        list.setCellFactory(ignored -> new ExtensionCell());
        list.getSelectionModel().selectedItemProperty().addListener((ignored, previous, selected) -> {
            if (selectionFeedbackSuppressionDepth == 0) {
                view.select(Optional.ofNullable(selected));
            }
        });
        list.setOnMouseClicked(ignored -> {
            EditorExtensionDescriptor selected = list.getSelectionModel().getSelectedItem();
            if (selected != null) {
                view.select(Optional.of(selected));
            }
        });
        list.getStyleClass().add(EditorStyleClasses.EDITOR_EXTENSIONS_LIST);
        VBox.setVgrow(list, Priority.ALWAYS);
        root.getChildren().setAll(search, group, list);
        root.getStyleClass().add(EditorStyleClasses.EDITOR_EXTENSIONS_VIEW);
    }

    private void showInstalled(List<EditorExtensionDescriptor> descriptors) {
        installed = List.copyOf(Objects.requireNonNull(descriptors, "descriptors"));
        runOnApplicationThread(this::filter);
    }

    private void filter() {
        String query = search.getText().strip().toLowerCase(Locale.ROOT);
        List<EditorExtensionDescriptor> visible = installed.stream()
                .filter(descriptor -> searchableText(descriptor).contains(query))
                .sorted(EXTENSION_ORDER)
                .toList();
        withoutSelectionFeedback(() -> {
            list.getItems().setAll(visible);
            synchronizeSelection();
        });
        count.setText(Integer.toString(visible.size()));
    }

    private void showSelection(Optional<EditorExtensionDescriptor> selected) {
        runOnApplicationThread(() -> withoutSelectionFeedback(() -> {
            if (selected.isPresent() && list.getItems().contains(selected.orElseThrow())) {
                list.getSelectionModel().select(selected.orElseThrow());
            } else {
                list.getSelectionModel().clearSelection();
            }
        }));
    }

    private void synchronizeSelection() {
        Optional<EditorExtensionDescriptor> selected = view.selection();
        if (selected.isPresent() && list.getItems().contains(selected.orElseThrow())) {
            list.getSelectionModel().select(selected.orElseThrow());
        } else {
            list.getSelectionModel().clearSelection();
        }
    }

    private void withoutSelectionFeedback(Runnable action) {
        selectionFeedbackSuppressionDepth++;
        try {
            action.run();
        } finally {
            selectionFeedbackSuppressionDepth--;
        }
    }

    private static String searchableText(EditorExtensionDescriptor descriptor) {
        return String.join(
                        " ",
                        descriptor.displayName(),
                        descriptor.description(),
                        descriptor.publisher(),
                        descriptor.id())
                .toLowerCase(Locale.ROOT);
    }

    private static void runOnApplicationThread(Runnable action) {
        if (Platform.isFxApplicationThread()) {
            action.run();
        } else {
            Platform.runLater(action);
        }
    }

    private static final class ExtensionCell extends ListCell<EditorExtensionDescriptor> {
        private static final double HORIZONTAL_INSETS = 16.0;

        private final Label name = rowLabel(EditorStyleClasses.EDITOR_EXTENSION_NAME);
        private final Label description = rowLabel(EditorStyleClasses.EDITOR_EXTENSION_DESCRIPTION);
        private final Label metadata = rowLabel(EditorStyleClasses.EDITOR_EXTENSION_METADATA);
        private final VBox content = new VBox(3.0, name, description, metadata);

        private ExtensionCell() {
            content.getStyleClass().add(EditorStyleClasses.EDITOR_EXTENSION_ROW);
            setAlignment(Pos.CENTER_LEFT);
            widthProperty().addListener((ignored, previous, current) -> resizeContent(current.doubleValue()));
        }

        @Override
        protected void updateItem(EditorExtensionDescriptor descriptor, boolean empty) {
            super.updateItem(descriptor, empty);
            if (empty || descriptor == null) {
                setText(null);
                setGraphic(null);
                setAccessibleText(null);
                setTooltip(null);
                return;
            }
            String metadataText = descriptor.publisher()
                    + descriptor.version().map(version -> "  ·  " + version).orElse("")
                    + (descriptor.builtIn() ? "  ·  Built in" : "");
            name.setText(descriptor.displayName());
            description.setText(descriptor.description());
            metadata.setText(metadataText);
            resizeContent(getWidth());
            setText(null);
            setGraphic(content);
            setAccessibleText(String.join(", ", descriptor.displayName(), descriptor.description(), metadataText));
            setTooltip(
                    new Tooltip(String.join("\n", descriptor.displayName(), descriptor.description(), metadataText)));
        }

        private void resizeContent(double cellWidth) {
            double availableWidth = Math.max(0.0, cellWidth - HORIZONTAL_INSETS);
            content.setMinWidth(availableWidth);
            content.setPrefWidth(availableWidth);
            content.setMaxWidth(availableWidth);
        }

        private static Label rowLabel(String styleClass) {
            Label label = new Label();
            label.setMinWidth(0.0);
            label.setMaxWidth(Double.MAX_VALUE);
            label.setTextOverrun(OverrunStyle.ELLIPSIS);
            label.setWrapText(false);
            label.getStyleClass().add(styleClass);
            return label;
        }
    }
}
