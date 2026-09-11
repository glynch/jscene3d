/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.workbench.view;

import io.github.glynch.jscene3d.editor.lifecycle.EditorRegistration;
import io.github.glynch.jscene3d.editor.view.EditorDetails;
import io.github.glynch.jscene3d.editor.view.EditorDetailsDataProvider;
import io.github.glynch.jscene3d.editor.view.EditorDetailsView;
import io.github.glynch.jscene3d.editor.workbench.style.EditorStyleClasses;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.OverrunStyle;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TitledPane;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

/** Workbench-owned JavaFX adapter for any toolkit-independent details view. */
final class JavaFxDetailsViewAdapter implements AutoCloseable {
    private final StackPane root = new StackPane();
    private final VBox empty = new VBox();
    private final VBox selected = new VBox();
    private final VBox sections = new VBox();
    private final Label title = new Label();
    private final Label kind = new Label();
    private final Label source = new Label();
    private final HBox decorations = new HBox(6.0);
    private final ScrollPane scroll = new ScrollPane();
    private final EditorRegistration registration;

    JavaFxDetailsViewAdapter(EditorDetailsView view) {
        EditorDetailsView logicalView = Objects.requireNonNull(view, "view");
        EditorDetailsDataProvider provider = Objects.requireNonNull(logicalView.dataProvider(), "view.dataProvider()");
        createEmptyState(logicalView);
        createDetailsState();
        root.getChildren().setAll(empty, scroll);
        registration = provider.observe(this::show);
    }

    Node node() {
        return root;
    }

    void requestFocus() {
        if (scroll.isVisible()) {
            scroll.requestFocus();
        } else {
            empty.requestFocus();
        }
    }

    @Override
    public void close() {
        registration.close();
        sections.getChildren().clear();
        decorations.getChildren().clear();
    }

    private void createEmptyState(EditorDetailsView view) {
        Label emptyTitle = new Label(view.emptyTitle());
        emptyTitle.getStyleClass().add(EditorStyleClasses.EDITOR_EMPTY_TITLE);
        Label emptyMessage = new Label(view.emptyMessage());
        emptyMessage.getStyleClass().add(EditorStyleClasses.EDITOR_EMPTY_DETAIL);
        empty.getChildren().setAll(emptyTitle, emptyMessage);
        empty.setSpacing(10.0);
        empty.getStyleClass().add(EditorStyleClasses.EDITOR_INSPECTOR_EMPTY);
    }

    private void createDetailsState() {
        title.getStyleClass().add(EditorStyleClasses.EDITOR_INSPECTOR_TITLE);
        kind.getStyleClass().add(EditorStyleClasses.EDITOR_INSPECTOR_KIND);
        source.getStyleClass().add(EditorStyleClasses.EDITOR_INSPECTOR_SOURCE);
        source.setWrapText(true);
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox titleLine = new HBox(6.0, title, spacer, decorations);
        titleLine.setAlignment(Pos.CENTER_LEFT);
        VBox header = new VBox(4.0, titleLine, kind, source);
        header.getStyleClass().add(EditorStyleClasses.EDITOR_INSPECTOR_HEADER);
        sections.getStyleClass().add(EditorStyleClasses.EDITOR_INSPECTOR_SECTIONS);
        selected.getChildren().setAll(header, sections);
        selected.getStyleClass().add(EditorStyleClasses.EDITOR_INSPECTOR_SELECTED);
        scroll.setContent(selected);
        scroll.setFitToWidth(true);
        scroll.getStyleClass().add(EditorStyleClasses.EDITOR_INSPECTOR_SCROLL);
    }

