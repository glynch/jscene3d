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
            Optional<EditableEntity> editable, List<ComponentDefinition> components, RegisteredTypeCatalog types) {
        return components.stream()
                .map(component -> componentSection(editable, component, types))
                .toList();
    }

    private static InspectorSection componentSection(
            Optional<EditableEntity> editable, ComponentDefinition component, RegisteredTypeCatalog types) {
        ComponentType type = new ComponentType(component.type(), component.typeVersion());
        return types.findComponent(type)
                .map(descriptor -> descriptorSection(
                        editable,
                        component,
                        type,
                        AuthoringText.literal(descriptor.presentation().displayName()),
                        descriptor.presentation().description().map(AuthoringText::literal),
                        descriptor.properties()))
                .orElseGet(() -> missingDescriptorSection(component, type));
    }

    private static InspectorSection descriptorSection(
            Optional<EditableEntity> editable,
            ComponentDefinition component,
            ComponentType type,
            AuthoringText label,
            Optional<AuthoringText> description,
            Map<PropertyId, PropertyDescriptor> descriptors) {
        List<InspectorProperty> properties = new ArrayList<>();
        descriptors.forEach(
                (id, descriptor) -> properties.add(descriptorProperty(editable, component, id, descriptor)));
        component.properties().forEach((id, value) -> {
            if (!descriptors.containsKey(id)) {
                properties.add(authoredProperty(id, value));
            }
        });
        return new InspectorSection(component.id().toString(), label, description, Optional.of(type), true, properties);
    }

    private static InspectorProperty descriptorProperty(
            Optional<EditableEntity> editable,
            ComponentDefinition component,
            PropertyId id,
            PropertyDescriptor descriptor) {
        Optional<ProjectValue> authored =
                Optional.ofNullable(component.properties().get(id));
        Optional<ProjectValue> displayed = authored.or(descriptor::defaultValue);
        InspectorProperty.Origin origin = authored.isPresent()
                ? InspectorProperty.Origin.AUTHORED
                : displayed.isPresent() ? InspectorProperty.Origin.DEFAULT : InspectorProperty.Origin.UNSET;
        InspectorConstraints constraints = new InspectorConstraints(
                descriptor.elementKind(),
                descriptor.exactElementCount(),
                descriptor.acceptedReferenceKinds(),
                descriptor.editorMetadata());
        Optional<InspectorMutationTarget> mutation =
                editable.map(target -> new InspectorMutationTarget.ComponentProperty(
                        target.occurrence(), target.entity(), component.id(), id));
        return new InspectorProperty(
                id.value(),
                new InspectorProperty.Presentation(
                        AuthoringText.literal(descriptor.presentation().displayName()),
                        descriptor.valueKind(),
                        descriptor.isRequired(),
                        descriptor.presentation().description().map(AuthoringText::literal),
                        constraints),
                new InspectorProperty.State(displayed, origin),
                mutation);
    }

    private static InspectorSection missingDescriptorSection(ComponentDefinition component, ComponentType type) {
        List<InspectorProperty> properties = component.properties().entrySet().stream()
                .map(entry -> authoredProperty(entry.getKey(), entry.getValue()))
                .toList();
        return new InspectorSection(
                component.id().toString(),
                AuthoringText.literal(component.type().value()),
                Optional.of(AuthoringText.message(
                        "editor.inspector.descriptor-unavailable",
                        "Descriptor metadata is unavailable for type version {0,number,integer}",
                        component.typeVersion())),
                Optional.of(type),
                false,
                properties);
    }

    private static InspectorProperty authoredProperty(PropertyId id, ProjectValue value) {
        return new InspectorProperty(
                id.value(),
                new InspectorProperty.Presentation(
                        AuthoringText.literal(id.value()),
                        ProjectValueKind.of(value),
                        false,
                        Optional.empty(),
                        InspectorConstraints.empty()),
                new InspectorProperty.State(Optional.of(value), InspectorProperty.Origin.AUTHORED),
                Optional.empty());
    }

    /** Editable local entity identity used to create stable mutation targets. */
    record EditableEntity(HierarchyOccurrenceId occurrence, EntityId entity) {}
}
