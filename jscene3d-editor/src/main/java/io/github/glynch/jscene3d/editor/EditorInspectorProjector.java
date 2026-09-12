/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor;

import io.github.glynch.jscene3d.editor.selection.EditorSelection;
import io.github.glynch.jscene3d.editor.selection.EditorSelectionKindId;
import io.github.glynch.jscene3d.editor.selection.EditorSelectionKinds;
import io.github.glynch.jscene3d.editor.view.EditorDetails;
import io.github.glynch.jscene3d.editor.view.EditorIcon;
import io.github.glynch.jscene3d.editor.view.EditorIcons;
import io.github.glynch.jscene3d.editor.view.EditorPropertyEditor;
import io.github.glynch.jscene3d.project.component.ComponentDefinition;
import io.github.glynch.jscene3d.project.component.ComponentId;
import io.github.glynch.jscene3d.project.component.ComponentType;
import io.github.glynch.jscene3d.project.component.PropertyId;
import io.github.glynch.jscene3d.project.entity.EntityDefinition;
import io.github.glynch.jscene3d.project.entity.EntityId;
import io.github.glynch.jscene3d.project.entity.EntityPlacement;
import io.github.glynch.jscene3d.project.entity.LocalEntity;
import io.github.glynch.jscene3d.project.extension.DescriptorPresentation;
import io.github.glynch.jscene3d.project.extension.ProjectValueKind;
import io.github.glynch.jscene3d.project.extension.PropertyDescriptor;
import io.github.glynch.jscene3d.project.extension.RegisteredTypeCatalog;
import io.github.glynch.jscene3d.project.imports.ImportDefinition;
import io.github.glynch.jscene3d.project.manifest.GameProject;
import io.github.glynch.jscene3d.project.spatial3d.descriptor.Spatial3dDescriptors;
import io.github.glynch.jscene3d.project.value.ProjectValue;
import io.github.glynch.jscene3d.project.world.WorldDefinition;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.StringJoiner;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/** Projects validated project data into immutable, non-executable Inspector data. */
final class EditorInspectorProjector {
    private EditorInspectorProjector() {}

    /** Projects the opened world root. */
    static EditorSelection world(WorldDefinition world, Path source, Path projectRoot) {
        List<EditorDetails.Property> properties = List.of(
                textProperty("asset-id", "Asset ID", world.id().toString()),
                numberProperty("root-count", "Root entities", world.roots().size()),
                numberProperty(
                        "connection-count", "Connections", world.connections().size()));
        EditorDetails view = view(
                world.name(),
                "World definition",
                source,
                projectRoot,
                world.id().toString(),
                Editability.READ_ONLY,
                List.of(section("World", properties)));
        return selection(EditorSelectionKinds.WORLD, source, world.id().toString(), view);
    }

    /** Projects one local or generated entity and its descriptor-backed components. */
    static EditorSelection entity(
            LocalEntity entity, Path source, Path projectRoot, RegisteredTypeCatalog types, boolean generated) {
        return entity(entity, source, projectRoot, types, generated, Optional.empty(), Optional.empty());
    }

    /** Projects one local or generated entity with an optional enabled-state edit command. */
    static EditorSelection entity(
            LocalEntity entity,
            Path source,
            Path projectRoot,
            RegisteredTypeCatalog types,
            boolean generated,
            Optional<Consumer<Boolean>> enabledEditor) {
        return entity(entity, source, projectRoot, types, generated, enabledEditor, Optional.empty());
    }

    /** Projects one local or generated entity with optional authored entity/component edit commands. */
    static EditorSelection entity(
            LocalEntity entity,
            Path source,
            Path projectRoot,
            RegisteredTypeCatalog types,
            boolean generated,
            Optional<Consumer<Boolean>> enabledEditor,
            Optional<EditorComponentPropertyEditor> componentEditor) {
        String title = entity.name().orElse("Unnamed entity");
        List<EditorDetails.Section> sections = new ArrayList<>();
        sections.add(section(
                "Entity",
                List.of(
                        booleanProperty("enabled", "Enabled", entity.isEnabled(), enabledEditor),
                        numberProperty(
                                "child-count", "Children", entity.children().size()))));
        sections.addAll(componentSections(entity.id(), entity.components(), types, componentEditor));
        EditorDetails view = view(
                title,
                generated ? "Generated entity" : "Local entity",
                source,
                projectRoot,
                entity.id().toString(),
                editability(generated, enabledEditor.isPresent()),
                sections);
        EditorSelectionKindId kind =
                generated ? EditorSelectionKinds.GENERATED_ENTITY : EditorSelectionKinds.LOCAL_ENTITY;
        return selection(kind, source, entity.id().toString(), view);
    }

