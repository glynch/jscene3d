/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.workbench.inspector;

import io.github.glynch.jscene3d.editor.presentation.AuthoringText;
import io.github.glynch.jscene3d.editor.workbench.hierarchy.HierarchyOccurrenceId;
import io.github.glynch.jscene3d.project.component.ComponentDefinition;
import io.github.glynch.jscene3d.project.component.ComponentType;
import io.github.glynch.jscene3d.project.component.PropertyId;
import io.github.glynch.jscene3d.project.entity.EntityId;
import io.github.glynch.jscene3d.project.extension.ProjectValueKind;
import io.github.glynch.jscene3d.project.extension.PropertyDescriptor;
import io.github.glynch.jscene3d.project.extension.RegisteredTypeCatalog;
import io.github.glynch.jscene3d.project.value.ProjectValue;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Projects component values through their exact descriptor metadata. */
final class EditorComponentSectionProjector {
    private EditorComponentSectionProjector() {
        throw new AssertionError("EditorComponentSectionProjector cannot be instantiated");
    }

    /** Projects component definitions in authored order. */
    static List<InspectorSection> componentSections(
            Optional<EditableEntity> editable,
            List<ComponentDefinition> components,
            RegisteredTypeCatalog types,
            InspectorValueProjector values) {
        return components.stream()
                .map(component -> componentSection(editable, component, types, values))
                .toList();
    }

    private static InspectorSection componentSection(
            Optional<EditableEntity> editable,
            ComponentDefinition component,
            RegisteredTypeCatalog types,
            InspectorValueProjector values) {
        ComponentType type = new ComponentType(component.type(), component.typeVersion());
        return types.findComponent(type)
                .map(descriptor -> descriptorSection(
                        editable,
                        component,
                        type,
                        AuthoringText.literal(descriptor.presentation().displayName()),
                        descriptor.presentation().description().map(AuthoringText::literal),
                        descriptor.properties(),
                        values))
                .orElseGet(() -> missingDescriptorSection(component, type, values));
    }

    private static InspectorSection descriptorSection(
            Optional<EditableEntity> editable,
            ComponentDefinition component,
            ComponentType type,
            AuthoringText label,
            Optional<AuthoringText> description,
            Map<PropertyId, PropertyDescriptor> descriptors,
            InspectorValueProjector values) {
        List<InspectorProperty> properties = new ArrayList<>();
        descriptors.forEach(
                (id, descriptor) -> properties.add(descriptorProperty(editable, component, id, descriptor, values)));
        component.properties().forEach((id, value) -> {
            if (!descriptors.containsKey(id)) {
                properties.add(authoredProperty(id, value, values));
            }
        });
        return new InspectorSection(
                component.id().toString(),
                InspectorSection.Kind.COMPONENT,
                label,
                description,
                Optional.of(component.id()),
                Optional.of(type),
                true,
                editable.isPresent(),
                properties);
    }

    private static InspectorProperty descriptorProperty(
            Optional<EditableEntity> editable,
            ComponentDefinition component,
            PropertyId id,
            PropertyDescriptor descriptor,
            InspectorValueProjector values) {
        Optional<ProjectValue> authored =
                Optional.ofNullable(component.properties().get(id));
        Optional<ProjectValue> defaultValue = descriptor.defaultValue();
        Optional<ProjectValue> displayed = authored.or(() -> defaultValue);
        InspectorProperty.Origin origin;
        if (authored.isPresent()) {
            origin = InspectorProperty.Origin.AUTHORED;
        } else if (displayed.isPresent()) {
            origin = InspectorProperty.Origin.DEFAULT;
        } else {
            origin = InspectorProperty.Origin.UNSET;
        }
        InspectorConstraints constraints = new InspectorConstraints(
                descriptor.elementKind(),
                descriptor.exactElementCount(),
                descriptor.acceptedReferenceKinds(),
                descriptor.editor());
        Optional<InspectorMutationTarget> mutation =
                editable.map(target -> new InspectorMutationTarget.ComponentProperty(
                        target.occurrence(), target.entity(), component.id(), id));
        Optional<InspectorValue> authoredInspection = authored.map(values::project);
        Optional<InspectorValue> defaultInspection = defaultValue.map(values::project);
        Optional<InspectorValue> effectiveInspection = displayed.map(values::project);
        InspectorProperty.Validity validity;
        if (displayed.isEmpty() && descriptor.isRequired()) {
            validity = InspectorProperty.Validity.REQUIRED_UNSET;
        } else if (effectiveInspection
                .filter(EditorComponentSectionProjector::containsBrokenReference)
                .isPresent()) {
            validity = InspectorProperty.Validity.BROKEN_REFERENCE;
        } else {
            validity = InspectorProperty.Validity.VALID;
        }
        return new InspectorProperty(
                id.value(),
                new InspectorProperty.Presentation(
                        AuthoringText.literal(descriptor.presentation().displayName()),
                        descriptor.valueKind(),
                        descriptor.isRequired(),
                        descriptor.presentation().description().map(AuthoringText::literal),
                        constraints),
                new InspectorProperty.State(
                        authoredInspection,
                        defaultInspection,
                        effectiveInspection,
                        origin,
                        validity,
                        mutation.isPresent()),
                mutation);
    }

    private static InspectorSection missingDescriptorSection(
            ComponentDefinition component, ComponentType type, InspectorValueProjector values) {
        List<InspectorProperty> properties = component.properties().entrySet().stream()
                .map(entry -> authoredProperty(entry.getKey(), entry.getValue(), values))
                .toList();
        return new InspectorSection(
                component.id().toString(),
                InspectorSection.Kind.COMPONENT,
                AuthoringText.literal(component.type().value()),
                Optional.of(AuthoringText.message(
                        "editor.inspector.descriptor-unavailable",
                        "Descriptor metadata is unavailable for type version {0,number,integer}",
                        component.typeVersion())),
                Optional.of(component.id()),
                Optional.of(type),
                false,
                false,
                properties);
    }

    private static InspectorProperty authoredProperty(
            PropertyId id, ProjectValue value, InspectorValueProjector values) {
        InspectorValue projected = values.project(value);
        return new InspectorProperty(
                id.value(),
                new InspectorProperty.Presentation(
                        AuthoringText.literal(id.value()),
                        ProjectValueKind.of(value),
                        false,
                        Optional.empty(),
                        InspectorConstraints.empty()),
                new InspectorProperty.State(
                        Optional.of(projected),
                        Optional.empty(),
                        Optional.of(projected),
                        InspectorProperty.Origin.AUTHORED,
                        InspectorProperty.Validity.METADATA_UNAVAILABLE,
                        false),
                Optional.empty());
    }

    /** Returns whether a recursively projected value contains an unresolved semantic target. */
    private static boolean containsBrokenReference(InspectorValue value) {
        return switch (value) {
            case InspectorValue.ReferenceValue reference -> reference.resolution() == InspectorValue.Resolution.BROKEN;
            case InspectorValue.EntityTargetValue target -> target.resolution() == InspectorValue.Resolution.BROKEN;
            case InspectorValue.ComponentTargetValue target -> target.resolution() == InspectorValue.Resolution.BROKEN;
            case InspectorValue.ArrayValue array ->
                array.values().stream().anyMatch(EditorComponentSectionProjector::containsBrokenReference);
            case InspectorValue.ObjectValue object ->
                object.values().values().stream().anyMatch(EditorComponentSectionProjector::containsBrokenReference);
            default -> false;
        };
    }

    /** Editable local entity identity used to create stable mutation targets. */
    record EditableEntity(HierarchyOccurrenceId occurrence, EntityId entity) {}
}
