/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.workbench.configuration;

import io.github.glynch.jscene3d.configuration.SettingChoice;
import io.github.glynch.jscene3d.configuration.SettingDefinition;
import io.github.glynch.jscene3d.configuration.SettingValueType;
import io.github.glynch.jscene3d.editor.EditorProjectSession;
import io.github.glynch.jscene3d.editor.window.EditorMessage;
import io.github.glynch.jscene3d.editor.window.EditorMessageSeverity;
import io.github.glynch.jscene3d.editor.workbench.style.EditorStyleClasses;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;
import javafx.beans.value.ChangeListener;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Separator;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;

/** Generates project-setting controls entirely from registered declarations. */
public final class JavaFxProjectSettingsPane {
    private final EditorProjectSession session;
    private final Consumer<EditorMessage> messages;
    private final BorderPane root = new BorderPane();

    /** Creates a generated settings editor for one loaded project session. */
    public JavaFxProjectSettingsPane(EditorProjectSession session, Consumer<EditorMessage> messages) {
        this.session = Objects.requireNonNull(session, "session");
        this.messages = Objects.requireNonNull(messages, "messages");
        root.getStyleClass().add(EditorStyleClasses.EDITOR_SETTINGS_ROOT);
        root.setTop(header());
        root.setCenter(settingsScroll());
    }

    /** Returns the generated JavaFX settings editor. */
    public Node node() {
        return root;
    }

    private Node header() {
        Label title = new Label("Project Settings");
        title.getStyleClass().add(EditorStyleClasses.EDITOR_SETTINGS_TITLE);
        Label description = new Label("Settings are saved to .jscene3d/settings.json as soon as you change them.");
        description.getStyleClass().add(EditorStyleClasses.EDITOR_SETTINGS_DESCRIPTION);
        VBox header = new VBox(5.0, title, description);
        header.getStyleClass().add(EditorStyleClasses.EDITOR_SETTINGS_HEADER);
        return header;
    }

    private Node settingsScroll() {
        VBox settings = new VBox();
        settings.getStyleClass().add(EditorStyleClasses.EDITOR_SETTINGS_CONTENT);
        String previousOwner = null;
        String previousCategory = null;
        for (SettingDefinition<?> definition : orderedDefinitions()) {
            if (!definition.owner().equals(previousOwner)) {
                if (previousOwner != null) {
                    settings.getChildren().add(new Separator());
                }
                Label owner = new Label(definition.ownerDisplayName());
                owner.getStyleClass().add(EditorStyleClasses.EDITOR_SETTINGS_OWNER);
                settings.getChildren().add(owner);
                previousOwner = definition.owner();
                previousCategory = null;
            }
            if (!definition.category().equals(previousCategory)) {
                Label category = new Label(definition.category());
                category.getStyleClass().add(EditorStyleClasses.EDITOR_SETTINGS_CATEGORY);
                settings.getChildren().add(category);
                previousCategory = definition.category();
            }
            settings.getChildren().add(settingRow(definition));
        }
        if (settings.getChildren().isEmpty()) {
            Label empty = new Label("This project has no registered settings.");
            empty.getStyleClass().add(EditorStyleClasses.EDITOR_EMPTY_DETAIL);
            settings.getChildren().add(empty);
        }
        ScrollPane scroll = new ScrollPane(settings);
        scroll.setFitToWidth(true);
        scroll.getStyleClass().add(EditorStyleClasses.EDITOR_SETTINGS_SCROLL);
        return scroll;
    }

    private List<SettingDefinition<?>> orderedDefinitions() {
        List<SettingDefinition<?>> definitions =
                session.configuration().registry().definitions();
        Map<String, Integer> ownerOrder = new LinkedHashMap<>();
        Map<CategoryKey, Integer> categoryOrder = new LinkedHashMap<>();
        for (SettingDefinition<?> definition : definitions) {
            ownerOrder.computeIfAbsent(definition.owner(), ignored -> ownerOrder.size());
            CategoryKey categoryKey = new CategoryKey(definition.owner(), definition.category());
            categoryOrder.computeIfAbsent(categoryKey, ignored -> categoryOrder.size());
        }
        ArrayList<SettingDefinition<?>> ordered = new ArrayList<>(definitions);
        ordered.sort(Comparator.comparingInt((SettingDefinition<?> definition) -> ownerOrder.get(definition.owner()))
                .thenComparingInt(
                        definition -> categoryOrder.get(new CategoryKey(definition.owner(), definition.category())))
                .thenComparingInt(SettingDefinition::order)
                .thenComparing(definition -> definition.key().value()));
        return List.copyOf(ordered);
    }

    private Node settingRow(SettingDefinition<?> definition) {
        Label name = new Label(definition.displayName());
        name.getStyleClass().add(EditorStyleClasses.EDITOR_SETTINGS_NAME);
        VBox description = new VBox(3.0, name);
        definition.description().ifPresent(text -> {
            Label detail = new Label(text);
            detail.setWrapText(true);
            detail.getStyleClass().add(EditorStyleClasses.EDITOR_SETTINGS_DESCRIPTION);
            description.getChildren().add(detail);
        });
        Label key = new Label(definition.key().value());
        key.getStyleClass().add(EditorStyleClasses.EDITOR_SETTINGS_KEY);
        description.getChildren().add(key);

        Region editor = editor(definition);
        Button resetButton = new Button("Reset");
        resetButton.setDisable(!session.configuration().isOverridden(definition.key()));
        resetButton.getStyleClass().add(EditorStyleClasses.EDITOR_SETTINGS_RESET);
        resetButton.setOnAction(ignored -> reset(definition));
        HBox value = new HBox(8.0, editor, resetButton);
        value.setAlignment(Pos.CENTER_LEFT);
        value.getStyleClass().add(EditorStyleClasses.EDITOR_SETTINGS_VALUE);
        HBox.setHgrow(editor, Priority.ALWAYS);

        BorderPane row = new BorderPane();
        row.setLeft(description);
        row.setRight(value);
        row.getStyleClass().add(EditorStyleClasses.EDITOR_SETTINGS_ROW);
        return row;
    }