    private void show(Optional<EditorDetails> updated) {
        boolean present = updated.isPresent();
        empty.setManaged(!present);
        empty.setVisible(!present);
        scroll.setManaged(present);
        scroll.setVisible(present);
        if (updated.isEmpty()) {
            sections.getChildren().clear();
            decorations.getChildren().clear();
            return;
        }
        EditorDetails details = updated.orElseThrow();
        title.setText(details.title());
        title.setTooltip(new Tooltip(details.identity()));
        kind.setText(details.kind());
        source.setText(details.source());
        source.setTooltip(new Tooltip(details.source()));
        decorations
                .getChildren()
                .setAll(details.decorations().stream()
                        .map(icon -> JavaFxIconRenderer.create(icon, EditorStyleClasses.EDITOR_DETAILS_DECORATION))
                        .toList());
        sections.getChildren()
                .setAll(details.sections().stream()
                        .map(JavaFxDetailsViewAdapter::createSection)
                        .toList());
        scroll.setVvalue(0.0);
    }

    private static TitledPane createSection(EditorDetails.Section section) {
        VBox content = new VBox(6.0);
        content.getStyleClass().add(EditorStyleClasses.EDITOR_INSPECTOR_SECTION_CONTENT);
        section.description().ifPresent(description -> {
            Label detail = new Label(description);
            detail.setWrapText(true);
            detail.getStyleClass().add(EditorStyleClasses.EDITOR_INSPECTOR_DESCRIPTION);
            content.getChildren().add(detail);
        });
        if (!section.metadataAvailable()) {
            Label unavailable = new Label("Descriptor metadata unavailable");
            unavailable.getStyleClass().add(EditorStyleClasses.EDITOR_INSPECTOR_METADATA_WARNING);
            content.getChildren().add(unavailable);
        }
        section.properties().stream()
                .map(JavaFxDetailsViewAdapter::createProperty)
                .forEach(content.getChildren()::add);
        TitledPane pane = new TitledPane(section.title(), content);
        pane.setAnimated(false);
        pane.setExpanded(true);
        pane.getStyleClass().add(EditorStyleClasses.EDITOR_INSPECTOR_SECTION);
        return pane;
    }

    private static VBox createProperty(EditorDetails.Property property) {
        Label name = new Label(property.displayName());
        name.getStyleClass().add(EditorStyleClasses.EDITOR_INSPECTOR_PROPERTY_NAME);
        Label value = new Label(property.value());
        value.setMaxWidth(Double.MAX_VALUE);
        value.setTextOverrun(OverrunStyle.CENTER_ELLIPSIS);
        value.getStyleClass().add(EditorStyleClasses.EDITOR_INSPECTOR_VALUE);
        if (property.origin() == EditorDetails.ValueOrigin.DEFAULT) {
            value.getStyleClass().add(EditorStyleClasses.EDITOR_INSPECTOR_DEFAULT_VALUE);
        } else if (property.origin() == EditorDetails.ValueOrigin.UNSET) {
            value.getStyleClass().add(EditorStyleClasses.EDITOR_INSPECTOR_UNSET_VALUE);
        }
        Label metadata = new Label(propertyMetadata(property));
        metadata.getStyleClass().add(EditorStyleClasses.EDITOR_INSPECTOR_PROPERTY_METADATA);
        String tooltip = propertyTooltip(property);
        if (!tooltip.isEmpty()) {
            Tooltip help = new Tooltip(tooltip);
            name.setTooltip(help);
            value.setTooltip(help);
        }
        VBox row = new VBox(3.0, name, value, metadata);
        row.getStyleClass().add(EditorStyleClasses.EDITOR_INSPECTOR_PROPERTY);
        return row;
    }

    private static String propertyMetadata(EditorDetails.Property property) {
        StringBuilder text = new StringBuilder(property.valueKind());
        if (property.required()) {
            text.append(" · required");
        }
        if (property.origin() == EditorDetails.ValueOrigin.DEFAULT) {
            text.append(" · default");
        } else if (property.origin() == EditorDetails.ValueOrigin.UNSET) {
            text.append(" · not set");
        }
        return text.toString();
    }

    private static String propertyTooltip(EditorDetails.Property property) {
        StringBuilder text = new StringBuilder();
        property.description().ifPresent(text::append);
        for (Map.Entry<String, String> constraint : property.constraints().entrySet()) {
            if (!text.isEmpty()) {
                text.append('\n');
            }
            text.append(constraint.getKey()).append(": ").append(constraint.getValue());
        }
        return text.toString();
    }
}
