/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.workbench.appearance;

import io.github.glynch.jscene3d.editor.lifecycle.EditorRegistration;
import io.github.glynch.jscene3d.editor.theme.EditorColorThemeId;
import io.github.glynch.jscene3d.editor.workbench.style.EditorStyleClasses;
import java.util.List;
import java.util.Objects;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.util.StringConverter;

/** JavaFX settings surface for user-level color-theme and source-editor preferences. */
public final class JavaFxAppearanceSettingsPane implements AutoCloseable {
    private final EditorColorThemeRegistry appearances;
    private final BorderPane root = new BorderPane();
    private final ComboBox<EditorResolvedColorTheme> themes = new ComboBox<>();
    private final ComboBox<String> fontFamilies = new ComboBox<>();
    private final Spinner<Integer> fontSize = new Spinner<>();
    private final EditorRegistration themeRegistration;
    private final EditorRegistration appearanceRegistration;
    private boolean updating;

    /** Creates a generated appearance settings surface around the live registry. */
    public JavaFxAppearanceSettingsPane(EditorColorThemeRegistry appearances) {
        this.appearances = Objects.requireNonNull(appearances, "appearances");
        configureControls();
        root.getStyleClass().add(EditorStyleClasses.EDITOR_SETTINGS_ROOT);
        root.setTop(header());
        root.setCenter(settingsScroll());
        themeRegistration = appearances.observeThemes(this::replaceThemes);
        appearanceRegistration = appearances.observeAppearance(this::showAppearance);
    }

    /** Returns the settings node. */
    public Node node() {
        return root;
    }

    @Override
    public void close() {
        appearanceRegistration.close();
        themeRegistration.close();
    }

    private void configureControls() {
        themes.setConverter(new StringConverter<>() {
            @Override
            public String toString(EditorResolvedColorTheme theme) {
                return theme == null ? "" : theme.label();
            }

            @Override
            public EditorResolvedColorTheme fromString(String label) {
                return themes.getItems().stream()
                        .filter(theme -> theme.label().equals(label))
                        .findFirst()
                        .orElse(null);
            }
        });
        themes.setMaxWidth(Double.MAX_VALUE);
        themes.setOnAction(ignored -> {
            EditorResolvedColorTheme selected = themes.getValue();
            if (!updating && selected != null) {
                appearances.select(selected.id());
            }
        });

        fontFamilies.getItems().setAll(Font.getFamilies());
        fontFamilies.setEditable(true);
        fontFamilies.setMaxWidth(Double.MAX_VALUE);
        fontFamilies.setOnAction(ignored -> saveFont());
        fontFamilies.getEditor().focusedProperty().addListener((ignored, wasFocused, focused) -> {
            if (wasFocused && !focused) {
                saveFont();
            }
        });

        fontSize.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(8, 48, 13));
        fontSize.setEditable(true);
        fontSize.setMaxWidth(Double.MAX_VALUE);
        fontSize.valueProperty().addListener((ignored, previous, selected) -> saveFont());
        fontSize.getEditor().setOnAction(ignored -> commitFontSize());
        fontSize.getEditor().focusedProperty().addListener((ignored, wasFocused, focused) -> {
            if (wasFocused && !focused) {
                commitFontSize();
            }
        });
    }

    private Node header() {
        Label title = new Label("Appearance");
        title.getStyleClass().add(EditorStyleClasses.EDITOR_SETTINGS_TITLE);
        Label description = new Label(
                "Color themes and source-editor fonts are saved for this user and update open editors immediately.");
        description.setWrapText(true);
        description.getStyleClass().add(EditorStyleClasses.EDITOR_SETTINGS_DESCRIPTION);
        VBox header = new VBox(5.0, title, description);
        header.getStyleClass().add(EditorStyleClasses.EDITOR_SETTINGS_HEADER);
        return header;
    }

    private Node settingsScroll() {
        VBox settings = new VBox();
        settings.getStyleClass().add(EditorStyleClasses.EDITOR_SETTINGS_CONTENT);
        Label category = new Label("Workbench");
        category.getStyleClass().add(EditorStyleClasses.EDITOR_SETTINGS_CATEGORY);
        settings.getChildren()
                .addAll(
                        category,
                        row("Color theme", "Controls semantic workbench and source-code colors.", themes),
                        row(
                                "Editor font",
                                "Controls source editors independently of the workbench UI font.",
                                fontFamilies),
                        row("Editor font size", "Size in points from 8 to 48.", fontSize));
        ScrollPane scroll = new ScrollPane(settings);
        scroll.setFitToWidth(true);
        scroll.getStyleClass().add(EditorStyleClasses.EDITOR_SETTINGS_SCROLL);
        return scroll;
    }

    private static Node row(String name, String detail, Node control) {
        Label title = new Label(name);
        title.getStyleClass().add(EditorStyleClasses.EDITOR_SETTINGS_NAME);
        Label description = new Label(detail);
        description.setWrapText(true);
        description.getStyleClass().add(EditorStyleClasses.EDITOR_SETTINGS_DESCRIPTION);
        VBox labels = new VBox(3.0, title, description);
        HBox value = new HBox(control);
        value.setAlignment(Pos.CENTER_LEFT);
        value.getStyleClass().add(EditorStyleClasses.EDITOR_SETTINGS_VALUE);
        HBox.setHgrow(control, Priority.ALWAYS);
        BorderPane row = new BorderPane();
        row.setLeft(labels);
        row.setRight(value);
        row.getStyleClass().add(EditorStyleClasses.EDITOR_SETTINGS_ROW);
        return row;
    }

    private void replaceThemes(List<EditorResolvedColorTheme> available) {
        EditorColorThemeId selected = appearances.current().colorTheme().id();
        updating = true;
        themes.getItems().setAll(available);
        themes.setValue(available.stream()
                .filter(theme -> theme.id().equals(selected))
                .findFirst()
                .orElse(null));
        updating = false;
    }

    private void showAppearance(EditorAppearanceSnapshot appearance) {
        updating = true;
        themes.setValue(themes.getItems().stream()
                .filter(theme -> theme.id().equals(appearance.colorTheme().id()))
                .findFirst()
                .orElse(null));
        if (!fontFamilies.getItems().contains(appearance.editorFontFamily())) {
            fontFamilies.getItems().add(appearance.editorFontFamily());
        }
        fontFamilies.setValue(appearance.editorFontFamily());
        fontFamilies.getEditor().setText(appearance.editorFontFamily());
        fontSize.getValueFactory().setValue(appearance.editorFontSize());
        updating = false;
    }

    private void saveFont() {
        if (updating) {
            return;
        }
        String family = fontFamilies.getEditor().getText().strip();
        Integer size = fontSize.getValue();
        if (!family.isEmpty() && size != null) {
            appearances.setEditorFont(family, size);
        }
    }

    private void commitFontSize() {
        if (updating) {
            return;
        }
        SpinnerValueFactory<Integer> values = fontSize.getValueFactory();
        try {
            int size = Integer.parseInt(fontSize.getEditor().getText().strip());
            if (size < 8 || size > 48) {
                throw new NumberFormatException("font size must be between 8 and 48");
            }
            values.setValue(size);
            saveFont();
        } catch (NumberFormatException exception) {
            fontSize.getEditor().setText(Integer.toString(values.getValue()));
        }
    }
}