    private Region editor(SettingDefinition<?> definition) {
        return switch (definition.valueType()) {
            case BOOLEAN -> booleanEditor(definition);
            case ENUM -> enumEditor(definition);
            case INTEGER, NUMBER, STRING, PATH -> textEditor(definition);
        };
    }

    private CheckBox booleanEditor(SettingDefinition<?> definition) {
        CheckBox editor = new CheckBox();
        editor.setSelected((Boolean) effectiveValue(definition));
        editor.setOnAction(ignored -> update(definition, editor.isSelected()));
        return editor;
    }

    private ComboBox<SettingChoice> enumEditor(SettingDefinition<?> definition) {
        ComboBox<SettingChoice> editor = new ComboBox<>();
        editor.getItems().addAll(definition.constraints().choices());
        String selectedValue = (String) effectiveValue(definition);
        editor.setValue(editor.getItems().stream()
                .filter(choice -> choice.value().equals(selectedValue))
                .findFirst()
                .orElseThrow());
        editor.setConverter(new StringConverter<>() {
            @Override
            public String toString(SettingChoice choice) {
                return choice == null ? "" : choice.label();
            }

            @Override
            public SettingChoice fromString(String label) {
                return editor.getItems().stream()
                        .filter(choice -> choice.label().equals(label))
                        .findFirst()
                        .orElse(null);
            }
        });
        editor.setMaxWidth(Double.MAX_VALUE);
        editor.setOnAction(ignored -> update(definition, editor.getValue().value()));
        return editor;
    }

    private TextField textEditor(SettingDefinition<?> definition) {
        TextField editor = new TextField(display(effectiveValue(definition)));
        editor.setMaxWidth(Double.MAX_VALUE);
        editor.setOnAction(ignored -> updateFromText(definition, editor));
        ChangeListener<Boolean> focusListener = (ignored, wasFocused, focused) -> {
            if (wasFocused && !focused) {
                updateFromText(definition, editor);
            }
        };
        editor.focusedProperty().addListener(focusListener);
        return editor;
    }

    private void updateFromText(SettingDefinition<?> definition, TextField editor) {
        String current = display(effectiveValue(definition));
        if (editor.getText().equals(current)) {
            return;
        }
        try {
            Object parsed = parse(definition.valueType(), editor.getText());
            updateSetting(session, definition, parsed);
            editor.setText(display(effectiveValue(definition)));
            showSaved(definition);
        } catch (IllegalArgumentException | IOException exception) {
            editor.setText(current);
            showFailure(definition, exception);
        }
    }

    private void update(SettingDefinition<?> definition, Object value) {
        try {
            updateSetting(session, definition, value);
            showSaved(definition);
        } catch (IllegalArgumentException | IOException exception) {
            showFailure(definition, exception);
        }
    }

    private void reset(SettingDefinition<?> definition) {
        try {
            session.resetSetting(definition.key());
            rebuild();
            messages.accept(new EditorMessage(
                    EditorMessageSeverity.INFORMATION, definition.displayName() + " reset to its default"));
        } catch (IOException exception) {
            showFailure(definition, exception);
        }
    }

    private void rebuild() {
        root.setCenter(settingsScroll());
    }

    private Object effectiveValue(SettingDefinition<?> definition) {
        return effectiveValue(session, definition);
    }

    private void showSaved(SettingDefinition<?> definition) {
        rebuild();
        messages.accept(new EditorMessage(EditorMessageSeverity.INFORMATION, definition.displayName() + " saved"));
    }

    private void showFailure(SettingDefinition<?> definition, Exception exception) {
        String detail = Objects.requireNonNullElse(exception.getMessage(), exception.toString());
        messages.accept(new EditorMessage(
                EditorMessageSeverity.ERROR, "Unable to change " + definition.displayName() + ": " + detail));
    }

    private static Object parse(SettingValueType type, String text) {
        String candidate = Objects.requireNonNull(text, "text");
        try {
            return switch (type) {
                case INTEGER -> Integer.valueOf(candidate.strip());
                case NUMBER -> new BigDecimal(candidate.strip());
                case PATH -> Path.of(candidate.strip());
                case STRING, ENUM -> candidate;
                case BOOLEAN -> throw new IllegalArgumentException("boolean settings use a checkbox");
            };
        } catch (NumberFormatException | InvalidPathException exception) {
            throw new IllegalArgumentException("value has the wrong format", exception);
        }
    }

    private static String display(Object value) {
        return value instanceof Path path ? path.toString() : value.toString();
    }

    private static <T> void updateSetting(EditorProjectSession session, SettingDefinition<T> definition, Object value)
            throws IOException {
        T validated = definition.validate(value);
        session.updateSetting(definition.key(), validated);
    }

    private static <T> T effectiveValue(EditorProjectSession session, SettingDefinition<T> definition) {
        return session.configuration().get(definition.key());
    }

    private record CategoryKey(String owner, String category) {}
}