    /** Projects an authored reusable-definition placement and its realized root components. */
    static EditorSelection placement(
            EntityPlacement placement,
            Optional<EntityDefinition> definition,
            Path source,
            Path projectRoot,
            RegisteredTypeCatalog types,
            boolean generated) {
        return placement(placement, definition, source, projectRoot, types, generated, Optional.empty());
    }

    /** Projects an authored placement with an optional enabled-state edit command. */
    static EditorSelection placement(
            EntityPlacement placement,
            Optional<EntityDefinition> definition,
            Path source,
            Path projectRoot,
            RegisteredTypeCatalog types,
            boolean generated,
            Optional<Consumer<Boolean>> enabledEditor) {
        String title = placement
                .name()
                .orElseGet(() -> definition.map(EntityDefinition::name).orElse("Unavailable definition"));
        List<EditorDetails.Property> placementProperties = new ArrayList<>();
        placementProperties.add(textProperty(
                "definition", "Definition", placement.definition().id().toString()));
        placementProperties.add(booleanProperty("enabled", "Enabled", placement.isEnabled(), enabledEditor));
        placement.arguments().forEach((id, value) -> placementProperties.add(authoredProperty(id, value)));
        List<EditorDetails.Section> sections = new ArrayList<>();
        sections.add(section("Placement", placementProperties));
        definition.ifPresent(
                value -> sections.addAll(componentSections(value.root().components(), types)));
        EditorDetails view = view(
                title,
                generated ? "Generated placement" : "Entity-definition placement",
                source,
                projectRoot,
                placement.id().toString(),
                editability(generated, enabledEditor.isPresent()),
                sections);
        return selection(EditorSelectionKinds.PLACEMENT, source, placement.id().toString(), view);
    }

    /** Projects one reusable entity-definition asset. */
    static EditorSelection entityDefinition(
            EntityDefinition definition, Path source, Path projectRoot, RegisteredTypeCatalog types) {
        List<EditorDetails.Section> sections = new ArrayList<>();
        sections.add(section(
                "Definition",
                List.of(
                        textProperty("asset-id", "Asset ID", definition.id().toString()),
                        numberProperty(
                                "parameters",
                                "Parameters",
                                definition.contract().parameters().size()),
                        numberProperty(
                                "connections",
                                "Connections",
                                definition.connections().size()))));
        sections.addAll(componentSections(definition.root().components(), types));
        EditorDetails view = view(
                definition.name(),
                "Entity definition",
                source,
                projectRoot,
                definition.id().toString(),
                Editability.READ_ONLY,
                sections);
        return selection(EditorSelectionKinds.ASSET, source, definition.id().toString(), view);
    }

    /** Projects one world-definition asset. */
    static EditorSelection worldDefinition(WorldDefinition world, Path source, Path projectRoot) {
        EditorDetails view = view(
                world.name(),
                "World definition",
                source,
                projectRoot,
                world.id().toString(),
                Editability.READ_ONLY,
                List.of(section(
                        "Definition",
                        List.of(
                                textProperty("asset-id", "Asset ID", world.id().toString()),
                                numberProperty(
                                        "root-count",
                                        "Root entities",
                                        world.roots().size()),
                                numberProperty(
                                        "connection-count",
                                        "Connections",
                                        world.connections().size())))));
        return selection(EditorSelectionKinds.ASSET, source, world.id().toString(), view);
    }

    /** Projects one authoritative source asset declared by the manifest. */
    static EditorSelection sourceAsset(GameProject.AssetSource asset, Path projectRoot) {
        List<EditorDetails.Property> properties = new ArrayList<>();
        properties.add(textProperty("identity", "Identity", asset.id()));
        properties.add(textProperty("type", "Type", asset.type()));
        asset.sha256().ifPresent(hash -> properties.add(textProperty("sha256", "SHA-256", hash)));
        EditorDetails view = view(
                asset.id(),
                "Source asset",
                asset.path(),
                projectRoot,
                asset.id(),
                Editability.READ_ONLY,
                List.of(section("Asset", properties)));
        return selection(EditorSelectionKinds.ASSET, asset.path(), asset.id(), view);
    }

