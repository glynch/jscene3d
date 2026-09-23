/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.workbench.inspector;

import static io.github.glynch.jscene3d.editor.workbench.inspector.EditorInspectorProperties.authoredProperty;
import static io.github.glynch.jscene3d.editor.workbench.inspector.EditorInspectorProperties.constraints;
import static io.github.glynch.jscene3d.editor.workbench.inspector.EditorInspectorProperties.label;

import io.github.glynch.jscene3d.editor.view.EditorDetails;
import io.github.glynch.jscene3d.editor.view.EditorPropertyEditor;
import io.github.glynch.jscene3d.project.component.ComponentDefinition;
import io.github.glynch.jscene3d.project.component.ComponentId;
import io.github.glynch.jscene3d.project.component.ComponentType;
import io.github.glynch.jscene3d.project.component.PropertyId;
import io.github.glynch.jscene3d.project.entity.EntityId;
import io.github.glynch.jscene3d.project.extension.DescriptorPresentation;
import io.github.glynch.jscene3d.project.extension.PropertyDescriptor;
import io.github.glynch.jscene3d.project.extension.RegisteredTypeCatalog;
import io.github.glynch.jscene3d.project.spatial3d.descriptor.Spatial3dDescriptors;
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

    /** Projects read-only component definitions in authored order. */
    static List<EditorDetails.Section> componentSections(
            List<ComponentDefinition> components, RegisteredTypeCatalog types) {
        return components.stream()
                .map(component -> componentSection(component, types))
                .toList();
    }

    /** Projects editable component definitions for one locally authored entity. */
    static List<EditorDetails.Section> componentSections(
            EntityId entityId,
            List<ComponentDefinition> components,
            RegisteredTypeCatalog types,
            Optional<EditorComponentPropertyEditor> componentEditor) {
        return components.stream()
                .map(component -> componentSection(Optional.of(entityId), component, types, componentEditor))
                .toList();
    }

    private static EditorDetails.Section componentSection(ComponentDefinition component, RegisteredTypeCatalog types) {
        return componentSection(Optional.empty(), component, types, Optional.empty());
    }

    private static EditorDetails.Section componentSection(
            Optional<EntityId> entityId,
            ComponentDefinition component,
            RegisteredTypeCatalog types,
            Optional<EditorComponentPropertyEditor> componentEditor) {
        ComponentType type = new ComponentType(component.type(), component.typeVersion());
        return types.findComponent(type)
                .map(descriptor -> descriptorSection(
                        entityId, component, descriptor.presentation(), descriptor.properties(), componentEditor))
                .orElseGet(() -> missingDescriptorSection(component));
    }

    private static EditorDetails.Section descriptorSection(
            Optional<EntityId> entityId,
            ComponentDefinition component,
            DescriptorPresentation presentation,
            Map<PropertyId, PropertyDescriptor> descriptors,
            Optional<EditorComponentPropertyEditor> componentEditor) {
        List<EditorDetails.Property> properties = new ArrayList<>();
        for (Map.Entry<PropertyId, PropertyDescriptor> entry : descriptors.entrySet()) {
            PropertyId id = entry.getKey();
            PropertyDescriptor descriptor = entry.getValue();
            Optional<ProjectValue> authored =
                    Optional.ofNullable(component.properties().get(id));
            Optional<ProjectValue> displayed = authored.or(descriptor::defaultValue);
            EditorDetails.ValueOrigin origin = valueOrigin(authored, displayed);
            properties.add(new EditorDetails.Property(
                    id.value(),
                    descriptor.presentation().displayName(),
                    label(descriptor.valueKind()),
                    displayed.map(EditorInspectorProperties::format).orElse("Not set"),
                    origin,
                    descriptor.isRequired(),
                    descriptor.presentation().description(),
                    constraints(descriptor),
                    propertyEditor(entityId, component, id, descriptor, componentEditor)));
        }
        component.properties().forEach((id, value) -> {
            if (!descriptors.containsKey(id)) {
                properties.add(authoredProperty(id, value));
            }
        });
        return new EditorDetails.Section(presentation.displayName(), presentation.description(), true, properties);
    }

    private static EditorDetails.ValueOrigin valueOrigin(
            Optional<ProjectValue> authored, Optional<ProjectValue> displayed) {
        if (authored.isPresent()) {
            return EditorDetails.ValueOrigin.AUTHORED;
        }
        return displayed.isPresent() ? EditorDetails.ValueOrigin.DEFAULT : EditorDetails.ValueOrigin.UNSET;
    }

    private static Optional<EditorPropertyEditor> propertyEditor(
            Optional<EntityId> entityId,
            ComponentDefinition component,
            PropertyId propertyId,
            PropertyDescriptor descriptor,
            Optional<EditorComponentPropertyEditor> componentEditor) {
        if (!isEditablePosition(entityId, component, propertyId, componentEditor)) {
            return Optional.empty();
        }
        ComponentId componentId = component.id();
        return Optional.of(replacement -> {
            ProjectValue value = EditorVectorValueParser.parse(replacement, 3);
            if (!descriptor.accepts(value)) {
                throw new IllegalArgumentException("replacement does not satisfy property " + propertyId.value());
            }
            componentEditor.orElseThrow().set(entityId.orElseThrow(), componentId, propertyId, value);
        });
    }

    private static boolean isEditablePosition(
            Optional<EntityId> entityId,
            ComponentDefinition component,
            PropertyId propertyId,
            Optional<EditorComponentPropertyEditor> componentEditor) {
        return entityId.isPresent()
                && componentEditor.isPresent()
                && component.type().equals(Spatial3dDescriptors.transformType().id())
                && component.typeVersion()
                        == Spatial3dDescriptors.transformType().version()
                && propertyId.equals(Spatial3dDescriptors.positionProperty());
    }

    private static EditorDetails.Section missingDescriptorSection(ComponentDefinition component) {
        List<EditorDetails.Property> properties = component.properties().entrySet().stream()
                .map(entry -> authoredProperty(entry.getKey(), entry.getValue()))
                .toList();
        return new EditorDetails.Section(
                component.type().value(),
                Optional.of("Descriptor metadata unavailable for type version " + component.typeVersion()),
                false,
                properties);
    }
}
