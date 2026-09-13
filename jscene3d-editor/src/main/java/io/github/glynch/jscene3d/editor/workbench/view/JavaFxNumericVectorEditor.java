/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.workbench.view;

import io.github.glynch.jscene3d.editor.view.EditorDetails;
import io.github.glynch.jscene3d.editor.workbench.style.EditorStyleClasses;
import io.github.glynch.jscene3d.project.extension.ProjectValueKind;
import io.github.glynch.jscene3d.project.extension.PropertyDescriptorKeys;
import io.github.glynch.jscene3d.project.extension.PropertyEditorSemantics;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.StringJoiner;
import java.util.regex.Pattern;
import javafx.application.Platform;
import javafx.css.PseudoClass;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;

/** Three labelled numeric fields backed by one atomic vector-property edit command. */
final class JavaFxNumericVectorEditor extends HBox {
    private static final List<String> AXIS_NAMES = List.of("X", "Y", "Z");
    private static final Pattern PARTIAL_DECIMAL = Pattern.compile("[+-]?\\d*\\.?\\d*");
    private static final PseudoClass INVALID = PseudoClass.getPseudoClass("invalid");

    private final EditorDetails.Property property;
    private final List<String> originalValues;
    private final List<TextField> fields = new ArrayList<>(AXIS_NAMES.size());

    private boolean committing;

    /** Creates an XYZ editor from the property's compact serialized value. */
    JavaFxNumericVectorEditor(EditorDetails.Property property) {
        super(6.0);
        this.property = Objects.requireNonNull(property, "property");
        originalValues = components(property.value(), AXIS_NAMES.size());
        setAlignment(Pos.CENTER_LEFT);
        setMaxWidth(Double.MAX_VALUE);
        setAccessibleText(property.displayName() + " vector");
        getStyleClass().add(EditorStyleClasses.EDITOR_INSPECTOR_VECTOR_VALUE);
        for (int index = 0; index < AXIS_NAMES.size(); index++) {
            getChildren().add(createAxis(index));
        }
    }

    /** Returns whether this property requests the standard three-axis numeric editor. */
    static boolean supports(EditorDetails.Property property) {
        return property.editor().isPresent()
                && PropertyEditorSemantics.VECTOR3.equals(
                        property.constraints().get(PropertyDescriptorKeys.EDITOR_SEMANTIC))
                && ProjectValueKind.NUMBER
                        .name()
                        .equalsIgnoreCase(property.constraints().get(PropertyDescriptorKeys.ELEMENT_KIND))
                && Integer.toString(AXIS_NAMES.size())
                        .equals(property.constraints().get(PropertyDescriptorKeys.EXACT_ELEMENT_COUNT));
    }

    /** Returns whether text is a legal intermediate state for editing a decimal number. */
    static boolean acceptsPartialNumber(String value) {
        return PARTIAL_DECIMAL.matcher(Objects.requireNonNull(value, "value")).matches();
    }

    /** Returns whether text is a complete non-exponential decimal accepted by the project value model. */
    static boolean isCompleteNumber(String value) {
        String text = Objects.requireNonNull(value, "value");
        if (!PARTIAL_DECIMAL.matcher(text).matches()) {
            return false;
        }
        try {
            new BigDecimal(text);
            return true;
        } catch (NumberFormatException exception) {
            return false;
        }
    }

    /** Splits one compact bracketed vector into its trimmed components. */
    static List<String> components(String value, int size) {
        String text = Objects.requireNonNull(value, "value").trim();
        if (text.length() < 2 || text.charAt(0) != '[' || text.charAt(text.length() - 1) != ']') {
            throw invalid(size);
        }
        String[] parts = text.substring(1, text.length() - 1).split(",", -1);
        if (parts.length != size) {
            throw invalid(size);
        }
        List<String> result = new ArrayList<>(size);
        for (String part : parts) {
            String component = part.trim();
            if (component.isEmpty()) {
                throw invalid(size);
            }
            result.add(component);
        }
        return List.copyOf(result);
    }

    /** Joins individual numeric field values into the complete property replacement. */
    static String replacement(List<String> values) {
        StringJoiner result = new StringJoiner(", ", "[", "]");
        values.forEach(value -> result.add(value.trim()));
        return result.toString();
    }

    /** Creates one labelled, horizontally growing axis input. */
    private HBox createAxis(int index) {
        String axisName = AXIS_NAMES.get(index);
        TextField field = new TextField(originalValues.get(index));
        field.setAccessibleText(property.displayName() + " " + axisName);
        field.getStyleClass().add(EditorStyleClasses.EDITOR_INSPECTOR_VECTOR_INPUT);
        field.setTextFormatter(
                new TextFormatter<>(change -> acceptsPartialNumber(change.getControlNewText()) ? change : null));
        field.textProperty().addListener((ignored, previous, current) -> clearInvalid(field));
        field.setOnAction(ignored -> commit(field));
        field.focusedProperty().addListener((ignored, wasFocused, isFocused) -> {
            if (wasFocused && !isFocused) {
                Platform.runLater(() -> commitAfterFocusChange(field));
            }
        });
        fields.add(field);

        Label label = new Label(axisName);
        label.setLabelFor(field);
        label.getStyleClass().add(EditorStyleClasses.EDITOR_INSPECTOR_VECTOR_AXIS);
        HBox axis = new HBox(4.0, label, field);
        axis.setAlignment(Pos.CENTER_LEFT);
        axis.setMaxWidth(Double.MAX_VALUE);
        axis.getStyleClass().add(EditorStyleClasses.EDITOR_INSPECTOR_VECTOR_COMPONENT);
        HBox.setHgrow(field, Priority.ALWAYS);
        HBox.setHgrow(axis, Priority.ALWAYS);
        return axis;
    }

    /** Commits only after focus has left all three fields, not while moving between axes. */
    private void commitAfterFocusChange(TextField previousField) {
        if (fields.stream().noneMatch(TextField::isFocused)) {
            commit(previousField);
        }
    }

    /** Applies all three axes as one validated edit, restoring the projection after rejection. */
    private void commit(TextField activeField) {
        if (committing) {
            return;
        }
        List<TextField> invalidFields = fields.stream()
                .filter(field -> !isCompleteNumber(field.getText()))
                .toList();
        if (!invalidFields.isEmpty()) {
            invalidFields.forEach(field -> markInvalid(field, "Enter a complete number"));
            invalidFields.getFirst().requestFocus();
            invalidFields.getFirst().selectAll();
            return;
        }
        String updated = replacement(fields.stream().map(TextField::getText).toList());
        if (property.value().equals(updated)) {
            return;
        }
        committing = true;
        try {
            property.editor().orElseThrow().setValue(updated);
        } catch (IllegalArgumentException exception) {
            markInvalid(activeField, Objects.toString(exception.getMessage(), "Value is not accepted"));
            activeField.requestFocus();
            activeField.selectAll();
        } finally {
            committing = false;
        }
    }

    /** Marks one rejected field until the author edits it again. */
    private static void markInvalid(TextField field, String message) {
        field.pseudoClassStateChanged(INVALID, true);
        field.setTooltip(new Tooltip(message));
    }

    /** Removes a stale validation marker after the author changes the field. */
    private static void clearInvalid(TextField field) {
        field.pseudoClassStateChanged(INVALID, false);
        field.setTooltip(null);
    }

    private static IllegalArgumentException invalid(int size) {
        return new IllegalArgumentException("value must be a bracketed vector containing " + size + " components");
    }
}