    /** Projects one deterministic source-import definition and its authored settings. */
    static EditorSelection importDefinition(ImportDefinition definition, Path projectRoot) {
        List<EditorDetails.Property> properties = new ArrayList<>();
        properties.add(textProperty("identity", "Identity", definition.id()));
        properties.add(textProperty("importer", "Importer", definition.importer()));
        properties.add(textProperty("asset", "Source asset", definition.asset().id()));
        properties.add(numberProperty(
                "selection-count", "Selected items", definition.selection().size()));
        definition.settings().forEach((id, value) -> properties.add(authoredProperty(new PropertyId(id), value)));
        EditorDetails view = view(
                definition.id(),
                "Import definition",
                definition.source(),
                projectRoot,
                definition.id(),
                Editability.READ_ONLY,
                List.of(section("Import", properties)));
        return selection(EditorSelectionKinds.ASSET, definition.source(), definition.id(), view);
    }

    /** Projects component definitions in authored order using exact descriptor versions. */
    private static List<EditorDetails.Section> componentSections(
            List<ComponentDefinition> components, RegisteredTypeCatalog types) {
        return components.stream()
                .map(component -> componentSection(component, types))
                .toList();
    }

    /** Projects editable component definitions for one locally authored entity. */
    private static List<EditorDetails.Section> componentSections(
            EntityId entityId,
            List<ComponentDefinition> components,
            RegisteredTypeCatalog types,
            Optional<EditorComponentPropertyEditor> componentEditor) {
        return components.stream()
                .map(component -> componentSection(Optional.of(entityId), component, types, componentEditor))
                .toList();
    }

    /** Projects one component while retaining authored values even if metadata is unavailable. */
    private static EditorDetails.Section componentSection(ComponentDefinition component, RegisteredTypeCatalog types) {
        return componentSection(Optional.empty(), component, types, Optional.empty());
    }

    /** Projects one optionally editable component while retaining unavailable metadata safely. */
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

