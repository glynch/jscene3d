/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.workbench.inspector;

import io.github.glynch.jscene3d.editor.view.EditorDetails;
import io.github.glynch.jscene3d.editor.view.EditorPropertyEditor;
import io.github.glynch.jscene3d.project.component.PropertyId;
import io.github.glynch.jscene3d.project.extension.ProjectValueKind;
import io.github.glynch.jscene3d.project.extension.PropertyDescriptor;
import io.github.glynch.jscene3d.project.extension.PropertyDescriptorKeys;
import io.github.glynch.jscene3d.project.value.ProjectValue;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.StringJoiner;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/** Creates and formats the property rows consumed by Inspector projections. */
final class EditorInspectorProperties {
    private EditorInspectorProperties() {
        throw new AssertionError("EditorInspectorProperties cannot be instantiated");
    }

    static Map<String, String> constraints(PropertyDescriptor descriptor) {
        Map<String, String> constraints = new LinkedHashMap<>();
        descriptor.elementKind().ifPresent(kind -> constraints.put(PropertyDescriptorKeys.ELEMENT_KIND, label(kind)));
        descriptor
                .exactElementCount()
                .ifPresent(
                        count -> constraints.put(PropertyDescriptorKeys.EXACT_ELEMENT_COUNT, Integer.toString(count)));
        if (!descriptor.acceptedReferenceKinds().isEmpty()) {
            String accepted = descriptor.acceptedReferenceKinds().stream()
                    .map(kind -> kind.prefix().substring(0, kind.prefix().length() - 1))
                    .sorted()
                    .collect(Collectors.joining(", "));
            constraints.put(PropertyDescriptorKeys.ACCEPTED_REFERENCE_KINDS, accepted);
        }
        descriptor.editorMetadata().forEach((key, value) -> constraints.put(key, format(value)));
        return constraints;
    }

    static EditorDetails.Property authoredProperty(PropertyId id, ProjectValue value) {
        return new EditorDetails.Property(
                id.value(),
                displayName(id.value()),
                label(ProjectValueKind.of(value)),
                format(value),
                EditorDetails.ValueOrigin.AUTHORED,
                false,
                Optional.empty(),
                Map.of(),
                Optional.empty());
    }

    static EditorDetails.Property textProperty(String id, String name, String value) {
        return summaryProperty(id, name, ProjectValueKind.TEXT, value);
    }

    static EditorDetails.Property numberProperty(String id, String name, int value) {
        return summaryProperty(id, name, ProjectValueKind.NUMBER, Integer.toString(value));
    }

    static EditorDetails.Property booleanProperty(
            String id, String name, boolean value, Optional<Consumer<Boolean>> editor) {
        Optional<EditorPropertyEditor> propertyEditor = editor.map(command -> replacement -> {
            if (!"true".equals(replacement) && !"false".equals(replacement)) {
                throw new IllegalArgumentException("boolean property value must be true or false");
            }
            command.accept(Boolean.valueOf(replacement));
        });
        return new EditorDetails.Property(
                id,
                name,
                label(ProjectValueKind.BOOLEAN),
                Boolean.toString(value),
                EditorDetails.ValueOrigin.AUTHORED,
                false,
                Optional.empty(),
                Map.of(),
                propertyEditor);
    }

    private static EditorDetails.Property summaryProperty(String id, String name, ProjectValueKind kind, String value) {
        return new EditorDetails.Property(
                id,
                name,
                label(kind),
                value,
                EditorDetails.ValueOrigin.AUTHORED,
                false,
                Optional.empty(),
                Map.of(),
                Optional.empty());
    }

    static String format(ProjectValue value) {
        Objects.requireNonNull(value, "value");
        return switch (value) {
            case ProjectValue.NullValue ignored -> "null";
            case ProjectValue.BooleanValue booleanValue -> Boolean.toString(booleanValue.value());
            case ProjectValue.NumberValue numberValue -> numberValue.value().toPlainString();
            case ProjectValue.TextValue textValue -> textValue.value();
            case ProjectValue.ReferenceValue referenceValue ->
                referenceValue.reference().toString();
            case ProjectValue.EntityTargetValue targetValue ->
                targetValue.entity().toString();
            case ProjectValue.ComponentTargetValue targetValue ->
                targetValue.target().entity() + "/" + targetValue.target().component();
            case ProjectValue.ArrayValue arrayValue -> join(arrayValue.values(), "[", "]");
            case ProjectValue.ObjectValue objectValue -> joinObject(objectValue.values());
        };
    }

    private static String join(List<ProjectValue> values, String prefix, String suffix) {
        StringJoiner joiner = new StringJoiner(", ", prefix, suffix);
        values.stream().map(EditorInspectorProperties::format).forEach(joiner::add);
        return joiner.toString();
    }

    private static String joinObject(Map<String, ProjectValue> values) {
        StringJoiner joiner = new StringJoiner(", ", "{", "}");
        values.forEach((key, value) -> joiner.add(key + ": " + format(value)));
        return joiner.toString();
    }

    private static String displayName(String identity) {
        StringJoiner words = new StringJoiner(" ");
        for (String part : identity.split("-")) {
            words.add(part);
        }
        String value = words.toString();
        return value.isEmpty() ? identity : Character.toUpperCase(value.charAt(0)) + value.substring(1);
    }

    static String label(ProjectValueKind kind) {
        return kind.name().toLowerCase(Locale.ROOT).replace('_', ' ');
    }
}