    /** Applies descriptor presentation, property order, defaults, and constraints. */
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
            EditorDetails.ValueOrigin origin;
            if (authored.isPresent()) {
                origin = EditorDetails.ValueOrigin.AUTHORED;
            } else if (displayed.isPresent()) {
                origin = EditorDetails.ValueOrigin.DEFAULT;
            } else {
                origin = EditorDetails.ValueOrigin.UNSET;
            }
            properties.add(new EditorDetails.Property(
                    id.value(),
                    descriptor.presentation().displayName(),
                    label(descriptor.valueKind()),
                    displayed.map(EditorInspectorProjector::format).orElse("Not set"),
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

    /** Supplies the first descriptor-backed editor for authored Transform3d positions. */
    private static Optional<EditorPropertyEditor> propertyEditor(
            Optional<EntityId> entityId,
            ComponentDefinition component,
            PropertyId propertyId,
            PropertyDescriptor descriptor,
            Optional<EditorComponentPropertyEditor> componentEditor) {
        if (entityId.isEmpty()
                || componentEditor.isEmpty()
                || !component.type().equals(Spatial3dDescriptors.transformType().id())
                || component.typeVersion()
                        != Spatial3dDescriptors.transformType().version()
                || !propertyId.equals(Spatial3dDescriptors.positionProperty())) {
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

    /** Preserves authored values when exact safe descriptor metadata could not be resolved. */
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

    /** Retains structural constraints without teaching the editor component-specific semantics. */
    private static Map<String, String> constraints(PropertyDescriptor descriptor) {
        Map<String, String> constraints = new LinkedHashMap<>();
        descriptor.elementKind().ifPresent(kind -> constraints.put("Element type", label(kind)));
        if (!descriptor.acceptedReferenceKinds().isEmpty()) {
            String accepted = descriptor.acceptedReferenceKinds().stream()
                    .map(kind -> kind.prefix().substring(0, kind.prefix().length() - 1))
                    .sorted()
                    .collect(Collectors.joining(", "));
            constraints.put("Accepted references", accepted);
        }
        descriptor.editorMetadata().forEach((key, value) -> constraints.put(key, format(value)));
        return constraints;
    }

    /** Creates one ordinary authored property without descriptor presentation metadata. */
    private static EditorDetails.Property authoredProperty(PropertyId id, ProjectValue value) {
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

    /** Creates one text-valued summary row. */
    private static EditorDetails.Property textProperty(String id, String name, String value) {
        return summaryProperty(id, name, ProjectValueKind.TEXT, value);
    }

    /** Creates one numeric summary row. */
    private static EditorDetails.Property numberProperty(String id, String name, int value) {
        return summaryProperty(id, name, ProjectValueKind.NUMBER, Integer.toString(value));
    }

    /** Creates a boolean row optionally backed by one document edit command. */
    private static EditorDetails.Property booleanProperty(
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

    /** Creates a projected summary row backed by immutable loaded data. */
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

    /** Creates one ordinary Inspector section. */
    private static EditorDetails.Section section(String title, List<EditorDetails.Property> properties) {
        return new EditorDetails.Section(title, Optional.empty(), true, properties);
    }

    /** Creates the common immutable view header. */
    private static EditorDetails view(
            String title,
            String kind,
            Path source,
            Path projectRoot,
            String identity,
            Editability editability,
            List<EditorDetails.Section> sections) {
        String tooltip = editability == Editability.GENERATED_READ_ONLY ? "Generated content · read-only" : "Read-only";
        return new EditorDetails(
                title,
                kind,
                displaySource(source, projectRoot),
                identity,
                editability == Editability.EDITABLE
                        ? List.of()
                        : List.of(new EditorIcon(EditorIcons.READ_ONLY, tooltip)),
                sections);
    }

    /** Describes whether an Inspector projection can be edited and why not. */
    private static Editability editability(boolean generated, boolean editable) {
        if (editable) {
            return Editability.EDITABLE;
        }
        return generated ? Editability.GENERATED_READ_ONLY : Editability.READ_ONLY;
    }

    /** Creates a source-scoped stable selection key because entity IDs are asset-local. */
    private static EditorSelection selection(
            EditorSelectionKindId kind, Path source, String identity, EditorDetails view) {
        String key = kind.value() + ':' + source.toAbsolutePath().normalize() + ':' + identity;
        return new EditorSelection(kind, key, view);
    }

    /** Displays project-local sources without discarding the complete normalized source in the selection key. */
    private static String displaySource(Path source, Path projectRoot) {
        Path normalizedSource = source.toAbsolutePath().normalize();
        Path normalizedRoot = projectRoot.toAbsolutePath().normalize();
        return normalizedSource.startsWith(normalizedRoot)
                ? normalizedRoot.relativize(normalizedSource).toString()
                : normalizedSource.toString();
    }

    /** Formats closed portable values without depending on a JSON implementation. */
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

    /** Joins a value sequence using compact JSON-like punctuation. */
    private static String join(List<ProjectValue> values, String prefix, String suffix) {
        StringJoiner joiner = new StringJoiner(", ", prefix, suffix);
        values.stream().map(EditorInspectorProjector::format).forEach(joiner::add);
        return joiner.toString();
    }

    /** Joins an ordered property map using compact JSON-like punctuation. */
    private static String joinObject(Map<String, ProjectValue> values) {
        StringJoiner joiner = new StringJoiner(", ", "{", "}");
        values.forEach((key, value) -> joiner.add(key + ": " + format(value)));
        return joiner.toString();
    }

    /** Converts a stable kebab-case identity into a fallback author-facing label. */
    private static String displayName(String identity) {
        StringJoiner words = new StringJoiner(" ");
        for (String part : identity.split("-")) {
            words.add(part);
        }
        String value = words.toString();
        return value.isEmpty() ? identity : Character.toUpperCase(value.charAt(0)) + value.substring(1);
    }

    /** Formats a structural value kind for secondary metadata. */
    static String label(ProjectValueKind kind) {
        return kind.name().toLowerCase(Locale.ROOT).replace('_', ' ');
    }

    private enum Editability {
        EDITABLE,
        READ_ONLY,
        GENERATED_READ_ONLY
    }
}
